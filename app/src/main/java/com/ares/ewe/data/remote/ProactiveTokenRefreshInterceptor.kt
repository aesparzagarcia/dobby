package com.ares.ewe.data.remote

import com.ares.ewe.core.auth.AccessTokenJwtParser
import com.ares.ewe.data.local.datastore.SessionManager
import com.ares.ewe.data.session.SessionEventBus
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Before each Dobby API call, refreshes when access is missing, unreadable, or within
 * [THRESHOLD_SECONDS] of expiring (same idea as the iOS client’s pre-request refresh).
 */
class ProactiveTokenRefreshInterceptor @Inject constructor(
    private val sessionManager: SessionManager,
    private val consumerTokenRefreshService: ConsumerTokenRefreshService,
    private val sessionEventBus: SessionEventBus,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        if (request.header(DobbyConsumerHttpAuthPolicy.HEADER_AUTH_RETRY) != null) {
            return chain.proceed(request)
        }
        if (!DobbyConsumerHttpAuthPolicy.isDobbyBackendRequest(request)) {
            return chain.proceed(request)
        }
        if (DobbyConsumerHttpAuthPolicy.shouldSkipRefresh(request)) {
            return chain.proceed(request)
        }

        val needsRefresh = runBlocking {
            if (!sessionManager.isLoggedIn.first()) return@runBlocking false
            val refresh = sessionManager.refreshToken.first()
            if (refresh.isNullOrBlank()) return@runBlocking false

            val bearer = request.header("Authorization").orEmpty()
                .removePrefix("Bearer ")
                .trim()
            val access = bearer.ifBlank { sessionManager.authToken.first().orEmpty() }

            if (access.isBlank()) return@runBlocking true

            val exp = AccessTokenJwtParser.expiryEpochSeconds(access) ?: return@runBlocking true
            val now = System.currentTimeMillis() / 1000
            (exp - now) <= THRESHOLD_SECONDS
        }

        if (!needsRefresh) return chain.proceed(request)

        when (
            runBlocking { consumerTokenRefreshService.refreshStoredSession(sessionManager) }
        ) {
            ConsumerLaunchRefreshOutcome.Skipped,
            ConsumerLaunchRefreshOutcome.Unchanged,
            -> Unit
            ConsumerLaunchRefreshOutcome.Refreshed -> {
                val newAccess = runBlocking { sessionManager.authToken.first().orEmpty() }
                if (newAccess.isNotBlank()) {
                    request = request.newBuilder()
                        .header("Authorization", "Bearer $newAccess")
                        .build()
                }
            }
            ConsumerLaunchRefreshOutcome.SessionDead -> {
                sessionEventBus.notifySessionExpired()
                request = request.newBuilder()
                    .removeHeader("Authorization")
                    .build()
            }
        }

        return chain.proceed(request)
    }

    private companion object {
        const val THRESHOLD_SECONDS = 10 * 60L
    }
}
