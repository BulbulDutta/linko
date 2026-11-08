package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MediaCategory(val folderName: String, val displayName: String) {
    PHOTOS("Photos", "Photos"),
    VIDEOS("Videos", "Videos"),
    DOCUMENTS("Documents", "Documents");

    companion object {
        fun fromMimeType(mimeType: String, fileName: String): MediaCategory {
            val mime = mimeType.lowercase()
            val ext = fileName.substringAfterLast(".", "").lowercase()
            return when {
                mime.startsWith("image/") || ext in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp") -> PHOTOS
                mime.startsWith("video/") || ext in listOf("mp4", "mkv", "webm", "avi", "mov", "3gp") -> VIDEOS
                else -> DOCUMENTS
            }
        }
    }
}

data class MediaFileInfo(
    val id: String,
    val file: File,
    val name: String,
    val category: MediaCategory,
    val sizeBytes: Long,
    val formattedSize: String,
    val mimeType: String,
    val lastModified: Long,
    val formattedDate: String,
    val uri: Uri
)

data class StorageSummary(
    val totalBytes: Long,
    val formattedTotalSize: String,
    val photosBytes: Long,
    val formattedPhotosSize: String,
    val photosCount: Int,
    val videosBytes: Long,
    val formattedVideosSize: String,
    val videosCount: Int,
    val documentsBytes: Long,
    val formattedDocumentsSize: String,
    val documentsCount: Int
)

object MediaDirectoryManager {
    private const val TAG = "MediaDirectoryManager"
    private const val ROOT_FOLDER_NAME = "Linko"
    private const val MEDIA_FOLDER_NAME = "Media"

    /**
     * Returns all potential directory locations for Linko media, creating them on disk.
     * Primary location is the public external storage directory (/storage/emulated/0/Linko/Media/<Category>),
     * which makes folders physically visible in the device's default File Manager.
     */
    fun getCategoryDirectories(context: Context, category: MediaCategory): List<File> {
        val dirs = mutableListOf<File>()

        // 1. Public External Storage Directory (Visible in Phone File Manager)
        try {
            val externalStorage = android.os.Environment.getExternalStorageDirectory()
            if (externalStorage != null) {
                val publicDir = File(externalStorage, "$ROOT_FOLDER_NAME/$MEDIA_FOLDER_NAME/${category.folderName}")
                if (!publicDir.exists()) {
                    val created = publicDir.mkdirs()
                    Log.d(TAG, "Created public directory ${publicDir.absolutePath}: $created")
                }
                dirs.add(publicDir)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining public external storage directory", e)
        }

        // 2. Secondary External App Storage Directory
        try {
            val extAppDir = context.getExternalFilesDir(null)
            if (extAppDir != null) {
                val appMediaDir = File(extAppDir, "$ROOT_FOLDER_NAME/$MEDIA_FOLDER_NAME/${category.folderName}")
                if (!appMediaDir.exists()) {
                    appMediaDir.mkdirs()
                }
                dirs.add(appMediaDir)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining external app files directory", e)
        }

        // 3. Internal Files Directory (Fallback)
        val internalDir = File(context.filesDir, "$ROOT_FOLDER_NAME/$MEDIA_FOLDER_NAME/${category.folderName}")
        if (!internalDir.exists()) {
            internalDir.mkdirs()
        }
        dirs.add(internalDir)

        return dirs
    }

    /**
     * Gets the primary writable dedicated local media directory for Linko (e.g. /storage/emulated/0/Linko/Media/<Category>).
     * Ensures sub-folders (Photos, Videos, Documents) are automatically created and physically accessible.
     */
    fun getCategoryDirectory(context: Context, category: MediaCategory): File {
        val dirs = getCategoryDirectories(context, category)
        return dirs.firstOrNull { it.canWrite() } ?: dirs.first()
    }

    /**
     * Formats raw bytes into clean human-readable footprint strings (e.g. "820 KB", "3.2 MB", "1.5 GB").
     */
    fun formatFileSize(sizeBytes: Long): String {
        if (sizeBytes <= 0) return "0 KB"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(sizeBytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        val value = sizeBytes / Math.pow(1024.0, index.toDouble())
        val df = DecimalFormat("#,##0.#")
        return "${df.format(value)} ${units[index]}"
    }

    /**
     * Saves raw media stream into Linko/Media/<Category> clean without watermarks, text, or file size modifications.
     */
    suspend fun saveMediaFileClean(
        context: Context,
        inputStream: InputStream,
        originalFileName: String,
        category: MediaCategory
    ): MediaFileInfo? = withContext(Dispatchers.IO) {
        try {
            val dir = getCategoryDirectory(context, category)
            val cleanFileName = if (originalFileName.isBlank()) {
                "Linko_${category.folderName}_${System.currentTimeMillis()}"
            } else {
                originalFileName
            }
            val targetFile = File(dir, cleanFileName)

            FileOutputStream(targetFile).use { out ->
                inputStream.copyTo(out)
                out.flush()
            }

            Log.d(TAG, "Saved clean media file to: ${targetFile.absolutePath}")
            createFileInfo(targetFile, category)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving clean media file", e)
            null
        }
    }

    /**
     * Saves a clean Bitmap image directly to Linko/Media/Photos without adding any watermark, text, or overlay.
     */
    suspend fun saveBitmapClean(
        context: Context,
        bitmap: Bitmap,
        preferredName: String? = null
    ): MediaFileInfo? = withContext(Dispatchers.IO) {
        try {
            val dir = getCategoryDirectory(context, MediaCategory.PHOTOS)
            val fileName = preferredName?.takeIf { it.isNotBlank() }
                ?: "Linko_Photo_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg"
            val targetFile = File(dir, fileName)

            FileOutputStream(targetFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                out.flush()
            }

            Log.d(TAG, "Saved clean bitmap image to: ${targetFile.absolutePath}")
            createFileInfo(targetFile, MediaCategory.PHOTOS)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving clean bitmap", e)
            null
        }
    }

    /**
     * Retrieves all stored media files across Linko/Media directories, categorized into Photos, Videos, and Documents.
     * Guaranteed strictly no mock data; only actual physical files present in Linko/Media directories are returned.
     */
    suspend fun getAllStoredMediaFiles(
        context: Context,
        filterCategory: MediaCategory? = null
    ): List<MediaFileInfo> = withContext(Dispatchers.IO) {
        val categories = if (filterCategory != null) listOf(filterCategory) else MediaCategory.values().toList()
        val result = mutableListOf<MediaFileInfo>()
        val seenPaths = mutableSetOf<String>()

        for (cat in categories) {
            val dirs = getCategoryDirectories(context, cat)
            for (dir in dirs) {
                val files = dir.listFiles() ?: emptyArray()
                for (file in files) {
                    if (file.isFile && file.length() > 0 && !seenPaths.contains(file.absolutePath)) {
                        seenPaths.add(file.absolutePath)
                        val fileInfo = createFileInfo(file, cat)
                        result.add(fileInfo)
                    }
                }
            }
        }

        result.sortedByDescending { it.lastModified }
    }

    /**
     * Deletes a list of files and returns the total storage bytes freed up.
     */
    suspend fun deleteMediaFiles(files: List<File>): Long = withContext(Dispatchers.IO) {
        var bytesFreed = 0L
        for (file in files) {
            if (file.exists()) {
                val len = file.length()
                if (file.delete()) {
                    bytesFreed += len
                    Log.d(TAG, "Deleted media file: ${file.name}, freed $len bytes")
                }
            }
        }
        bytesFreed
    }

    /**
     * Clears all media cache for a specific category across all Linko/Media locations.
     */
    suspend fun clearCategoryMedia(context: Context, category: MediaCategory): Long = withContext(Dispatchers.IO) {
        val dirs = getCategoryDirectories(context, category)
        var totalFreed = 0L
        for (dir in dirs) {
            val files = dir.listFiles() ?: emptyArray()
            totalFreed += deleteMediaFiles(files.toList())
        }
        totalFreed
    }

    /**
     * Calculates storage footprint breakdown across Photos, Videos, and Documents.
     * Guaranteed strictly based on real files in disk; returns 0 MB when empty.
     */
    suspend fun getStorageSummary(context: Context): StorageSummary = withContext(Dispatchers.IO) {
        var photosBytes = 0L
        var photosCount = 0
        var videosBytes = 0L
        var videosCount = 0
        var docsBytes = 0L
        var docsCount = 0
        val seenPaths = mutableSetOf<String>()

        for (cat in MediaCategory.values()) {
            val dirs = getCategoryDirectories(context, cat)
            for (dir in dirs) {
                val files = dir.listFiles() ?: emptyArray()
                for (file in files) {
                    if (file.isFile && file.length() > 0 && !seenPaths.contains(file.absolutePath)) {
                        seenPaths.add(file.absolutePath)
                        when (cat) {
                            MediaCategory.PHOTOS -> {
                                photosBytes += file.length()
                                photosCount++
                            }
                            MediaCategory.VIDEOS -> {
                                videosBytes += file.length()
                                videosCount++
                            }
                            MediaCategory.DOCUMENTS -> {
                                docsBytes += file.length()
                                docsCount++
                            }
                        }
                    }
                }
            }
        }

        val total = photosBytes + videosBytes + docsBytes

        StorageSummary(
            totalBytes = total,
            formattedTotalSize = formatFileSize(total),
            photosBytes = photosBytes,
            formattedPhotosSize = formatFileSize(photosBytes),
            photosCount = photosCount,
            videosBytes = videosBytes,
            formattedVideosSize = formatFileSize(videosBytes),
            videosCount = videosCount,
            documentsBytes = docsBytes,
            formattedDocumentsSize = formatFileSize(docsBytes),
            documentsCount = docsCount
        )
    }

    private fun createFileInfo(file: File, category: MediaCategory): MediaFileInfo {
        val extension = file.extension.lowercase()
        val mimeType = when (extension) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "pdf" -> "application/pdf"
            "txt" -> "text/plain"
            "doc", "docx" -> "application/msword"
            else -> "application/octet-stream"
        }

        val dateFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(file.lastModified()))

        return MediaFileInfo(
            id = file.absolutePath.hashCode().toString(),
            file = file,
            name = file.name,
            category = category,
            sizeBytes = file.length(),
            formattedSize = formatFileSize(file.length()),
            mimeType = mimeType,
            lastModified = file.lastModified(),
            formattedDate = formattedDate,
            uri = Uri.fromFile(file)
        )
    }
}
