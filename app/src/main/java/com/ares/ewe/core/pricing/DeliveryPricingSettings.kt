package com.ares.ewe.core.pricing

/**
 * Tarifas de envío (desde API `app/delivery-pricing-config` o [DEFAULT]).
 */
data class DeliveryPricingSettings(
    val baseFee: Double,
    val pricePerKm: Double,
    val weatherFee: Double,
    val defaultDemandMultiplier: Double,
    val defaultIsRaining: Boolean,
    val zoneAMaxKm: Double,
    val zoneBMaxKm: Double,
    val zoneCMaxKm: Double,
    val zoneBFee: Double,
    val zoneCFee: Double,
    val zoneDFee: Double,
) {
    companion object {
        val DEFAULT = DeliveryPricingSettings(
            baseFee = 25.0,
            pricePerKm = 7.0,
            weatherFee = 15.0,
            defaultDemandMultiplier = 1.0,
            defaultIsRaining = false,
            zoneAMaxKm = 3.0,
            zoneBMaxKm = 7.0,
            zoneCMaxKm = 12.0,
            zoneBFee = 10.0,
            zoneCFee = 25.0,
            zoneDFee = 50.0,
        )
    }
}
