package com.ares.ewe.core.delivery

import com.ares.ewe.domain.model.CartItem
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * ETA aproximada tienda → domicilio (sin Directions API): Haversine + factor vial + prep.
 * Paridad con iOS `DeliveryEtaEstimator`.
 */
object DeliveryEtaEstimator {

    private const val EARTH_RADIUS_M = 6_371_000.0
    private const val ROAD_FACTOR = 1.32
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
        val pickups = items.mapNotNull { item ->
            val fromPickup = item.pickupLatitude?.let { la ->
                item.pickupLongitude?.let { lo ->
                    if (la.isFinite() && lo.isFinite()) la to lo else null
                }
            }
            if (fromPickup != null) return@mapNotNull fromPickup
            val sid = item.shopId ?: return@mapNotNull null
            shopCoordsByShopId[sid]
        }.distinctBy { "${it.first},${it.second}" }
        if (pickups.isEmpty()) return FALLBACK_LABEL
        val maxCenter = pickups.maxOf { centerMinutes(userLat, userLng, it.first, it.second) }
        val low = (maxCenter * 0.82).roundToInt().coerceIn(18, 150)
        val high = (maxCenter * 1.18).roundToInt().coerceIn(low + 5, 160)
        return "$low\u2013$high min"
    }

    private fun centerMinutes(userLat: Double, userLng: Double, shopLat: Double, shopLng: Double): Double {
        val m = haversineMeters(userLat, userLng, shopLat, shopLng)
        val roadKm = (m / 1000.0) * ROAD_FACTOR
        val travelMin = (roadKm / AVG_SPEED_KMH) * 60.0
        return (PREP_MINUTES + travelMin).coerceIn(22.0, 130.0)
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r1 = Math.toRadians(lat1)
        val r2 = Math.toRadians(lat2)
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val h = sin(dLat / 2).pow(2) + cos(r1) * cos(r2) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(h), sqrt(1 - h))
        return EARTH_RADIUS_M * c
    }
}
