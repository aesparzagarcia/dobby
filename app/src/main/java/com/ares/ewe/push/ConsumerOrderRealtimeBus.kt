package com.ares.ewe.push

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

data class OrderRefreshEvent(val orderId: String? = null)

@Singleton
class ConsumerOrderRealtimeBus @Inject constructor() {
    private val _events = MutableSharedFlow<OrderRefreshEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<OrderRefreshEvent> = _events.asSharedFlow()

    fun notifyOrderChanged(orderId: String? = null) {
        _events.tryEmit(OrderRefreshEvent(orderId))
    }
}
