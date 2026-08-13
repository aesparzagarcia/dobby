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

/**
 * Returns only street and number (first comma-separated part).
 * e.g. "C. Caoba 28A, Camino Real, 45306 Tala, Jal., Mexico" -> "C. Caoba 28A"
 */
fun String.toStreetAndNumberOnly(): String {
    val parts = this.split(",").map { it.trim() }.filter { it.isNotBlank() }
    return parts.firstOrNull().orEmpty().ifBlank { this.trim() }
}

/**
 * Replaces street+colony (first two parts) of this full address with [editedStreetAndColony],
 * keeping city/state/country so the saved value stays complete.
 */
fun String.withEditedStreetAndColony(editedStreetAndColony: String): String {
    val edited = editedStreetAndColony.trim()
    if (edited.isBlank()) return trim()
    val rest = split(",").map { it.trim() }.filter { it.isNotBlank() }.drop(2)
    return if (rest.isEmpty()) edited else "$edited, ${rest.joinToString(", ")}"
}
