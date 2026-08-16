package com.ares.ewe.domain.model

/**
 * Home tracking UI steps (see [orderStatusToTrackingStep]).
 * Food/shop: 7 stages. Carwash: 9 stages (En camino + Recogido before Lavando).
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
    val isCarWash: Boolean
        get() = shopType?.trim()?.equals("CAR_WASH", ignoreCase = true) == true

    /** Step index for the home progress UI (last = delivered). */
    val stepIndex: Int
        get() = orderStatusToTrackingStep(status, isCarWash)

    /** Etiqueta para listas con varios pedidos activos. */
    val productSummary: String
        get() = productLines.joinToString(", ") { line ->
            if (line.quantity > 1) "${line.name} ×${line.quantity}" else line.name
        }
}

/** Maps each API status to its home tracker step (0 = first, last = delivered). */
fun orderStatusToTrackingStep(status: String, isCarWash: Boolean = false): Int {
    if (isCarWash) {
        return when (status.uppercase()) {
            "PENDING" -> 0
            "CONFIRMED" -> 1
            "OUT_FOR_PICKUP" -> 2
            "PICKED_UP" -> 3
            "PREPARING" -> 4
            "READY_FOR_PICKUP" -> 5
            "ASSIGNED" -> 6
            "ON_DELIVERY" -> 7
            "DELIVERED" -> 8
            "CANCELLED" -> 0
            else -> 0
        }
    }
    return when (status.uppercase()) {
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
}
