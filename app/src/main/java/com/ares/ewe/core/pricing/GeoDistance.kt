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
    const val MAX_REASONABLE_DELIVERY_ROAD_KM = 100.0

    fun isUsableWgs84Point(lat: Double, lng: Double): Boolean {
        if (!lat.isFinite() || !lng.isFinite()) return false
        if (kotlin.math.abs(lat) > 90 || kotlin.math.abs(lng) > 180) return false
        if (kotlin.math.abs(lat) < 1e-5 && kotlin.math.abs(lng) < 1e-5) return false
        return true
    }

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
        if (!isUsableWgs84Point(userLat, userLng)) return null
        val pickups = resolvePickups(items, shopCoordsByShopId)
        if (pickups.isEmpty()) return null
        val maxKm = pickups.maxOf { roadDistanceKm(userLat, userLng, it.first, it.second) }
        return maxKm.takeIf { it <= MAX_REASONABLE_DELIVERY_ROAD_KM }
    }

    fun resolvePickups(
        items: List<CartItem>,
        shopCoordsByShopId: Map<String, Pair<Double, Double>>,
    ): List<Pair<Double, Double>> =
        items.mapNotNull { item ->
            val fromPickup = item.pickupLatitude?.let { la ->
                item.pickupLongitude?.let { lo ->
                    if (isUsableWgs84Point(la, lo)) la to lo else null
                }
            }
            if (fromPickup != null) return@mapNotNull fromPickup
            val sid = item.shopId ?: return@mapNotNull null
            shopCoordsByShopId[sid]?.takeIf { isUsableWgs84Point(it.first, it.second) }
        }.distinctBy { "${it.first},${it.second}" }
}
