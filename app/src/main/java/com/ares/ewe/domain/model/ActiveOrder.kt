package com.ares.ewe.domain.model

/**
 * Seven-step home tracking UI (see [orderStatusToTrackingStep]).
 * Backend: PENDING, CONFIRMED, PREPARING, READY_FOR_PICKUP, ASSIGNED, ON_DELIVERY, DELIVERED, CANCELLED.
 */
data class ActiveOrderProductLine(
    val name: String,
    val quantity: Int,
)

data class ActiveOrder(
    val id: String,
    val status: String,
    val total: Double = 0.0,
    val deliveryAddress: String? = null,
    val createdAt: String? = null,
    val shopType: String? = null,
    val productLines: List<ActiveOrderProductLine> = emptyList(),
) {
    /** Step index 0–6 for the 7-stage home progress (6 = delivered). */
    val stepIndex: Int
        get() = orderStatusToTrackingStep(status)

    val isCarWash: Boolean
        get() = shopType?.trim()?.equals("CAR_WASH", ignoreCase = true) == true

    /** Etiqueta para listas con varios pedidos activos. */
    val productSummary: String
        get() = productLines.joinToString(", ") { line ->
            if (line.quantity > 1) "${line.name} ×${line.quantity}" else line.name
        }
}

/** Maps each API status to its home tracker step (0 = first, 6 = delivered). */
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
