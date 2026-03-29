package com.ares.ewe.session

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.ares.ewe.core.auth.AccessTokenJwtParser
import com.ares.ewe.data.local.datastore.SessionManager
import com.ares.ewe.data.remote.ConsumerTokenRefreshService
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * While the app is in the foreground, periodically checks JWT [exp] on the access token and
 * calls [ConsumerTokenRefreshService.refreshStoredSession] before it expires so API calls rarely
 * hit 401.
 */
@Singleton
class ProactiveAccessTokenRefresh @Inject constructor(
    private val sessionManager: SessionManager,
    private val consumerTokenRefreshService: ConsumerTokenRefreshService,
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var foregroundJob: Job? = null

    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        foregroundJob?.cancel()
        foregroundJob = scope.launch {
            while (isActive) {
                runCatching { refreshIfExpiringSoon() }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        foregroundJob?.cancel()
        foregroundJob = null
    }

    private suspend fun refreshIfExpiringSoon() {
        if (!sessionManager.isLoggedIn.first()) return
        val access = sessionManager.authToken.first().orEmpty()
        if (access.isBlank()) return
        val exp = AccessTokenJwtParser.expiryEpochSeconds(access) ?: return
        val now = System.currentTimeMillis() / 1000
        val secondsLeft = exp - now
        if (secondsLeft > REFRESH_WHEN_SECONDS_LEFT) return
        if (sessionManager.refreshToken.first().isNullOrBlank()) return
        consumerTokenRefreshService.refreshStoredSession(sessionManager)
    }

    private companion object {
        const val REFRESH_WHEN_SECONDS_LEFT = 3 * 60L
        const val POLL_INTERVAL_MS = 3 * 60 * 1000L
    }
}
