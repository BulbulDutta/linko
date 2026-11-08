package com.example.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.util.Log
import java.io.File

object AudioPlayerManager {
    private var mediaPlayer: MediaPlayer? = null
    private var currentlyPlayingUri: String? = null
    private var isPaused: Boolean = false
    private var currentSpeed: Float = 1.0f
    private var activeCompletionCallback: (() -> Unit)? = null

    fun playAudio(
        context: Context,
        audioUri: String,
        speed: Float = currentSpeed,
        onCompletion: () -> Unit
    ) {
        try {
            currentSpeed = speed
            if (currentlyPlayingUri == audioUri && mediaPlayer != null) {
                val player = mediaPlayer!!
                if (player.isPlaying) {
                    pauseAudio()
                    return
                } else if (isPaused) {
                    applySpeed(player, currentSpeed)
                    player.start()
                    isPaused = false
                    activeCompletionCallback = onCompletion
                    return
                }
            }

            stopAudio()
            activeCompletionCallback = onCompletion

            val player = MediaPlayer()
            val cachedRemotePath = com.example.data.remote.FirebaseVoiceNoteStorageService
                .getCachedFilePathIfAvailable(context, audioUri)
            val cleanPath = (cachedRemotePath ?: audioUri).removePrefix("file://")
            val localFile = File(cleanPath)
            val isRemoteNetworkUrl = !localFile.exists() &&
                (audioUri.startsWith("http://", ignoreCase = true) ||
                    audioUri.startsWith("https://", ignoreCase = true))

            when {
                localFile.exists() -> {
                    player.setDataSource(localFile.absolutePath)
                }
                audioUri.startsWith("content://") || audioUri.startsWith("file://") -> {
                    player.setDataSource(context, Uri.parse(audioUri))
                }
                audioUri.startsWith("/") -> {
                    player.setDataSource(audioUri)
                }
                else -> {
                    player.setDataSource(context, Uri.parse(audioUri))
                }
            }

            player.setOnCompletionListener {
                isPaused = false
                currentlyPlayingUri = null
                activeCompletionCallback?.invoke()
                activeCompletionCallback = null
            }

            mediaPlayer = player
            currentlyPlayingUri = audioUri
            isPaused = false

            if (isRemoteNetworkUrl) {
                player.setOnPreparedListener { mp ->
                    applySpeed(mp, currentSpeed)
                    mp.start()
                }
                player.setOnErrorListener { _, _, _ ->
                    stopAudio()
                    onCompletion()
                    true
                }
                player.prepareAsync()
            } else {
                player.prepare()
                applySpeed(player, currentSpeed)
                player.start()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error playing audio file: $audioUri", e)
            stopAudio()
            onCompletion()
        }
    }

    private fun applySpeed(player: MediaPlayer, speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                player.playbackParams = PlaybackParams().setSpeed(speed.coerceIn(0.5f, 2.5f))
            } catch (_: Exception) {}
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        currentSpeed = speed
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            applySpeed(player, speed)
        }
    }

    fun pauseAudio() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                isPaused = true
            }
        } catch (_: Exception) {}
    }

    fun seekToFraction(audioUri: String, fraction: Float) {
        try {
            if (currentlyPlayingUri == audioUri && mediaPlayer != null) {
                val duration = mediaPlayer?.duration ?: 0
                if (duration > 0) {
                    val targetMs = (duration * fraction.coerceIn(0f, 1f)).toInt()
                    mediaPlayer?.seekTo(targetMs)
                }
            }
        } catch (_: Exception) {}
    }

    fun getProgress(audioUri: String): Float {
        return try {
            if (currentlyPlayingUri == audioUri && mediaPlayer != null) {
                val duration = mediaPlayer?.duration ?: 0
                val position = mediaPlayer?.currentPosition ?: 0
                if (duration > 0) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
            } else {
                0f
            }
        } catch (_: Exception) {
            0f
        }
    }

    fun getCurrentPositionMs(audioUri: String): Int {
        return try {
            if (currentlyPlayingUri == audioUri && mediaPlayer != null) {
                (mediaPlayer?.currentPosition ?: 0).coerceAtLeast(0)
            } else {
                0
            }
        } catch (_: Exception) {
            0
        }
    }

    fun getDurationMs(context: Context, audioUri: String): Int {
        if (audioUri.isBlank()) return 0
        try {
            if (currentlyPlayingUri == audioUri && mediaPlayer != null) {
                val d = mediaPlayer?.duration ?: 0
                if (d > 0) return d
            }
            val cachedRemotePath = com.example.data.remote.FirebaseVoiceNoteStorageService
                .getCachedFilePathIfAvailable(context, audioUri)
            val resolvedUri = cachedRemotePath ?: audioUri
            if (resolvedUri.startsWith("http://", ignoreCase = true) ||
                resolvedUri.startsWith("https://", ignoreCase = true) ||
                resolvedUri.startsWith("gs://", ignoreCase = true)
            ) {
                return 0
            }
            val retriever = MediaMetadataRetriever()
            try {
                val cleanPath = resolvedUri.removePrefix("file://")
                val file = File(cleanPath)
                if (file.exists()) {
                    retriever.setDataSource(file.absolutePath)
                } else {
                    retriever.setDataSource(context, Uri.parse(resolvedUri))
                }
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                return durationStr?.toIntOrNull() ?: 0
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {
            return 0
        }
    }

    fun isPlaying(audioUri: String): Boolean {
        return try {
            currentlyPlayingUri == audioUri && mediaPlayer?.isPlaying == true
        } catch (_: Exception) {
            false
        }
    }

    fun stopAudio() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null
        currentlyPlayingUri = null
        isPaused = false
        activeCompletionCallback = null
    }
}
