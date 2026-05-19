package com.ares.ewe.core.pricing

data class DeliveryPricingInput(
    val distanceKm: Double,
    val demandMultiplier: Double = DeliveryPricingSettings.DEFAULT.defaultDemandMultiplier,
    val isRaining: Boolean = DeliveryPricingSettings.DEFAULT.defaultIsRaining,
)
