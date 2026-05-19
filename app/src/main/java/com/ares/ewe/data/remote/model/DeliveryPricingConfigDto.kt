package com.ares.ewe.data.remote.model

import com.ares.ewe.core.pricing.DeliveryPricingSettings
import com.google.gson.annotations.SerializedName

data class DeliveryPricingConfigDto(
    @SerializedName("baseFee") val baseFee: Double,
    @SerializedName("pricePerKm") val pricePerKm: Double,
    @SerializedName("weatherFee") val weatherFee: Double,
    @SerializedName("defaultDemandMultiplier") val defaultDemandMultiplier: Double,
    @SerializedName("defaultIsRaining") val defaultIsRaining: Boolean,
    @SerializedName("zoneAMaxKm") val zoneAMaxKm: Double,
    @SerializedName("zoneBMaxKm") val zoneBMaxKm: Double,
    @SerializedName("zoneCMaxKm") val zoneCMaxKm: Double,
    @SerializedName("zoneBFee") val zoneBFee: Double,
    @SerializedName("zoneCFee") val zoneCFee: Double,
    @SerializedName("zoneDFee") val zoneDFee: Double,
) {
    fun toSettings(): DeliveryPricingSettings = DeliveryPricingSettings(
        baseFee = baseFee,
        pricePerKm = pricePerKm,
        weatherFee = weatherFee,
        defaultDemandMultiplier = defaultDemandMultiplier,
        defaultIsRaining = defaultIsRaining,
        zoneAMaxKm = zoneAMaxKm,
        zoneBMaxKm = zoneBMaxKm,
        zoneCMaxKm = zoneCMaxKm,
        zoneBFee = zoneBFee,
        zoneCFee = zoneCFee,
        zoneDFee = zoneDFee,
    )
}
