package com.ares.ewe.data.remote

import com.ares.ewe.data.local.datastore.SessionManager
import com.ares.ewe.data.session.SessionEventBus
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Refreshes consumer (Dobby) access token on 401. Only runs for requests to the Dobby API host
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

        if (request.header(DobbyConsumerHttpAuthPolicy.HEADER_AUTH_RETRY) != null) {
            return response
        }

        if (!DobbyConsumerHttpAuthPolicy.isDobbyBackendRequest(request)) {
            return response
        }

        if (DobbyConsumerHttpAuthPolicy.shouldSkipRefresh(request)) {
            return response
        }

        if (!runBlocking { sessionManager.isLoggedIn.first() }) {
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
                        .header(DobbyConsumerHttpAuthPolicy.HEADER_AUTH_RETRY, "1")
                        .removeHeader("Authorization")
                        .build()
                )
            }
            is ConsumerCoordinatorResult.SessionInvalid -> {
                runBlocking { sessionManager.clearSession() }
                sessionEventBus.notifySessionExpired()
                return chain.proceed(
                    request.newBuilder()
                        .header(DobbyConsumerHttpAuthPolicy.HEADER_AUTH_RETRY, "1")
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
                    .header(DobbyConsumerHttpAuthPolicy.HEADER_AUTH_RETRY, "1")
                    .build()
                val retryResp = chain.proceed(retry)
                if (retryResp.code == 401) {
                    retryResp.close()
                    runBlocking { sessionManager.clearSession() }
                    sessionEventBus.notifySessionExpired()
                    return chain.proceed(
                        request.newBuilder()
                            .header(DobbyConsumerHttpAuthPolicy.HEADER_AUTH_RETRY, "1")
                            .removeHeader("Authorization")
                            .build()
                    )
                }
                return retryResp
            }
        }
    }
}
