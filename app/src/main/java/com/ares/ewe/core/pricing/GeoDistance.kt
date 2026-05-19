package com.ares.ewe.core.pricing

import com.ares.ewe.domain.model.CartItem
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Distancia tienda → domicilio (Haversine + factor vial).
 * Compartido por ETA ([com.ares.ewe.core.delivery.DeliveryEtaEstimator]) y envío.
 */
object GeoDistance {

    const val EARTH_RADIUS_M = 6_371_000.0
    const val ROAD_FACTOR = 1.32

    fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r1 = Math.toRadians(lat1)
        val r2 = Math.toRadians(lat2)
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val h = sin(dLat / 2).pow(2) + cos(r1) * cos(r2) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(h), sqrt(1 - h))
        return EARTH_RADIUS_M * c
    }

    fun roadDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val meters = haversineMeters(lat1, lon1, lat2, lon2)
        return (meters / 1000.0) * ROAD_FACTOR
    }

    /**
     * Máxima distancia vial (km) entre el domicilio y cada punto de recogida del carrito.
     * `null` si faltan coords de usuario o no hay pickups válidos.
     */
    fun maxRoadKmFromPickups(
        userLat: Double?,
        userLng: Double?,
        items: List<CartItem>,
        shopCoordsByShopId: Map<String, Pair<Double, Double>>,
    ): Double? {
        if (userLat == null || userLng == null) return null
        if (!userLat.isFinite() || !userLng.isFinite()) return null
        val pickups = resolvePickups(items, shopCoordsByShopId)
        if (pickups.isEmpty()) return null
        return pickups.maxOf { roadDistanceKm(userLat, userLng, it.first, it.second) }
    }

    fun resolvePickups(
        items: List<CartItem>,
        shopCoordsByShopId: Map<String, Pair<Double, Double>>,
    ): List<Pair<Double, Double>> =
        items.mapNotNull { item ->
            val fromPickup = item.pickupLatitude?.let { la ->
                item.pickupLongitude?.let { lo ->
                    if (la.isFinite() && lo.isFinite()) la to lo else null
                }
            }
            if (fromPickup != null) return@mapNotNull fromPickup
            val sid = item.shopId ?: return@mapNotNull null
            shopCoordsByShopId[sid]
        }.distinctBy { "${it.first},${it.second}" }
}
