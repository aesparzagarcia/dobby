package com.ares.ewe.data.session

import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionEventBus @Inject constructor() {
    private val _sessionExpired = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    private val expiredEventPending = AtomicBoolean(false)

    /**
     * Emits at most once until [resetExpiredGate] runs (e.g. after a new login). Parallel 401s
     * otherwise fire this many times and re-trigger navigation.
     */
    fun notifySessionExpired() {
        if (expiredEventPending.compareAndSet(false, true)) {
            _sessionExpired.tryEmit(Unit)
        }
    }

    fun resetExpiredGate() {
        expiredEventPending.set(false)
    }
}
