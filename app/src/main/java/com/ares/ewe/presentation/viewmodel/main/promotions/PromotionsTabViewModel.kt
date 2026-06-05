package com.ares.ewe.presentation.viewmodel.main.promotions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe.core.network.toUserFacingMessage
import com.ares.ewe.domain.model.BestSellerProduct
import com.ares.ewe.domain.model.FeaturedPlace
import com.ares.ewe.domain.repository.PlacesRepository
import com.ares.ewe.presentation.ui.main.home.isProductShopAvailableForOrders
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PromotionsUiState(
    val products: List<BestSellerProduct> = emptyList(),
    val featuredPlaces: List<com.ares.ewe.domain.model.FeaturedPlace> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class PromotionsTabViewModel @Inject constructor(
    private val placesRepository: PlacesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PromotionsUiState(isLoading = true))
    val uiState: StateFlow<PromotionsUiState> = _uiState.asStateFlow()

    init {
        loadPromotions()
    }

    fun loadPromotions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val featuredPlaces = runCatching { placesRepository.getHome().featuredPlaces }.getOrDefault(emptyList())
                val promotions = placesRepository.getPromotions()
                    .filter { it.hasPromotion && it.discount > 0 }
                    .filter { isProductShopAvailableForOrders(it.shopId, featuredPlaces) }
                _uiState.value = PromotionsUiState(
                    products = promotions,
                    featuredPlaces = featuredPlaces,
                    isLoading = false,
                    errorMessage = null,
                )
            } catch (e: Exception) {
                _uiState.value = PromotionsUiState(
                    products = emptyList(),
                    isLoading = false,
                    errorMessage = e.toUserFacingMessage(),
                )
            }
        }
    }
}
