package com.ares.ewe.presentation.viewmodel.main.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe.core.network.toUserFacingMessage
import com.ares.ewe.domain.model.ShopProduct
import com.ares.ewe.domain.repository.CartRepository
import com.ares.ewe.domain.repository.PlacesRepository
import com.ares.ewe.presentation.ui.main.home.formatShopReopensLabel
import com.ares.ewe.presentation.ui.main.home.isShopAvailableForOrders
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CarWashTab {
    Services,
    Portfolio,
    Reviews,
}

data class CarWashDetailUiState(
    val shopName: String = "",
    val logoUrl: String? = null,
    val rate: Float = 0f,
    val ratingCount: Int = 0,
    val jobsDone: Int = 0,
    val products: List<ShopProduct> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val selectedTab: CarWashTab = CarWashTab.Services,
    val shopStatus: String? = null,
    val openingHour: String? = null,
    val closingHour: String? = null,
    val isShopAvailableForOrders: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val showShopClosedBanner: Boolean
        get() = !isShopAvailableForOrders

    val shopReopensLabel: String?
        get() = formatShopReopensLabel(shopStatus, openingHour)

    val selectedProducts: List<ShopProduct>
        get() = products.filter { it.id in selectedIds }

    val selectedTotal: Double
        get() = selectedProducts.sumOf { it.unitPrice() }
}

private fun ShopProduct.unitPrice(): Double {
    val validDiscount = discount.coerceIn(0, 100)
    return if (hasPromotion && validDiscount > 0) {
        price * (1 - validDiscount / 100.0)
    } else {
        price
    }
}

@HiltViewModel
class CarWashDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val placesRepository: PlacesRepository,
    private val cartRepository: CartRepository,
) : ViewModel() {

    val openedShopId: String = checkNotNull(savedStateHandle.get<String>("id"))
    private val navShopName: String = savedStateHandle.get<String>("name").orEmpty()

    val pickupLatitude: Double? = savedStateHandle.get<String>("pickupLat").toNavPickupDouble()
    val pickupLongitude: Double? = savedStateHandle.get<String>("pickupLng").toNavPickupDouble()

    private val _uiState = MutableStateFlow(CarWashDetailUiState(shopName = navShopName))
    val uiState: StateFlow<CarWashDetailUiState> = _uiState.asStateFlow()

    val cartItemCount: StateFlow<Int> = cartRepository.items
        .map { it.sumOf { item -> item.quantity } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val page = placesRepository.getShopProducts(openedShopId)
                _uiState.update {
                    it.copy(
                        shopName = page.shopName?.takeIf { n -> n.isNotBlank() } ?: navShopName,
                        logoUrl = page.logoUrl,
                        rate = page.rate,
                        ratingCount = page.ratingCount,
                        jobsDone = page.jobsDone,
                        products = page.products,
                        isShopAvailableForOrders = isShopAvailableForOrders(
                            shopStatus = page.shopStatus,
                            openingHour = page.openingHour,
                            closingHour = page.closingHour,
                        ),
                        shopStatus = page.shopStatus,
                        openingHour = page.openingHour,
                        closingHour = page.closingHour,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.toUserFacingMessage(),
                    )
                }
            }
        }
    }

    fun onTabSelected(tab: CarWashTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun toggleService(productId: String) {
        if (!_uiState.value.isShopAvailableForOrders) return
        _uiState.update { state ->
            val next = state.selectedIds.toMutableSet()
            if (!next.add(productId)) next.remove(productId)
            state.copy(selectedIds = next)
        }
    }

    fun addSelectedToCart(): Boolean {
        val state = _uiState.value
        if (!state.isShopAvailableForOrders || state.selectedProducts.isEmpty()) return false
        state.selectedProducts.forEach { product ->
            cartRepository.addItem(
                productId = product.id,
                name = product.name,
                price = product.unitPrice(),
                quantity = 1,
                imageUrl = product.imageUrl ?: state.logoUrl,
                listPrice = product.price,
                hasPromotion = product.hasPromotion,
                discount = product.discount,
                pickupLatitude = pickupLatitude,
                pickupLongitude = pickupLongitude,
                shopId = product.shopId ?: openedShopId,
            )
        }
        _uiState.update { it.copy(selectedIds = emptySet()) }
        return true
    }
}
