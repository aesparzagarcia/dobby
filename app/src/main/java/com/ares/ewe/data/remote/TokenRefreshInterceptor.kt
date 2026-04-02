package com.ares.ewe.data.remote

import com.ares.ewe.BuildConfig
import com.ares.ewe.data.local.datastore.SessionManager
import com.ares.ewe.data.session.SessionEventBus
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

private const val HEADER_AUTH_RETRY = "X-Dobby-Auth-Retry"

/**
 * Refreshes consumer (Dobby) access token on 401. Only runs for requests to [BuildConfig.BASE_URL] host
 * so Google Places / other hosts are untouched. Transient refresh failures keep the stored session;
 * only invalid refresh or missing tokens trigger logout.
 */
class TokenRefreshInterceptor @Inject constructor(
    private val sessionManager: SessionManager,
    private val consumerTokenRefreshService: ConsumerTokenRefreshService,
    private val sessionEventBus: SessionEventBus,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (response.code != 401) return response

        if (request.header(HEADER_AUTH_RETRY) != null) {
            return response
        }

        if (!isDobbyBackendRequest(request)) {
            return response
        }

        if (shouldSkipRefresh(request)) {
            return response
        }

        response.close()

        val requestAccess = request.header("Authorization").orEmpty()
            .removePrefix("Bearer ")
            .trim()

        val result = runBlocking {
            consumerTokenRefreshService.coordinateAfter401(requestAccess, sessionManager)
        }

        when (result) {
            is ConsumerCoordinatorResult.NoRefreshStored -> {
                runBlocking { sessionManager.clearSession() }
                sessionEventBus.notifySessionExpired()
                return chain.proceed(
                    request.newBuilder()
                        .header(HEADER_AUTH_RETRY, "1")
                        .removeHeader("Authorization")
                        .build()
                )
            }
            is ConsumerCoordinatorResult.SessionInvalid -> {
                runBlocking { sessionManager.clearSession() }
                sessionEventBus.notifySessionExpired()
                return chain.proceed(
                    request.newBuilder()
                        .header(HEADER_AUTH_RETRY, "1")
                        .removeHeader("Authorization")
                        .build()
                )
            }
            is ConsumerCoordinatorResult.TransientFailure -> {
                throw IOException("No se pudo renovar la sesión. Comprueba tu conexión e inténtalo de nuevo.")
            }
            is ConsumerCoordinatorResult.UseAccess,
            is ConsumerCoordinatorResult.NewTokens -> {
                val access = when (result) {
                    is ConsumerCoordinatorResult.UseAccess -> result.token
                    is ConsumerCoordinatorResult.NewTokens -> result.token
                    else -> error("unreachable")
                }
                val retry = request.newBuilder()
                    .header("Authorization", "Bearer $access")
                    .header(HEADER_AUTH_RETRY, "1")
                    .build()
                val retryResp = chain.proceed(retry)
                if (retryResp.code == 401) {
                    retryResp.close()
                    runBlocking { sessionManager.clearSession() }
                    sessionEventBus.notifySessionExpired()
                    return chain.proceed(
                        request.newBuilder()
                            .header(HEADER_AUTH_RETRY, "1")
                            .removeHeader("Authorization")
                            .build()
                    )
                }
                return retryResp
            }
        }
    }

    private fun isDobbyBackendRequest(request: Request): Boolean {
        val base = BuildConfig.BASE_URL.toHttpUrlOrNull() ?: return false
        val url = request.url
        if (!url.host.equals(base.host, ignoreCase = true)) return false
        return url.port == base.port
    }

    private fun shouldSkipRefresh(request: Request): Boolean {
        val u = request.url.toString()
        return u.contains("auth/request-otp") ||
            u.contains("auth/verify-otp") ||
            u.contains("auth/complete-registration") ||
            u.contains("/auth/refresh")
    }
}
