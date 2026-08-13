package com.ares.ewe.domain.model

import com.ares.ewe.core.pricing.GeoDistance
import java.text.Normalizer
import java.util.Locale

/**
 * Detects when a new address matches an existing one by proximity or normalized text.
 */
object AddressDuplicate {

    const val PROXIMITY_METERS = 50.0
    const val MESSAGE = "Esta dirección ya está guardada."

    fun normalizeForCompare(address: String): String {
        val nfd = Normalizer.normalize(address.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
        return nfd
            .replace("\\p{Mn}+".toRegex(), "")
            .replace("[^a-z0-9\\s,]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    fun isDuplicate(
        existing: List<UserAddress>,
        address: String,
        lat: Double,
        lng: Double,
    ): Boolean {
        val colonyKey = normalizeForCompare(address.toAddressWithColonyOnly())
        val streetKey = normalizeForCompare(address.toStreetAndNumberOnly())
        return existing.any { saved ->
            if (!saved.isActive) return@any false
            val distance = GeoDistance.haversineMeters(lat, lng, saved.lat, saved.lng)
            if (distance <= PROXIMITY_METERS) return@any true
            val savedColony = normalizeForCompare(saved.address.toAddressWithColonyOnly())
            if (colonyKey.isNotBlank() && colonyKey == savedColony) return@any true
            val savedStreet = normalizeForCompare(saved.address.toStreetAndNumberOnly())
            streetKey.isNotBlank() && streetKey == savedStreet &&
                GeoDistance.haversineMeters(lat, lng, saved.lat, saved.lng) <= 150.0
        }
    }
}
