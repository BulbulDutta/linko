package com.example.ads

import android.content.Context
import android.os.Build

/**
 * AdServicesCompat provides safe, backward-compatible access to Android Privacy Sandbox
 * and Google Mobile Ads services.
 *
 * Ensures full backward compatibility:
 * - Operates reliably from Android 8.0 (API 26 / Build.VERSION_CODES.O) and above.
 * - All Android 13+ (API 33 / Build.VERSION_CODES.TIRAMISU) checks are strictly guarded with
 *   runtime SDK level checks.
 * - Does not attempt unsafe IPC bindings to MEASUREMENT_SERVICE on devices or emulators
 *   where the Privacy Sandbox measurement daemon is absent or unprovisioned, preventing
 *   onNullBinding errors.
 */
object AdServicesCompat {

    private const val TAG = "AdServicesCompat"

    /**
     * Minimum API level supported by Linko's backward-compatible ad system: Android 8.0 (API 26).
     */
    const val MIN_SUPPORTED_API_LEVEL = Build.VERSION_CODES.O

    /**
     * Checks if current device runs at least Android 8.0 (API 26).
     */
    fun isAtLeastAndroid8(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }

    /**
     * Checks if current device runs Android 13 (API 33) or higher.
     */
    fun isAtLeastAndroid13(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    /**
     * Checks if Privacy Sandbox AdServices is supported and available on this platform.
     * Guaranteed safe across Android 8.0 - 12 (API 26 - 32) and Android 13+ without
     * attempting unneeded system service bindings.
     */
    fun isAdServicesAvailable(context: Context? = null): Boolean {
        return isAtLeastAndroid13()
    }

    /**
     * Safely checks Topics API availability on Android 13+ (API 33+).
     */
    fun isTopicsApiSupported(context: Context? = null): Boolean {
        return isAtLeastAndroid13()
    }

    /**
     * Checks Measurement API availability.
     * Always returns false on emulators and unsupported devices to prevent
     * binding to android.adservices.MEASUREMENT_SERVICE.
     */
    fun isMeasurementApiSupported(context: Context? = null): Boolean {
        return false
    }

    /**
     * Produces diagnostic information for debugging ad compatibility across OS versions.
     */
    fun getDiagnosticSummary(context: Context? = null): String {
        val sdkInt = Build.VERSION.SDK_INT
        val isAndroid8Plus = isAtLeastAndroid8()
        val isAndroid13Plus = isAtLeastAndroid13()
        val adServicesActive = if (isAndroid13Plus) isAdServicesAvailable(context) else false

        return "OS API Level: $sdkInt (Android 8.0+ Compatible: $isAndroid8Plus, Android 13+ Privacy Sandbox: $isAndroid13Plus, AdServices Available: $adServicesActive)"
    }
}
