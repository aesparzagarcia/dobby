package com.ares.ewe.core.location

/**
 * Región fija para autocomplete de direcciones.
 * Por ahora solo Tala; cuando se amplíe el servicio, cambia estos valores.
 */
object AddressSearchRegion {
    /** Plaza principal de Tala, Jalisco. */
    const val CENTER_LAT = 20.6507582
    const val CENTER_LNG = -103.7029606

    /** Radio estricto (~cubre el municipio de Tala). */
    const val RADIUS_METERS = 12_000

    /** Sufijo estilo Google Maps para acotar la búsqueda local. */
    const val QUERY_LOCALITY = "Tala, Jalisco"

    fun localizedQuery(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return trimmed
        val lower = trimmed.lowercase()
        if (lower.contains("tala")) return trimmed
        return "$trimmed, $QUERY_LOCALITY"
    }
}
