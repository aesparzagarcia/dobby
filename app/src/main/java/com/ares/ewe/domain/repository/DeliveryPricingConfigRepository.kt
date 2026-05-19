package com.ares.ewe.domain.repository

import com.ares.ewe.core.pricing.DeliveryPricingSettings
import kotlinx.coroutines.flow.StateFlow

interface DeliveryPricingConfigRepository {
    val settings: StateFlow<DeliveryPricingSettings>
    suspend fun refresh()
}
