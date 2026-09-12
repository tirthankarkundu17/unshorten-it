package `in`.bitmaskers.unshortenit.data.api

import android.util.Log
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/**
 * An OkHttp interceptor that provides transparent failover between a primary
 * and secondary backend URL.
 *
 * Behavior:
 * 1. Executes against [primaryUrlStr].
 * 2. If the primary URL throws an [IOException] (network down, connection refused, timeout)
 *    OR returns an HTTP 5xx server error, it automatically falls back to [fallbackUrlStr].
 * 3. Enforces a 60-second cooldown period after a primary failure so subsequent requests
 *    immediately target the secondary URL without incurring timeout delays.
 * 4. Automatically retries the primary URL after the cooldown expires to restore primary routing.
 */
class FailoverInterceptor(
    primaryUrlStr: String,
    fallbackUrlStr: String?
) : Interceptor {

    private val primaryUrl: HttpUrl? = primaryUrlStr.trim().takeIf { it.isNotEmpty() }?.toHttpUrlOrNull()
    private val fallbackUrl: HttpUrl? = fallbackUrlStr?.trim()?.takeIf { it.isNotEmpty() }?.toHttpUrlOrNull()

    @Volatile
    private var primaryFailedUntil: Long = 0L

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // If no valid fallback is configured or both point to the same host, proceed normally
        if (fallbackUrl == null || primaryUrl == null || fallbackUrl.host == primaryUrl.host) {
            return chain.proceed(originalRequest)
        }

        val now = System.currentTimeMillis()
        val isPrimaryCoolingDown = now < primaryFailedUntil

        if (isPrimaryCoolingDown) {
            // Primary failed recently; try fallback first to avoid hanging on a dead primary
            try {
                val fallbackRequest = rewriteUrl(originalRequest, fallbackUrl)
                val response = chain.proceed(fallbackRequest)
                if (response.isSuccessful || response.code < 500) {
                    return response
                }
                response.close()
            } catch (e: IOException) {
                Log.w(TAG, "Fallback URL failed during primary cooldown: ${e.message}")
            }
            // Fallback also failed or returned 5xx; try primary in case it recovered
            return tryPrimary(chain, originalRequest)
        } else {
            // Normal flow: Try primary first, fallback on failure
            return tryPrimaryThenFallback(chain, originalRequest)
        }
    }

    private fun tryPrimary(chain: Interceptor.Chain, request: Request): Response {
        val primaryRequest = rewriteUrl(request, primaryUrl!!)
        val response = chain.proceed(primaryRequest)
        if (response.isSuccessful || response.code < 500) {
            primaryFailedUntil = 0L
        }
        return response
    }

    private fun tryPrimaryThenFallback(chain: Interceptor.Chain, request: Request): Response {
        val primaryRequest = rewriteUrl(request, primaryUrl!!)
        var primaryResponse: Response? = null

        try {
            primaryResponse = chain.proceed(primaryRequest)
            if (primaryResponse.isSuccessful || primaryResponse.code < 500) {
                primaryFailedUntil = 0L
                return primaryResponse
            }
            Log.w(TAG, "Primary URL returned HTTP ${primaryResponse.code}, failing over to secondary")
        } catch (e: IOException) {
            Log.w(TAG, "Primary URL request failed: ${e.message}, failing over to secondary")
        }

        // Primary failed (HTTP 5xx or IOException) -> enter cooldown
        primaryFailedUntil = System.currentTimeMillis() + COOLDOWN_MS
        primaryResponse?.close()

        // Proceed to fallback URL
        val fallbackRequest = rewriteUrl(request, fallbackUrl!!)
        return chain.proceed(fallbackRequest)
    }

    private fun rewriteUrl(request: Request, targetBaseUrl: HttpUrl): Request {
        val originalUrl = request.url
        val newUrl = originalUrl.newBuilder()
            .scheme(targetBaseUrl.scheme)
            .host(targetBaseUrl.host)
            .port(targetBaseUrl.port)
            .build()

        return request.newBuilder()
            .url(newUrl)
            .build()
    }

    companion object {
        private const val TAG = "FailoverInterceptor"
        private const val COOLDOWN_MS = 60_000L // 60 seconds cooldown
    }
}
