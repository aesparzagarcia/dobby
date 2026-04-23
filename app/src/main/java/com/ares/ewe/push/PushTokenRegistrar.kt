package com.ares.ewe.push

import com.ares.ewe.data.local.datastore.SessionManager
import com.ares.ewe.data.remote.api.DobbyApi
import com.ares.ewe.data.remote.model.RegisterPushDeviceRequest
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushTokenRegistrar @Inject constructor(
    private val api: DobbyApi,
    private val sessionManager: SessionManager,
) {
    suspend fun registerCurrentToken() {
        if (!sessionManager.isLoggedIn.first()) return
        val token = try {
            FirebaseMessaging.getInstance().token.await()
        } catch (_: Exception) {
            return
        }
        registerToken(token)
    }

    suspend fun registerToken(fcmToken: String) {
        if (!sessionManager.isLoggedIn.first()) return
        try {
            api.registerPushDevice(
                RegisterPushDeviceRequest(fcmToken = fcmToken, platform = "android"),
            )
        } catch (_: Exception) {
            // Offline or 401 — next app open / token refresh will retry
        }
    }

    suspend fun unregisterOnServer() {
        try {
            api.unregisterPushDevice()
        } catch (_: Exception) {
        }
    }
}
