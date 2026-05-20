package com.ares.ewe.push

import com.ares.ewe.data.remote.api.DobbyApi
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConsumerFirebaseAuth @Inject constructor(
    private val api: DobbyApi,
) {
    suspend fun signInWithBackendToken() {
        val customToken = try {
            api.getFirebaseCustomToken().token
        } catch (_: Exception) {
            return
        }
        if (customToken.isBlank()) return
        try {
            FirebaseAuth.getInstance().signInWithCustomToken(customToken).await()
        } catch (_: Exception) {
        }
    }

    fun signOut() {
        FirebaseAuth.getInstance().signOut()
    }
}
