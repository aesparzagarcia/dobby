package com.ares.ewe.data.repository

import com.ares.ewe.core.pricing.DeliveryPricingSettings
import com.ares.ewe.data.remote.api.DobbyApi
import com.ares.ewe.domain.repository.DeliveryPricingConfigRepository
import com.ares.ewe.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeliveryPricingConfigRepositoryImpl @Inject constructor(
    private val api: DobbyApi,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : DeliveryPricingConfigRepository {

    private val _settings = MutableStateFlow(DeliveryPricingSettings.DEFAULT)
    override val settings: StateFlow<DeliveryPricingSettings> = _settings.asStateFlow()

    init {
        applicationScope.launch { refresh() }
    }

    override suspend fun refresh() {
        try {
            val dto = api.getDeliveryPricingConfig()
            _settings.value = dto.toSettings()
        } catch (_: Exception) {
            // Mantener último valor o DEFAULT
        }
    }
}
