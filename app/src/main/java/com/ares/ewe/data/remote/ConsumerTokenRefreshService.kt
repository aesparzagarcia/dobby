package com.ares.ewe.data.remote

import com.ares.ewe.BuildConfig
import com.ares.ewe.data.local.datastore.SessionManager
import com.ares.ewe.data.remote.model.AppRefreshRequest
import com.ares.ewe.data.remote.model.AppRefreshResponse
import com.ares.ewe.di.DobbyNoAuthClient
import com.google.gson.Gson
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

sealed interface ConsumerCoordinatorResult {
    data object NoRefreshStored : ConsumerCoordinatorResult
    data object SessionInvalid : ConsumerCoordinatorResult
    data object TransientFailure : ConsumerCoordinatorResult
    data class UseAccess(val token: String) : ConsumerCoordinatorResult
    data class NewTokens(val token: String) : ConsumerCoordinatorResult
}

sealed interface ConsumerLaunchRefreshOutcome {
    data object Skipped : ConsumerLaunchRefreshOutcome
    data object Refreshed : ConsumerLaunchRefreshOutcome
    data object Unchanged : ConsumerLaunchRefreshOutcome
    data object SessionDead : ConsumerLaunchRefreshOutcome
}

@Singleton
class ConsumerTokenRefreshService @Inject constructor(
    @DobbyNoAuthClient private val noAuthClient: OkHttpClient,
) {
    private val gson = Gson()
    private val mutex = Mutex()

    suspend fun coordinateAfter401(
        requestAccessToken: String,
        sessionManager: SessionManager,
    ): ConsumerCoordinatorResult = mutex.withLock {
        val currentAccess = sessionManager.authToken.first().orEmpty()
        if (currentAccess.isNotBlank() && currentAccess != requestAccessToken.trim()) {
            return@withLock ConsumerCoordinatorResult.UseAccess(currentAccess)
        }
        val refresh = sessionManager.refreshToken.first()
        if (refresh.isNullOrBlank()) {
            return@withLock ConsumerCoordinatorResult.NoRefreshStored
        }
        when (val r = withContext(Dispatchers.IO) { executeRefresh(refresh) }) {
            is HttpRefreshResult.Success -> {
                sessionManager.saveSession(r.accessToken, r.refreshToken)
                ConsumerCoordinatorResult.NewTokens(r.accessToken)
            }
            HttpRefreshResult.SessionInvalid -> ConsumerCoordinatorResult.SessionInvalid
            HttpRefreshResult.TransientFailure -> ConsumerCoordinatorResult.TransientFailure
        }
    }

    suspend fun refreshStoredSession(sessionManager: SessionManager): ConsumerLaunchRefreshOutcome =
        mutex.withLock {
            val refresh = sessionManager.refreshToken.first()
            if (refresh.isNullOrBlank()) {
                return@withLock ConsumerLaunchRefreshOutcome.Skipped
            }
            when (val r = withContext(Dispatchers.IO) { executeRefresh(refresh) }) {
                is HttpRefreshResult.Success -> {
                    sessionManager.saveSession(r.accessToken, r.refreshToken)
                    ConsumerLaunchRefreshOutcome.Refreshed
                }
                HttpRefreshResult.SessionInvalid -> {
                    sessionManager.clearSession()
                    ConsumerLaunchRefreshOutcome.SessionDead
                }
                HttpRefreshResult.TransientFailure -> ConsumerLaunchRefreshOutcome.Unchanged
            }
        }

    private fun executeRefresh(refresh: String): HttpRefreshResult {
        val url = BuildConfig.BASE_URL.trimEnd('/') + "/auth/refresh"
        val bodyJson = gson.toJson(AppRefreshRequest(refresh))
        val httpReq = Request.Builder()
            .url(url)
            .post(bodyJson.toRequestBody(jsonMediaType))
            .build()
        return try {
            noAuthClient.newCall(httpReq).execute().use { resp ->
                when (resp.code) {
                    401, 403 -> HttpRefreshResult.SessionInvalid
                    in 500..599 -> HttpRefreshResult.TransientFailure
                    else -> {
                        if (!resp.isSuccessful) {
                            if (resp.code == 400) HttpRefreshResult.SessionInvalid
                            else HttpRefreshResult.TransientFailure
                        } else {
                            val body = resp.body?.string().orEmpty()
                            val parsed = try {
                                gson.fromJson(body, AppRefreshResponse::class.java)
                            } catch (_: Exception) {
                                null
                            }
                            val access = parsed?.token
                            val nextRefresh = parsed?.refreshToken
                            if (access.isNullOrBlank() || nextRefresh.isNullOrBlank()) {
                                HttpRefreshResult.TransientFailure
                            } else {
                                HttpRefreshResult.Success(access, nextRefresh)
                            }
                        }
                    }
                }
            }
        } catch (_: IOException) {
            HttpRefreshResult.TransientFailure
        }
    }

    private sealed interface HttpRefreshResult {
        data class Success(val accessToken: String, val refreshToken: String) : HttpRefreshResult
        data object SessionInvalid : HttpRefreshResult
        data object TransientFailure : HttpRefreshResult
    }
}
