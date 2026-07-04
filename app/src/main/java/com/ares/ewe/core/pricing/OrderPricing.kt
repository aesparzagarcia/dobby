package com.ares.ewe.core.pricing

private const val SERVICE_FEE_RATE = 0.08

data class OrderPricing(
    val productsSubtotal: Double,
    val delivery: DeliveryPricingBreakdown,
) {
    val serviceFee: Double =
        roundMoney(productsSubtotal * SERVICE_FEE_RATE)

    val grandTotal: Double =
        roundMoney(productsSubtotal + serviceFee + delivery.finalDeliveryFee)
}

fun roundMoney(value: Double): Double =
    kotlin.math.round(value * 100.0) / 100.0
