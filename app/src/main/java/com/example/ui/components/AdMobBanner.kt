package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ads.AdMobManager
import com.example.ads.AdServicesCompat
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

private const val TAG = "AdMobBanner"

/**
 * AdMobBanner renders a Google Mobile Ads test banner with full lifecycle management,
 * graceful fallback, and verified backward compatibility for Android 8.0 (API 26) through Android 14+.
 */
@Composable
fun AdMobBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = AdMobManager.BANNER_TEST_AD_UNIT_ID
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isAdLoaded by remember { mutableStateOf(false) }
    var adLoadError by remember { mutableStateOf<String?>(null) }
    var adViewInstance by remember { mutableStateOf<AdView?>(null) }

    // Resolve host Activity safely to guarantee WebViews initialize correctly on Android 8.0+
    val hostActivity = remember(context) { context.findActivity() }
    val effectiveContext = hostActivity ?: context

    DisposableEffect(lifecycleOwner, adViewInstance) {
        val targetAdView = adViewInstance
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    try {
                        targetAdView?.resume()
                    } catch (e: Throwable) {
                        Log.e(TAG, "Error resuming AdView on API ${Build.VERSION.SDK_INT}", e)
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    try {
                        targetAdView?.pause()
                    } catch (e: Throwable) {
                        Log.e(TAG, "Error pausing AdView on API ${Build.VERSION.SDK_INT}", e)
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    try {
                        targetAdView?.destroy()
                    } catch (e: Throwable) {
                        Log.e(TAG, "Error destroying AdView on API ${Build.VERSION.SDK_INT}", e)
                    }
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                targetAdView?.let { adView ->
                    (adView.parent as? ViewGroup)?.removeView(adView)
                    adView.destroy()
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error destroying AdView on dispose (API ${Build.VERSION.SDK_INT})", e)
            }
        }
    }

    // Outer container with testTag
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .testTag("admob_banner_container"),
        contentAlignment = Alignment.Center
    ) {
        // Only render wrapper when not in an unrecoverable failure state
        if (!isAdLoaded && adLoadError != null) {
            // Collapses completely without layout jumps on load failure
            Box(modifier = Modifier.height(0.dp))
        } else {
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .then(
                        if (!isAdLoaded) Modifier.padding(0.dp)
                        else Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(vertical = 4.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { viewCtx ->
                        val viewActivity = viewCtx.findActivity() ?: effectiveContext
                        try {
                            AdView(viewActivity).apply {
                                setAdSize(AdSize.BANNER)
                                this.adUnitId = adUnitId
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                )

                                // Hardware acceleration compatibility on Android 8.0+ (API 26+)
                                if (AdServicesCompat.isAtLeastAndroid8()) {
                                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                                }

                                adListener = object : AdListener() {
                                    override fun onAdLoaded() {
                                        Log.d(TAG, "Banner ad loaded successfully ($adUnitId) on API ${Build.VERSION.SDK_INT}")
                                        isAdLoaded = true
                                        adLoadError = null
                                    }

                                    override fun onAdFailedToLoad(error: LoadAdError) {
                                        Log.w(TAG, "Banner ad failed to load on API ${Build.VERSION.SDK_INT}: ${error.message} (Code: ${error.code})")
                                        isAdLoaded = false
                                        adLoadError = error.message
                                    }

                                    override fun onAdOpened() {
                                        Log.d(TAG, "Banner ad opened.")
                                    }

                                    override fun onAdClicked() {
                                        Log.d(TAG, "Banner ad clicked.")
                                    }

                                    override fun onAdClosed() {
                                        Log.d(TAG, "Banner ad closed.")
                                    }

                                    override fun onAdImpression() {
                                        Log.d(TAG, "Banner ad impression recorded.")
                                    }
                                }

                                try {
                                    loadAd(AdMobManager.buildAdRequest())
                                } catch (e: Throwable) {
                                    Log.e(TAG, "Exception while loading banner ad on API ${Build.VERSION.SDK_INT}", e)
                                    adLoadError = e.message
                                }

                                adViewInstance = this
                            }
                        } catch (t: Throwable) {
                            Log.e(TAG, "Failed to instantiate AdView on API ${Build.VERSION.SDK_INT}", t)
                            adLoadError = t.message
                            View(viewCtx)
                        }
                    },
                    modifier = Modifier
                        .wrapContentSize()
                        .testTag("admob_banner_view")
                )
            }
        }
    }
}

/**
 * Helper to recursively unwrap Context to find the host Activity.
 * Guaranteed safe across all API levels starting from Android 8.0 (API 26).
 */
private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
