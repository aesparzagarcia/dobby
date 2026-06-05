package com.ares.ewe.push

/** Pedidos terminados: no abrir seguimiento desde push, volver a home. */
object OrderPushNavigation {
    private val TERMINAL_STATUSES = setOf("DELIVERED", "CANCELLED")

    fun canOpenTracking(status: String): Boolean =
        status.uppercase() !in TERMINAL_STATUSES
}
