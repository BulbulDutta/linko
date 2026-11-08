package androidx.privacysandbox.ads.adservices.java.measurement

import android.content.Context
import android.net.Uri
import android.view.InputEvent
import androidx.privacysandbox.ads.adservices.measurement.DeletionRequest
import androidx.privacysandbox.ads.adservices.measurement.WebSourceRegistrationRequest
import androidx.privacysandbox.ads.adservices.measurement.WebTriggerRegistrationRequest
import com.google.common.util.concurrent.ListenableFuture

/**
 * Compatibility shim for [MeasurementManagerFutures] that prevents the Google Mobile Ads SDK
 * from binding to `android.adservices.MEASUREMENT_SERVICE` on devices/emulators where the
 * AdServices measurement daemon is unavailable (avoiding `E/adservices: onNullBinding`).
 */
abstract class MeasurementManagerFutures internal constructor() {
    abstract fun deleteRegistrationsAsync(deletionRequest: DeletionRequest): ListenableFuture<Unit>

    abstract fun registerSourceAsync(
        attributionSource: Uri,
        inputEvent: InputEvent?,
    ): ListenableFuture<Unit>

    abstract fun registerTriggerAsync(trigger: Uri): ListenableFuture<Unit>

    abstract fun registerWebSourceAsync(
        request: WebSourceRegistrationRequest,
    ): ListenableFuture<Unit>

    abstract fun registerWebTriggerAsync(
        request: WebTriggerRegistrationRequest,
    ): ListenableFuture<Unit>

    abstract fun getMeasurementApiStatusAsync(): ListenableFuture<Int>

    companion object {
        @JvmStatic
        fun from(context: Context): MeasurementManagerFutures? = null
    }
}
