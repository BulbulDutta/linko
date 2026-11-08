package com.example.ads

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * AdMobManager provides unified Google Mobile Ads integration for Linko.
 *
 * Fully backward-compatible:
 * - Operates reliably from Android 8.0 (API 26) through Android 14+.
 * - Android 13+ (API 33 / TIRAMISU) Privacy Sandbox AdServices features are guarded with
 *   version checks to prevent crashing on older devices.
 *
 * Configured IDs:
 * - Sample App ID: ca-app-pub-9185028793925317~4904991596
 * - Banner Test Ad Unit ID: ca-app-pub-9185028793925317/8746987500
 * - Interstitial Test Ad Unit ID: ca-app-pub-9185028793925317/5312632818
 */
object AdMobManager {

    private const val TAG = "AdMobManager"

    const val SAMPLE_APP_ID = "ca-app-pub-9185028793925317~4904991596"
    const val BANNER_TEST_AD_UNIT_ID = "ca-app-pub-9185028793925317/8746987500"
    const val INTERSTITIAL_TEST_AD_UNIT_ID = "ca-app-pub-9185028793925317/5312632818"

    private var isInitialized = false
    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false
    private var lastAdShownTimestamp = 0L

    // Minimum cooldown period between interstitial ad displays to avoid spamming the user
    private const val MIN_INTERSTITIAL_COOLDOWN_MS = 35_000L

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Initializes the Google Mobile Ads SDK on a background thread.
     * Safe across all Android versions from API 26 (Android 8.0) to API 34+.
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Initializing Google Mobile Ads SDK on background thread...")
                Log.d(TAG, AdServicesCompat.getDiagnosticSummary(appContext))

                // Version check: Android 13+ (API 33, TIRAMISU) Privacy Sandbox inspection
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val isAdServicesActive = AdServicesCompat.isAdServicesAvailable(appContext)
                    Log.d(TAG, "Android 13+ Privacy Sandbox AdServices active: $isAdServicesActive")
                } else {
                    Log.d(TAG, "Running on Android ${Build.VERSION.SDK_INT} (< API 33): Using standard Google Play Services pipeline.")
                }

                // Global RequestConfiguration optimization
                val requestConfig = RequestConfiguration.Builder()
                    .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE)
                    .build()
                MobileAds.setRequestConfiguration(requestConfig)

                MobileAds.initialize(appContext) { status ->
                    isInitialized = true
                    Log.d(TAG, "GMA SDK initialized successfully. Adapters: ${status.adapterStatusMap.keys}")
                    preloadInterstitial(appContext)
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Notice during GMA SDK initialization on API ${Build.VERSION.SDK_INT}: ${e.message}")
            }
        }
    }

    /**
     * Builds a standard test ad request compatible across all supported Android versions.
     */
    fun buildAdRequest(): AdRequest {
        return AdRequest.Builder().build()
    }

    /**
     * Preloads an interstitial ad ahead of time.
     */
    fun preloadInterstitial(context: Context) {
        mainHandler.post {
            if (interstitialAd != null || isInterstitialLoading) {
                return@post
            }

            isInterstitialLoading = true
            Log.d(TAG, "Preloading interstitial ad with ID: $INTERSTITIAL_TEST_AD_UNIT_ID on API ${Build.VERSION.SDK_INT}")

            try {
                InterstitialAd.load(
                    context.applicationContext,
                    INTERSTITIAL_TEST_AD_UNIT_ID,
                    buildAdRequest(),
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: InterstitialAd) {
                            Log.d(TAG, "Interstitial ad loaded successfully.")
                            interstitialAd = ad
                            isInterstitialLoading = false
                        }

                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            Log.w(
                                TAG,
                                "Interstitial ad failed to load on API ${Build.VERSION.SDK_INT}: ${loadAdError.message} (Code: ${loadAdError.code})"
                            )
                            interstitialAd = null
                            isInterstitialLoading = false
                        }
                    }
                )
            } catch (t: Throwable) {
                Log.e(TAG, "Exception loading interstitial on API ${Build.VERSION.SDK_INT}", t)
                interstitialAd = null
                isInterstitialLoading = false
            }
        }
    }

    /**
     * Shows the preloaded interstitial ad if available and not throttled by the cooldown timer.
     * Executes [onDismissedOrCompleted] once the ad is closed or immediately if ad is not ready.
     */
    fun showInterstitial(
        activity: Activity,
        force: Boolean = false,
        onDismissedOrCompleted: () -> Unit
    ) {
        mainHandler.post {
            if (activity.isFinishing || activity.isDestroyed) {
                onDismissedOrCompleted()
                return@post
            }

            val currentAd = interstitialAd
            val now = System.currentTimeMillis()

            if (currentAd == null) {
                Log.d(TAG, "Interstitial ad not ready. Resuming transition without delay.")
                preloadInterstitial(activity.applicationContext)
                onDismissedOrCompleted()
                return@post
            }

            if (!force && (now - lastAdShownTimestamp < MIN_INTERSTITIAL_COOLDOWN_MS)) {
                Log.d(TAG, "Interstitial throttled to prevent frequent popups. Continuing transition.")
                onDismissedOrCompleted()
                return@post
            }

            currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad displayed full-screen.")
                    lastAdShownTimestamp = System.currentTimeMillis()
                    interstitialAd = null
                }

                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad dismissed by user.")
                    interstitialAd = null
                    preloadInterstitial(activity.applicationContext)
                    onDismissedOrCompleted()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w(TAG, "Interstitial ad failed to show: ${adError.message} (Code: ${adError.code})")
                    interstitialAd = null
                    preloadInterstitial(activity.applicationContext)
                    onDismissedOrCompleted()
                }

                override fun onAdImpression() {
                    Log.d(TAG, "Interstitial ad impression logged.")
                }

                override fun onAdClicked() {
                    Log.d(TAG, "Interstitial ad clicked.")
                }
            }

            try {
                currentAd.show(activity)
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to invoke currentAd.show() on API ${Build.VERSION.SDK_INT}", e)
                interstitialAd = null
                preloadInterstitial(activity.applicationContext)
                onDismissedOrCompleted()
            }
        }
    }
}
