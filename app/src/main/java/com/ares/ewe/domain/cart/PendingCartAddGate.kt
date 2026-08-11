package com.ares.ewe.domain.cart

import com.ares.ewe.data.local.datastore.SessionManager
import com.ares.ewe.domain.repository.UserAddressRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * When the user is logged in but has no saved delivery address, blocks add-to-cart,
 * opens the address flow, then resumes the pending add after an address is saved.
 */
@Singleton
class PendingCartAddGate @Inject constructor(
    private val sessionManager: SessionManager,
    private val userAddressRepository: UserAddressRepository,
) {
    private val mutex = Mutex()
    private var pendingAction: (suspend () -> Unit)? = null

    private val _requireAddress = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val requireAddress: SharedFlow<Unit> = _requireAddress.asSharedFlow()

    fun hasPending(): Boolean = pendingAction != null

    fun clearPending() {
        pendingAction = null
    }

    suspend fun hasValidDeliveryAddress(): Boolean {
        if (!sessionManager.isLoggedIn.first()) return true
        return userAddressRepository.getAddresses()
            .getOrNull()
            ?.any { address ->
                address.isActive &&
                    address.lat.isFinite() &&
                    address.lng.isFinite()
            } == true
    }

    /**
     * Runs [action] immediately if address is OK (or user is guest).
     * Otherwise stores [action] and emits [requireAddress].
     */
    suspend fun runOrRequestAddress(action: suspend () -> Unit) {
        if (!sessionManager.isLoggedIn.first()) {
            action()
            return
        }
        if (hasValidDeliveryAddress()) {
            action()
            return
        }
        mutex.withLock {
            pendingAction = action
        }
        _requireAddress.tryEmit(Unit)
    }

    /** Call after the user successfully created/selected a delivery address. */
    suspend fun resumePendingIfReady() {
        if (!hasValidDeliveryAddress()) {
            clearPending()
            return
        }
        val action = mutex.withLock {
            val pending = pendingAction
            pendingAction = null
            pending
        } ?: return
        action()
    }
}
