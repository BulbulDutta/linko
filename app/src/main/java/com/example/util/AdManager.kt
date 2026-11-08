package com.example.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.ads.AdServicesCompat
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * AdManager manages AdMob initialization and Interstitial Ads for Linko.
 *
 * Fully backward compatible:
 * - Supports Android 8.0 (API 26 / Build.VERSION_CODES.O) and above.
 * - Android 13+ (API 33 / Build.VERSION_CODES.TIRAMISU) Privacy Sandbox AdServices features
 *   are wrapped in version checks to avoid crashing older devices.
 *
 * Configured with:
 * - AdMob App ID: ca-app-pub-9185028793925317~4904991596
 * - Interstitial Ad Unit ID: ca-app-pub-9185028793925317/5312632818
 *
 * Lifecycle Triggers:
 * 1. App entry / launch interstitial.
 * 2. Chat exit interstitial when navigating back from a direct chat frame.
 */
object AdManager {

    private const val TAG = "LinkoAdManager"

    const val ADMOB_APP_ID = "ca-app-pub-9185028793925317~4904991596"
    const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-9185028793925317/5312632818"

    private var isInitialized = false
    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    // App launch ad management
    private var hasShownAppLaunchAd = false
    private var pendingAppLaunchActivity: WeakReference<Activity>? = null

    // Debounce guard to prevent rapid sequential flashes
    private var lastAdShowTime = 0L
    private const val MIN_AD_INTERVAL_MS = 3000L

    private val mainHandler = Handler(Looper.getMainLooper())

    fun initialize(context: Context) {
        if (isInitialized) return
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Initializing Google Mobile Ads on background thread...")
                Log.d(TAG, AdServicesCompat.getDiagnosticSummary(appContext))

                // Version check: Android 13+ (API 33) Privacy Sandbox inspection
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val adServicesEnabled = AdServicesCompat.isAdServicesAvailable(appContext)
                    Log.d(TAG, "Android 13+ Privacy Sandbox AdServices status: $adServicesEnabled")
                } else {
                    Log.d(TAG, "Running on Android ${Build.VERSION.SDK_INT} (< API 33): Fallback to standard Google Play Services ad pipeline.")
                }

                // Global RequestConfiguration optimization across API 26+
                val requestConfig = RequestConfiguration.Builder()
                    .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE)
                    .build()
                MobileAds.setRequestConfiguration(requestConfig)

                MobileAds.initialize(appContext) { status ->
                    isInitialized = true
                    Log.d(TAG, "AdMob initialized successfully: ${status.adapterStatusMap.keys}")
                    loadInterstitial(appContext)
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Notice during MobileAds initialization on API ${Build.VERSION.SDK_INT}: ${e.message}")
            }
        }
    }

    fun loadInterstitial(context: Context, onLoaded: (() -> Unit)? = null) {
        mainHandler.post {
            if (interstitialAd != null) {
                onLoaded?.invoke()
                return@post
            }
            if (isLoading) return@post

            isLoading = true
            val adRequest = AdRequest.Builder().build()
            Log.d(TAG, "Loading Interstitial Ad: $INTERSTITIAL_AD_UNIT_ID on API ${Build.VERSION.SDK_INT}")

            try {
                InterstitialAd.load(
                    context.applicationContext,
                    INTERSTITIAL_AD_UNIT_ID,
                    adRequest,
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: InterstitialAd) {
                            interstitialAd = ad
                            isLoading = false
                            Log.d(TAG, "Interstitial Ad successfully loaded")
                            onLoaded?.invoke()

                            // If user entered application and was waiting for the ad to load, show it
                            val waitingActivity = pendingAppLaunchActivity?.get()
                            if (waitingActivity != null &&
                                !waitingActivity.isFinishing &&
                                !waitingActivity.isDestroyed &&
                                !hasShownAppLaunchAd
                            ) {
                                Log.d(TAG, "Displaying pending app-launch ad")
                                showAppLaunchAd(waitingActivity)
                            }
                        }

                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            interstitialAd = null
                            isLoading = false
                            Log.w(TAG, "Interstitial Ad failed to load on API ${Build.VERSION.SDK_INT}: ${loadAdError.message} (code ${loadAdError.code})")
                        }
                    }
                )
            } catch (t: Throwable) {
                Log.e(TAG, "Exception loading interstitial ad on API ${Build.VERSION.SDK_INT}", t)
                interstitialAd = null
                isLoading = false
            }
        }
    }

    /**
     * Launched when someone enters the application.
     * Shows once per application launch session.
     */
    fun showAppLaunchAd(activity: Activity) {
        mainHandler.post {
            if (hasShownAppLaunchAd) return@post
            if (activity.isFinishing || activity.isDestroyed) return@post

            val ad = interstitialAd
            if (ad != null) {
                hasShownAppLaunchAd = true
                pendingAppLaunchActivity = null
                showAdInternal(activity) {
                    Log.d(TAG, "App launch ad completed")
                }
            } else {
                // Register interest to show as soon as loaded
                pendingAppLaunchActivity = WeakReference(activity)
                loadInterstitial(activity.applicationContext)
            }
        }
    }

    /**
     * Launched when someone exits the chat frame after clicking into a chat.
     * If ready, shows the ad then triggers onDone().
     * If not ready or failed, immediately triggers onDone() so user experience is not delayed.
     */
    fun showChatExitAd(activity: Activity, onDone: () -> Unit) {
        mainHandler.post {
            if (activity.isFinishing || activity.isDestroyed) {
                onDone()
                return@post
            }

            val now = System.currentTimeMillis()
            val ad = interstitialAd

            if (ad != null && (now - lastAdShowTime >= MIN_AD_INTERVAL_MS)) {
                Log.d(TAG, "Showing interstitial ad upon exiting chat frame")
                showAdInternal(activity) {
                    onDone()
                }
            } else {
                Log.d(TAG, "No interstitial ad ready or rate-limited; proceeding without delay")
                onDone()
                // Preload in background for subsequent exits
                loadInterstitial(activity.applicationContext)
            }
        }
    }

    private fun showAdInternal(activity: Activity, onFinished: () -> Unit) {
        val ad = interstitialAd
        if (ad == null || activity.isFinishing || activity.isDestroyed) {
            onFinished()
            return
        }

        lastAdShowTime = System.currentTimeMillis()

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial ad dismissed by user")
                interstitialAd = null
                loadInterstitial(activity.applicationContext)
                onFinished()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.w(TAG, "Interstitial ad failed to show on API ${Build.VERSION.SDK_INT}: ${adError.message}")
                interstitialAd = null
                loadInterstitial(activity.applicationContext)
                onFinished()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Interstitial ad showed full screen content")
            }
        }

        try {
            ad.show(activity)
        } catch (t: Throwable) {
            Log.e(TAG, "Error invoking ad.show() on API ${Build.VERSION.SDK_INT}", t)
            interstitialAd = null
            loadInterstitial(activity.applicationContext)
            onFinished()
        }
    }
}

/**
 * Public extension to safely find the host Activity from any Context across Android 8.0+ (API 26+).
 */
fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
