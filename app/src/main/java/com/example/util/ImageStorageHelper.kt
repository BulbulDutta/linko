package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object ImageStorageHelper {
    private const val TAG = "ImageStorageHelper"

    /**
     * Saves a captured camera Bitmap directly into Linko's persistent local media storage (Linko/Media/Photos).
     * Returns a local file Uri (file:///...) that can be accessed and rendered permanently.
     */
    suspend fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): Uri? = withContext(Dispatchers.IO) {
        val savedInfo = MediaDirectoryManager.saveBitmapClean(context, bitmap)
        savedInfo?.uri
    }

    /**
     * Copies a gallery content Uri (content://...) into Linko's persistent local media storage.
     * Automatically categorizes into Linko/Media/Photos, Videos, or Documents.
     */
    suspend fun saveUriToInternalStorage(context: Context, contentUri: Uri): Uri? = withContext(Dispatchers.IO) {
        try {
            if (contentUri.scheme == "file" && contentUri.path?.contains("Linko/Media") == true) {
                return@withContext contentUri
            }

            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(contentUri) ?: "application/octet-stream"
            val rawPath = contentUri.path ?: ""
            val fileName = rawPath.substringAfterLast("/").ifBlank { "Linko_Media_${System.currentTimeMillis()}" }
            val category = MediaCategory.fromMimeType(mimeType, fileName)

            val inputStream: InputStream? = contentResolver.openInputStream(contentUri)
            if (inputStream != null) {
                val savedInfo = MediaDirectoryManager.saveMediaFileClean(
                    context = context,
                    inputStream = inputStream,
                    originalFileName = if (fileName.contains(".")) fileName else "$fileName.${if (category == MediaCategory.PHOTOS) "jpg" else if (category == MediaCategory.VIDEOS) "mp4" else "pdf"}",
                    category = category
                )
                inputStream.close()
                savedInfo?.uri
            } else {
                Log.e(TAG, "InputStream was null for Uri: $contentUri")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving gallery Uri to internal storage", e)
            null
        }
    }

    /**
     * Simulates a secure cloud upload endpoint (e.g. Firebase Storage, AWS S3, Cloudinary).
     * Uploads the local file and yields the persistent public/local URI payload.
     */
    suspend fun uploadImagePayload(context: Context, inputUri: Uri): String = withContext(Dispatchers.IO) {
        val savedFileUri = saveUriToInternalStorage(context, inputUri)
        savedFileUri?.toString() ?: inputUri.toString()
    }

    /**
     * Explicitly downloads and saves an image from a local URI or remote URL into the device's
     * Gallery (MediaStore Pictures/Linko) as well as local storage when the user clicks Download.
     */
    suspend fun downloadImageToGallery(
        context: Context,
        imageUri: String,
        suggestedFileName: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val cleanUri = imageUri.trim()
            if (cleanUri.isEmpty()) return@withContext false

            val ext = when {
                cleanUri.contains(".png", ignoreCase = true) -> "png"
                cleanUri.contains(".webp", ignoreCase = true) -> "webp"
                else -> "jpg"
            }
            val mimeType = when (ext) {
                "png" -> "image/png"
                "webp" -> "image/webp"
                else -> "image/jpeg"
            }
            val fileName = suggestedFileName?.takeIf { it.isNotBlank() }
                ?: "Linko_IMG_${System.currentTimeMillis()}.$ext"

            val imageBytes = readMediaBytes(context, cleanUri) ?: return@withContext false
            if (imageBytes.isEmpty()) return@withContext false

            var savedToGallery = false

            // 1. Save to Android MediaStore (Gallery -> Pictures/Linko)
            try {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Linko")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }

                val insertedUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (insertedUri != null) {
                    resolver.openOutputStream(insertedUri)?.use { out ->
                        out.write(imageBytes)
                        out.flush()
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                        resolver.update(insertedUri, contentValues, null, null)
                    }
                    savedToGallery = true
                }
            } catch (e: Exception) {
                Log.w(TAG, "MediaStore gallery insert fallback triggered", e)
            }

            // 2. Also save to public Downloads directory & Linko local media folder
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val targetFile = File(downloadsDir, fileName)
                FileOutputStream(targetFile).use { out ->
                    out.write(imageBytes)
                    out.flush()
                }
                savedToGallery = true
            } catch (e: Exception) {
                Log.w(TAG, "Public Downloads folder write skipped", e)
            }

            val localInfo = ByteArrayInputStream(imageBytes).use { input ->
                MediaDirectoryManager.saveMediaFileClean(context, input, fileName, MediaCategory.PHOTOS)
            }
            if (localInfo != null) {
                savedToGallery = true
            }

            savedToGallery
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading image to gallery", e)
            false
        }
    }

    private fun readMediaBytes(context: Context, mediaUri: String): ByteArray? {
        return try {
            val parsed = Uri.parse(mediaUri)
            when {
                parsed.scheme == "content" -> {
                    context.contentResolver.openInputStream(parsed)?.use { it.readBytes() }
                }
                parsed.scheme == "file" -> {
                    val file = File(parsed.path ?: return null)
                    if (file.exists()) file.readBytes() else null
                }
                mediaUri.startsWith("/") -> {
                    val file = File(mediaUri)
                    if (file.exists()) file.readBytes() else null
                }
                else -> {
                    val connection = (URL(mediaUri).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 15000
                        readTimeout = 15000
                        instanceFollowRedirects = true
                        setRequestProperty("User-Agent", "Linko-Android-Client")
                    }
                    connection.inputStream.use { it.readBytes() }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read bytes from $mediaUri", e)
            null
        }
    }

    /**
     * Downloads/saves attached documents and media files directly into the device Downloads directory
     * as well as Linko/Media storage folders.
     */
    suspend fun downloadMediaFile(context: Context, mediaUri: String, suggestedFileName: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val fileExt = when {
                mediaUri.contains(".pdf", ignoreCase = true) -> "pdf"
                mediaUri.contains(".m4a", ignoreCase = true) || mediaUri.contains(".mp3", ignoreCase = true) -> "m4a"
                mediaUri.contains(".mp4", ignoreCase = true) -> "mp4"
                mediaUri.contains(".png", ignoreCase = true) -> "png"
                mediaUri.contains(".webp", ignoreCase = true) -> "webp"
                else -> "jpg"
            }

            if (fileExt in listOf("jpg", "png", "webp")) {
                return@withContext downloadImageToGallery(context, mediaUri, suggestedFileName)
            }

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            val fileName = suggestedFileName?.takeIf { it.isNotBlank() } ?: "Linko_Download_${System.currentTimeMillis()}.$fileExt"
            val targetFile = File(downloadsDir, fileName)

            val bytes = readMediaBytes(context, mediaUri) ?: return@withContext false
            FileOutputStream(targetFile).use { out ->
                out.write(bytes)
                out.flush()
            }

            val category = when (fileExt) {
                "mp4", "mkv" -> MediaCategory.VIDEOS
                else -> MediaCategory.DOCUMENTS
            }
            if (targetFile.exists()) {
                ByteArrayInputStream(bytes).use { input ->
                    MediaDirectoryManager.saveMediaFileClean(context, input, fileName, category)
                }
            }

            Log.d(TAG, "File successfully downloaded to: ${targetFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading media file", e)
            false
        }
    }
}

