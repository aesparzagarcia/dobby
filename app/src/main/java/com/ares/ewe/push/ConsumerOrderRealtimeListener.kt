package com.ares.ewe.push

import com.ares.ewe.data.local.datastore.SessionManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConsumerOrderRealtimeListener @Inject constructor(
    private val sessionManager: SessionManager,
    private val consumerFirebaseAuth: ConsumerFirebaseAuth,
    private val orderRealtimeBus: ConsumerOrderRealtimeBus,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var registration: ListenerRegistration? = null
    private var listeningUserId: String? = null

    fun start() {
        scope.launch {
            if (!sessionManager.isLoggedIn.first()) {
                stop()
                return@launch
            }
            consumerFirebaseAuth.signInWithBackendToken()
            val userId = sessionManager.userId.first()?.trim().orEmpty()
            if (userId.isEmpty()) return@launch
            if (userId == listeningUserId && registration != null) return@launch

            stop()
            listeningUserId = userId
            registration = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("order_signals")
                .addSnapshotListener { snapshot, _ ->
                    // Any snapshot (incl. reconnect) should wake UI — not only documentChanges.
                    if (snapshot == null) {
                        orderRealtimeBus.notifyOrderChanged()
                        return@addSnapshotListener
                    }
                    val changes = snapshot.documentChanges
                    if (changes.isEmpty()) {
                        orderRealtimeBus.notifyOrderChanged()
                    } else {
                        changes.forEach { change ->
                            orderRealtimeBus.notifyOrderChanged(change.document.id)
                        }
                    }
                }
        }
    }

    /** Force re-attach after background (parity with DobbyShop resume). */
    fun resume() {
        stop()
        start()
    }

    fun stop() {
        registration?.remove()
        registration = null
        listeningUserId = null
    }
}
