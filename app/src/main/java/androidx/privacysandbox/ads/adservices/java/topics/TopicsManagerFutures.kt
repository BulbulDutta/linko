package androidx.privacysandbox.ads.adservices.java.topics

import android.content.Context
import androidx.privacysandbox.ads.adservices.topics.GetTopicsRequest
import androidx.privacysandbox.ads.adservices.topics.GetTopicsResponse
import com.google.common.util.concurrent.ListenableFuture

/**
 * Compatibility shim for [TopicsManagerFutures] when `ads-adservices-java` is excluded
 * to prevent AdServices service binding errors on emulators and non-supported environments.
 */
abstract class TopicsManagerFutures internal constructor() {
    abstract fun getTopicsAsync(request: GetTopicsRequest): ListenableFuture<GetTopicsResponse>

    companion object {
        @JvmStatic
        fun from(context: Context): TopicsManagerFutures? = null
    }
}
