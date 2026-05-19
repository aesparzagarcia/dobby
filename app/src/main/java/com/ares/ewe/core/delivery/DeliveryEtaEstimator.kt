package com.ares.ewe.core.delivery

import com.ares.ewe.core.pricing.GeoDistance
import com.ares.ewe.domain.model.CartItem
import kotlin.math.roundToInt

/**
 * ETA aproximada tienda → domicilio (sin Directions API): Haversine + factor vial + prep.
 * Paridad con iOS `DeliveryEtaEstimator`.
 */
object DeliveryEtaEstimator {

    private const val AVG_SPEED_KMH = 24.0
    private const val PREP_MINUTES = 14.0
    private const val FALLBACK_LABEL = "30–45 min"

    fun estimateLabel(
        userLat: Double?,
        userLng: Double?,
        items: List<CartItem>,
        shopCoordsByShopId: Map<String, Pair<Double, Double>>,
    ): String {
        if (userLat == null || userLng == null) return FALLBACK_LABEL
        if (!userLat.isFinite() || !userLng.isFinite()) return FALLBACK_LABEL
        val pickups = GeoDistance.resolvePickups(items, shopCoordsByShopId)
        if (pickups.isEmpty()) return FALLBACK_LABEL
        val maxCenter = pickups.maxOf { centerMinutes(userLat, userLng, it.first, it.second) }
        val low = (maxCenter * 0.82).roundToInt().coerceIn(18, 150)
        val high = (maxCenter * 1.18).roundToInt().coerceIn(low + 5, 160)
        return "$low\u2013$high min"
    }

    private fun centerMinutes(userLat: Double, userLng: Double, shopLat: Double, shopLng: Double): Double {
        val roadKm = GeoDistance.roadDistanceKm(userLat, userLng, shopLat, shopLng)
        val travelMin = (roadKm / AVG_SPEED_KMH) * 60.0
        return (PREP_MINUTES + travelMin).coerceIn(22.0, 130.0)
    }
}
