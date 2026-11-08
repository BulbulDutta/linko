package com.example

import com.example.ads.AdMobManager
import com.example.ads.AdServicesCompat
import com.example.util.AdManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdSystemRobolectricTest {

    @Test
    fun `test AdMob IDs match requirements`() {
        assertEquals("ca-app-pub-9185028793925317~4904991596", AdMobManager.SAMPLE_APP_ID)
        assertEquals("ca-app-pub-9185028793925317/8746987500", AdMobManager.BANNER_TEST_AD_UNIT_ID)
        assertEquals("ca-app-pub-9185028793925317/5312632818", AdMobManager.INTERSTITIAL_TEST_AD_UNIT_ID)

        assertEquals("ca-app-pub-9185028793925317~4904991596", AdManager.ADMOB_APP_ID)
        assertEquals("ca-app-pub-9185028793925317/5312632818", AdManager.INTERSTITIAL_AD_UNIT_ID)
    }

    @Test
    fun `test AdServicesCompat minimum supported API is Android 8 (API 26)`() {
        assertEquals(26, AdServicesCompat.MIN_SUPPORTED_API_LEVEL)
    }

    @Test
    fun `test AdServicesCompat diagnostic summary contains API information`() {
        val summary = AdServicesCompat.getDiagnosticSummary()

        assertNotNull(summary)
        assertTrue(summary.contains("OS API Level:"))
    }

    @Test
    fun `test AdServicesCompat measurement api is safely disabled`() {
        assertEquals(false, AdServicesCompat.isMeasurementApiSupported())
    }

    @Test
    fun `test PrivacySandbox MeasurementManagerFutures and TopicsManagerFutures shims return null`() {
        val dummyContext = android.content.ContextWrapper(null)
        val measurementFutures = androidx.privacysandbox.ads.adservices.java.measurement.MeasurementManagerFutures.from(dummyContext)
        val topicsFutures = androidx.privacysandbox.ads.adservices.java.topics.TopicsManagerFutures.from(dummyContext)
        org.junit.Assert.assertNull(measurementFutures)
        org.junit.Assert.assertNull(topicsFutures)
    }
}
