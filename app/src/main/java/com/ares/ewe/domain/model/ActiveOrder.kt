package com.ares.ewe.domain.model

/**
 * One UI step per backend [OrderStatus] stage (see [orderStatusToTrackingStep]).
 * Backend: PENDING, CONFIRMED, PREPARING, READY_FOR_PICKUP, ASSIGNED, ON_DELIVERY, DELIVERED, CANCELLED.
 */
data class ActiveOrder(
    val id: String,
    val status: String,
    val total: Double = 0.0,
    val deliveryAddress: String? = null,
    val createdAt: String? = null
) {
    /** Step index 0–6 for the 7-stage progress (6 = delivered). */
    val stepIndex: Int
        get() = orderStatusToTrackingStep(status)
}

/** Maps each API status to its own step in the tracking UI (0 = first, 6 = delivered). */
fun orderStatusToTrackingStep(status: String): Int = when (status.uppercase()) {
    "PENDING" -> 0
    "CONFIRMED" -> 1
    "PREPARING" -> 2
    "READY_FOR_PICKUP" -> 3
    "ASSIGNED" -> 4
    "ON_DELIVERY" -> 5
    "DELIVERED" -> 6
    "CANCELLED" -> 0
    else -> 0
}
