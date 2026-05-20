package com.ares.ewe.push

import com.ares.ewe.data.local.datastore.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConsumerRealtimeCoordinator @Inject constructor(
    private val sessionManager: SessionManager,
    private val consumerFirebaseAuth: ConsumerFirebaseAuth,
    private val pushTokenRegistrar: PushTokenRegistrar,
    private val orderRealtimeListener: ConsumerOrderRealtimeListener,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun onSessionReady() {
        scope.launch {
            if (!sessionManager.isLoggedIn.first()) return@launch
            consumerFirebaseAuth.signInWithBackendToken()
            pushTokenRegistrar.registerCurrentToken()
            orderRealtimeListener.start()
        }
    }

    fun onLogout() {
        orderRealtimeListener.stop()
        consumerFirebaseAuth.signOut()
        scope.launch {
            pushTokenRegistrar.unregisterOnServer()
        }
    }
}
