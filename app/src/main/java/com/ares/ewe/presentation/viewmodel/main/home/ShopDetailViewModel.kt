package com.ares.ewe.presentation.viewmodel.main.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe.core.network.toUserFacingMessage
import com.ares.ewe.core.util.ProductCategory
import com.ares.ewe.domain.cart.CartCarWashSingleProductPolicy
import com.ares.ewe.presentation.ui.main.home.formatShopReopensLabel
import com.ares.ewe.presentation.ui.main.home.isShopAvailableForOrders
import com.ares.ewe.domain.model.ShopProduct
import com.ares.ewe.domain.repository.CartRepository
import com.ares.ewe.domain.repository.PlacesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShopDetailUiState(
    val shopName: String = "",
    val shopType: String? = null,
    val products: List<ShopProduct> = emptyList(),
    val searchQuery: String = "",
    val selectedCategoryId: String? = null,
    val shopStatus: String? = null,
    val openingHour: String? = null,
    val closingHour: String? = null,
    val isShopAvailableForOrders: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showCarWashSingleProductDialog: Boolean = false,
) {
    val showShopClosedBanner: Boolean
        get() = !isShopAvailableForOrders

    val shopReopensLabel: String?
        get() = formatShopReopensLabel(shopStatus, openingHour)
    val filteredProducts: List<ShopProduct>
        get() {
            val query = searchQuery.trim()
            return products.filter { product ->
                ProductCategory.matchesFilter(product.category, selectedCategoryId) &&
                    (query.isBlank() || product.name.contains(query, ignoreCase = true))
            }
        }
}

@HiltViewModel
class ShopDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val placesRepository: PlacesRepository,
    private val cartRepository: CartRepository,
) : ViewModel() {

    val openedShopId: String = checkNotNull(savedStateHandle.get<String>("id"))
    private val shopName: String = savedStateHandle.get<String>("name").orEmpty()

    val pickupLatitude: Double? = savedStateHandle.get<String>("pickupLat").toNavPickupDouble()
    val pickupLongitude: Double? = savedStateHandle.get<String>("pickupLng").toNavPickupDouble()

    private val _uiState = MutableStateFlow(ShopDetailUiState())
    val uiState: StateFlow<ShopDetailUiState> = _uiState.asStateFlow()

    val cartItemCount: StateFlow<Int> = cartRepository.items
        .map { it.sumOf { item -> item.quantity } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        loadProducts()
    }

    fun loadProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val page = placesRepository.getShopProducts(openedShopId)
                _uiState.update {
                    it.copy(
                        shopName = shopName,
                        shopType = page.shopType,
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

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onCategorySelected(categoryId: String?) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun dismissCarWashSingleProductDialog() {
        _uiState.update { it.copy(showCarWashSingleProductDialog = false) }
    }

    fun addToCart(product: ShopProduct) {
        if (!_uiState.value.isShopAvailableForOrders) return
        viewModelScope.launch {
            val shopId = product.shopId ?: openedShopId
            val cartItems = cartRepository.items.first()
            if (CartCarWashSingleProductPolicy.blocksAdd(
                    shopType = _uiState.value.shopType,
                    cartItems = cartItems,
                    productId = product.id,
                    shopId = shopId,
                    quantityToAdd = 1,
                )
            ) {
                _uiState.update { it.copy(showCarWashSingleProductDialog = true) }
                return@launch
            }
            val validDiscount = product.discount.coerceIn(0, 100)
            val unitPrice = if (product.hasPromotion && validDiscount > 0) {
                product.price * (1 - validDiscount / 100.0)
            } else {
                product.price
            }
            cartRepository.addItem(
                productId = product.id,
                name = product.name,
                price = unitPrice,
                quantity = 1,
                imageUrl = product.imageUrl,
                listPrice = product.price,
                hasPromotion = product.hasPromotion,
                discount = product.discount,
                pickupLatitude = pickupLatitude,
                pickupLongitude = pickupLongitude,
                shopId = shopId,
            )
        }
    }
}

private fun String?.toNavPickupDouble(): Double? {
    val s = this ?: return null
    if (s == "none" || s.isBlank()) return null
    return s.toDoubleOrNull()
}
