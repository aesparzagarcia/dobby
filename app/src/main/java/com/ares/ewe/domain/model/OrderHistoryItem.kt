package com.ares.ewe.domain.model

data class OrderHistoryItem(
    val id: String,
    val status: String,
    val total: Double = 0.0,
    val createdAt: String? = null,
    val shopName: String? = null,
    val productLines: List<ActiveOrderProductLine> = emptyList(),
) {
    val productSummary: String
        get() = productLines.joinToString(", ") { line ->
            if (line.quantity > 1) "${line.name} ×${line.quantity}" else line.name
        }

    val displayTitle: String
        get() = shopName?.takeIf { it.isNotBlank() }
            ?: productSummary.ifBlank { "Pedido" }
}

fun orderStatusLabelEs(status: String): String = when (status.uppercase()) {
    "PENDING" -> "Pendiente"
    "CONFIRMED" -> "Confirmado"
    "OUT_FOR_PICKUP" -> "En camino"
    "PICKED_UP" -> "Recogido"
    "PREPARING" -> "En preparación"
    "READY_FOR_PICKUP" -> "Listo para recoger"
    "ASSIGNED" -> "Asignado"
    "ON_DELIVERY" -> "En camino"
    "DELIVERED" -> "Entregado"
    "CANCELLED" -> "Cancelado"
    else -> status
}
