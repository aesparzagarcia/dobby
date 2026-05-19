package com.ares.ewe.core.pricing

data class DeliveryPricingBreakdown(
    val distanceKm: Double,
    val baseFee: Double,
    val distanceFee: Double,
    val zoneFee: Double,
    val weatherFee: Double,
    val deliverySubtotal: Double,
    val dynamicMultiplier: Double,
    val finalDeliveryFee: Double,
)
