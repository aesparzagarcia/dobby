package com.ares.ewe.domain.model

/**
 * Returns only the address and colony (first two comma-separated parts).
 * e.g. "C. Caoba 28A, Camino Real, 45306 Tala, Jal., Mexico" -> "C. Caoba 28A, Camino Real"
 */
fun String.toAddressWithColonyOnly(): String {
    val parts = this.split(",").map { it.trim() }.filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> parts.take(2).joinToString(", ")
        parts.size == 1 -> parts.first()
        else -> this
    }
}
