package com.example.data.remote

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.util.ImageStorageHelper
import com.google.firebase.Firebase
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles uploading recorded voice note audio clips (.m4a) to Firebase Storage
 * (`voice_notes/{chatId}/{fileName}`) and resolving remote Firebase Storage download URLs
 * with local caching for instant waveform playback and scrubbing.
 */
object FirebaseVoiceNoteStorageService {
    private const val TAG = "FirebaseVoiceNoteStorage"

    // Maps remote Firebase Storage download URLs to local cached file paths for zero-latency playback
    private val localCacheByRemoteUrl = ConcurrentHashMap<String, String>()

    data class VoiceNoteUploadResult(
        val audioUri: String,
        val localCachedPath: String?,
        val uploadedToFirebaseStorage: Boolean
    )

    /**
     * Uploads a recorded voice note [audioFile] to Firebase Storage under
     * `voice_notes/{sanitizedChatId}/{fileName}` and returns the downloadable URL.
     *
     * Also saves a local persistent copy in Linko's storage so playback works immediately
     * without re-downloading, and gracefully falls back to the local URI if the Firebase Storage
     * bucket has not yet been initialized in the Firebase Console.
     */
    suspend fun uploadVoiceNote(
        context: Context,
        chatId: String,
        audioFile: File,
        durationMs: Long,
        onProgress: (Float) -> Unit = {}
    ): VoiceNoteUploadResult = withContext(Dispatchers.IO) {
        // 1. Always preserve a local persistent copy for immediate local playback & fallback
        val localSavedUri = ImageStorageHelper.saveUriToInternalStorage(
            context = context,
            contentUri = Uri.fromFile(audioFile)
        )
        val fallbackLocalUriString = localSavedUri?.toString() ?: Uri.fromFile(audioFile).toString()
        val localFilePath = localSavedUri?.path ?: audioFile.absolutePath

        if (!audioFile.exists() || audioFile.length() == 0L) {
            onProgress(1f)
            return@withContext VoiceNoteUploadResult(
                audioUri = fallbackLocalUriString,
                localCachedPath = localFilePath,
                uploadedToFirebaseStorage = false
            )
        }

        val sanitizedChatId = chatId
            .lowercase()
            .replace(Regex("[^a-z0-9._-]"), "_")
            .ifBlank { "direct_chat" }

        val fileName = audioFile.name.ifBlank { "voice_note_${System.currentTimeMillis()}.m4a" }
        val storagePath = "voice_notes/$sanitizedChatId/$fileName"

        try {
            onProgress(0.08f)
            val storageRef = Firebase.storage.reference.child(storagePath)
            val metadata = StorageMetadata.Builder()
                .setContentType("audio/mp4")
                .setCustomMetadata("chatId", chatId)
                .setCustomMetadata("durationMs", durationMs.toString())
                .build()

            val uploadTask = storageRef.putFile(Uri.fromFile(audioFile), metadata)
            uploadTask.addOnProgressListener { snapshot ->
                val total = snapshot.totalByteCount
                if (total > 0) {
                    val fraction = (snapshot.bytesTransferred.toFloat() / total.toFloat()).coerceIn(0.08f, 0.95f)
                    onProgress(fraction)
                }
            }

            uploadTask.await()
            val downloadUri = storageRef.downloadUrl.await()
            val remoteUrlString = downloadUri.toString()

            if (localFilePath.isNotBlank()) {
                localCacheByRemoteUrl[remoteUrlString] = localFilePath
            }
            onProgress(1f)
            Log.d(TAG, "Voice note uploaded to Firebase Storage: $remoteUrlString")

            VoiceNoteUploadResult(
                audioUri = remoteUrlString,
                localCachedPath = localFilePath,
                uploadedToFirebaseStorage = true
            )
        } catch (e: Exception) {
            Log.w(
                TAG,
                "Firebase Storage upload failed (ensure Firebase Storage is enabled in Console); using persistent local audio URI fallback: ${e.message}"
            )
            onProgress(1f)
            VoiceNoteUploadResult(
                audioUri = fallbackLocalUriString,
                localCachedPath = localFilePath,
                uploadedToFirebaseStorage = false
            )
        }
    }

    /**
     * Returns a local cached file path if the given [remoteOrLocalUri] has already been cached
     * on this device, or null if not yet cached locally.
     */
    fun getCachedFilePathIfAvailable(context: Context, remoteOrLocalUri: String): String? {
        if (remoteOrLocalUri.isBlank()) return null
        localCacheByRemoteUrl[remoteOrLocalUri]?.let { cached ->
            if (File(cached).exists()) return cached
        }
        if (remoteOrLocalUri.startsWith("http://") || remoteOrLocalUri.startsWith("https://")) {
            val cacheFile = getDeterministicCacheFile(context, remoteOrLocalUri)
            if (cacheFile.exists() && cacheFile.length() > 0L) {
                localCacheByRemoteUrl[remoteOrLocalUri] = cacheFile.absolutePath
                return cacheFile.absolutePath
            }
        }
        return null
    }

    /**
     * Downloads and caches a remote Firebase Storage voice note URL to the app's cache directory
     * so subsequent plays and waveform seeks are instantaneous.
     */
    suspend fun ensureCachedAudioFile(context: Context, remoteUrl: String): File? = withContext(Dispatchers.IO) {
        try {
            getCachedFilePathIfAvailable(context, remoteUrl)?.let { existingPath ->
                val existingFile = File(existingPath)
                if (existingFile.exists() && existingFile.length() > 0L) {
                    return@withContext existingFile
                }
            }

            val targetFile = getDeterministicCacheFile(context, remoteUrl)
            if (remoteUrl.startsWith("gs://")) {
                val storageRef = Firebase.storage.getReferenceFromUrl(remoteUrl)
                storageRef.getFile(targetFile).await()
            } else {
                val connection = (URL(remoteUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10_000
                    readTimeout = 15_000
                    doInput = true
                }
                connection.inputStream.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                        output.flush()
                    }
                }
            }

            if (targetFile.exists() && targetFile.length() > 0L) {
                localCacheByRemoteUrl[remoteUrl] = targetFile.absolutePath
                targetFile
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cache remote voice note: $remoteUrl", e)
            null
        }
    }

    private fun getDeterministicCacheFile(context: Context, remoteUrl: String): File {
        val cacheDir = File(context.cacheDir, "firebase_voice_notes").apply {
            if (!exists()) mkdirs()
        }
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(remoteUrl.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(24)
        return File(cacheDir, "vn_$hash.m4a")
    }
}
