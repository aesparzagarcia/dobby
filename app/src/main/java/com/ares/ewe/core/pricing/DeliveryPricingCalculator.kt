package com.ares.ewe.core.pricing

/**
 * Costo de envío híbrido: base + km + zona + clima, con multiplicador dinámico.
 */
object DeliveryPricingCalculator {

    fun zoneFee(distanceKm: Double, config: DeliveryPricingSettings): Double = when {
        distanceKm <= config.zoneAMaxKm -> 0.0
        distanceKm <= config.zoneBMaxKm -> config.zoneBFee
        distanceKm <= config.zoneCMaxKm -> config.zoneCFee
        else -> config.zoneDFee
    }

    fun calculate(
        input: DeliveryPricingInput,
        config: DeliveryPricingSettings = DeliveryPricingSettings.DEFAULT,
    ): DeliveryPricingBreakdown {
        val distanceKm = input.distanceKm.coerceAtLeast(0.0)
        val baseFee = config.baseFee
        val distanceFee = roundMoney(distanceKm * config.pricePerKm)
        val zone = roundMoney(zoneFee(distanceKm, config))
        val weather = if (input.isRaining) config.weatherFee else 0.0
        val subtotal = roundMoney(baseFee + distanceFee + zone + weather)
        val multiplier = input.demandMultiplier.coerceAtLeast(1.0)
        val finalFee = roundMoney(subtotal * multiplier)
        return DeliveryPricingBreakdown(
            distanceKm = roundMoney(distanceKm),
            baseFee = baseFee,
            distanceFee = distanceFee,
            zoneFee = zone,
            weatherFee = weather,
            deliverySubtotal = subtotal,
            dynamicMultiplier = multiplier,
            finalDeliveryFee = finalFee,
        )
    }
}
