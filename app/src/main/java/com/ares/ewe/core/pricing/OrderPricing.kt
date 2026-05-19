package com.ares.ewe.core.pricing

data class OrderPricing(
    val productsSubtotal: Double,
    val delivery: DeliveryPricingBreakdown,
) {
    val grandTotal: Double =
        roundMoney(productsSubtotal + delivery.finalDeliveryFee)
}

fun roundMoney(value: Double): Double =
    kotlin.math.round(value * 100.0) / 100.0
