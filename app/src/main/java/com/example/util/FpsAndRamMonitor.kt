package com.example.util

import android.app.ActivityManager
import android.content.Context
import android.view.Choreographer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

/**
 * Real-time RAM snapshot representing actual physical device memory from ActivityManager.MemoryInfo.
 */
data class RealtimeRamInfo(
    val availMemMb: Long,
    val totalMemMb: Long,
    val usedMemMb: Long,
    val usedPercentage: Int,
    val formattedSummary: String
)

object FpsAndRamMonitor {

    /**
     * Reads actual physical hardware/OS memory info dynamically using ActivityManager.MemoryInfo.
     * Never uses hardcoded or fake strings.
     */
    fun getMemoryInfo(context: Context): RealtimeRamInfo {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)

        val totalMb = memInfo.totalMem / (1024 * 1024)
        val availMb = memInfo.availMem / (1024 * 1024)
        val usedMb = maxOf(0L, totalMb - availMb)
        val usedPercentage = if (totalMb > 0) {
            ((usedMb.toDouble() / totalMb.toDouble()) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }

        val formattedSummary = if (availMb >= 1024) {
            String.format(Locale.US, "%.1f GB Free", availMb / 1024.0)
        } else {
            "$availMb MB Free"
        }

        return RealtimeRamInfo(
            availMemMb = availMb,
            totalMemMb = totalMb,
            usedMemMb = usedMb,
            usedPercentage = usedPercentage,
            formattedSummary = formattedSummary
        )
    }
}

/**
 * Composable hook to track the REAL dynamic UI frame rate using Choreographer.FrameCallback.
 * Controlled by [enabled] flag so it only runs when Game Turbo is active, avoiding lag and battery drain.
 */
@Composable
fun rememberRealtimeFps(enabled: Boolean = true): State<Int> {
    val fpsState = remember { mutableIntStateOf(60) }

    DisposableEffect(enabled) {
        if (!enabled) {
            return@DisposableEffect onDispose {}
        }

        var lastFrameTimeNanos = 0L
        var frameCount = 0
        var accumulatedTimeNanos = 0L

        val frameCallback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (lastFrameTimeNanos != 0L) {
                    val frameDelta = frameTimeNanos - lastFrameTimeNanos
                    accumulatedTimeNanos += frameDelta
                    frameCount++

                    // Calculate real FPS every 500ms
                    if (accumulatedTimeNanos >= 500_000_000L) {
                        val calculatedFps = (frameCount * 1_000_000_000L / accumulatedTimeNanos).toInt()
                        fpsState.intValue = calculatedFps.coerceIn(1, 144)
                        frameCount = 0
                        accumulatedTimeNanos = 0L
                    }
                }
                lastFrameTimeNanos = frameTimeNanos
                Choreographer.getInstance().postFrameCallback(this)
            }
        }

        Choreographer.getInstance().postFrameCallback(frameCallback)

        onDispose {
            Choreographer.getInstance().removeFrameCallback(frameCallback)
        }
    }

    return fpsState
}

/**
 * Composable hook to monitor real available RAM dynamically.
 * Controlled by [enabled] flag to prevent background battery drain.
 */
@Composable
fun rememberRealtimeRam(
    enabled: Boolean = true,
    refreshIntervalMs: Long = 2000L
): State<RealtimeRamInfo> {
    val context = LocalContext.current
    val ramState = remember { mutableStateOf(FpsAndRamMonitor.getMemoryInfo(context)) }

    LaunchedEffect(enabled, refreshIntervalMs) {
        if (!enabled) return@LaunchedEffect

        // Immediately read current memory
        ramState.value = FpsAndRamMonitor.getMemoryInfo(context)

        while (isActive) {
            delay(refreshIntervalMs)
            ramState.value = FpsAndRamMonitor.getMemoryInfo(context)
        }
    }

    return ramState
}
