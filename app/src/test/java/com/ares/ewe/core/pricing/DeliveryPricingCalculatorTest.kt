package com.ares.ewe.core.pricing

import org.junit.Assert.assertEquals
import org.junit.Test

class DeliveryPricingCalculatorTest {

    @Test
    private val config = DeliveryPricingSettings.DEFAULT

    @Test
    fun zoneFee_zoneA_isZero() {
        assertEquals(0.0, DeliveryPricingCalculator.zoneFee(2.0, config), 0.001)
    }

    @Test
    fun zoneFee_zoneB_isTen() {
        assertEquals(10.0, DeliveryPricingCalculator.zoneFee(5.0, config), 0.001)
    }

    @Test
    fun zoneFee_zoneD_isFifty() {
        assertEquals(50.0, DeliveryPricingCalculator.zoneFee(15.0, config), 0.001)
    }

    @Test
    fun calculate_8km_matchesSpecExample() {
        val result = DeliveryPricingCalculator.calculate(
            DeliveryPricingInput(distanceKm = 8.0),
            config = config,
        )
        // 25 + (8*7) + 10 = 81
        assertEquals(81.0, result.finalDeliveryFee, 0.001)
        assertEquals(25.0, result.baseFee, 0.001)
        assertEquals(56.0, result.distanceFee, 0.001)
        assertEquals(10.0, result.zoneFee, 0.001)
        assertEquals(0.0, result.weatherFee, 0.001)
    }

    @Test
    fun calculate_withRain_addsWeatherFee() {
        val result = DeliveryPricingCalculator.calculate(
            DeliveryPricingInput(distanceKm = 2.0, isRaining = true),
            config = config,
        )
        // 25 + 14 + 0 + 15 = 54
        assertEquals(54.0, result.finalDeliveryFee, 0.001)
    }

    @Test
    fun calculate_demandMultiplier_appliesToSubtotal() {
        val result = DeliveryPricingCalculator.calculate(
            DeliveryPricingInput(distanceKm = 8.0, demandMultiplier = 1.2),
            config = config,
        )
        assertEquals(97.2, result.finalDeliveryFee, 0.001)
    }

    @Test
    fun orderPricing_grandTotal_sumsProductsAndDelivery() {
        val delivery = DeliveryPricingCalculator.calculate(
            DeliveryPricingInput(distanceKm = 8.0),
            config = config,
        )
        val order = OrderPricing(productsSubtotal = 220.0, delivery = delivery)
        assertEquals(301.0, order.grandTotal, 0.001)
    }
}
