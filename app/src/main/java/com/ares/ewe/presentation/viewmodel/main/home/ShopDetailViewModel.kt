package com.ares.ewe.presentation.viewmodel.main.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe.core.network.toUserFacingMessage
import com.ares.ewe.core.util.ProductCategory
import com.ares.ewe.domain.cart.CartCarWashSingleProductPolicy
import com.ares.ewe.domain.cart.CartShopSwitchPolicy
import com.ares.ewe.domain.cart.PendingCartAddGate
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
    val openingDays: List<String> = emptyList(),
    val isShopAvailableForOrders: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showCarWashSingleProductDialog: Boolean = false,
    val showShopSwitchDialog: Boolean = false,
) {
    val isCarWash: Boolean
        get() = shopType?.trim()?.equals("CAR_WASH", ignoreCase = true) == true

    val showShopClosedBanner: Boolean
        get() = !isShopAvailableForOrders

    val shopReopensLabel: String?
        get() = formatShopReopensLabel(shopStatus, openingHour, openingDays)
    val filteredProducts: List<ShopProduct>
        get() {
            val query = searchQuery.trim()
            return products.filter { product ->
                (isCarWash || ProductCategory.matchesFilter(product.category, selectedCategoryId)) &&
                    (query.isBlank() || product.name.contains(query, ignoreCase = true))
            }
        }
}

@HiltViewModel
class ShopDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val placesRepository: PlacesRepository,
    private val cartRepository: CartRepository,
    private val pendingCartAddGate: PendingCartAddGate,
) : ViewModel() {

    val openedShopId: String = checkNotNull(savedStateHandle.get<String>("id"))
    private val shopName: String = savedStateHandle.get<String>("name").orEmpty()

    val pickupLatitude: Double? = savedStateHandle.get<String>("pickupLat").toNavPickupDouble()
    val pickupLongitude: Double? = savedStateHandle.get<String>("pickupLng").toNavPickupDouble()

    private val _uiState = MutableStateFlow(ShopDetailUiState())
    val uiState: StateFlow<ShopDetailUiState> = _uiState.asStateFlow()
    private var pendingShopSwitchProduct: ShopProduct? = null

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
                val isCarWash = page.shopType?.trim()?.equals("CAR_WASH", ignoreCase = true) == true
                val chips = ProductCategory.filterChips(page.shopType)
                _uiState.update {
                    val selected = it.selectedCategoryId
                    val keepSelected = selected == null || chips.any { chip -> chip.filterId == selected }
                    it.copy(
                        shopName = shopName,
                        shopType = page.shopType,
                        products = page.products,
                        selectedCategoryId = if (isCarWash) null else if (keepSelected) selected else null,
                        isShopAvailableForOrders = isShopAvailableForOrders(
                            shopStatus = page.shopStatus,
                            openingHour = page.openingHour,
                            closingHour = page.closingHour,
                            openingDays = page.openingDays,
                            openingSchedules = page.openingSchedules,
                        ),
                        shopStatus = page.shopStatus,
                        openingHour = page.openingHour,
                        closingHour = page.closingHour,
                        openingDays = page.openingDays,
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

    fun dismissShopSwitchDialog() {
        pendingShopSwitchProduct = null
        _uiState.update { it.copy(showShopSwitchDialog = false) }
    }

    fun confirmShopSwitchAndAdd() {
        viewModelScope.launch {
            cartRepository.clearNow()
            _uiState.update { it.copy(showShopSwitchDialog = false) }
            val product = pendingShopSwitchProduct
            pendingShopSwitchProduct = null
            if (product != null) addToCart(product)
        }
    }

    fun addToCart(
        product: ShopProduct,
        variant: com.ares.ewe.domain.model.ProductVariant? = null,
        quantity: Int = 1,
    ) {
        if (!_uiState.value.isShopAvailableForOrders) return
        val qty = quantity.coerceAtLeast(1)
        viewModelScope.launch {
            val shopId = product.shopId ?: openedShopId
            val cartItems = cartRepository.items.first()
            if (CartShopSwitchPolicy.needsConfirmationIfShopKnown(cartItems, shopId)) {
                pendingShopSwitchProduct = product
                _uiState.update { it.copy(showShopSwitchDialog = true) }
                return@launch
            }
            if (CartCarWashSingleProductPolicy.blocksAdd(
                    shopType = _uiState.value.shopType,
                    cartItems = cartItems,
                    productId = product.id,
                    shopId = shopId,
                    quantityToAdd = qty,
                )
            ) {
                _uiState.update { it.copy(showCarWashSingleProductDialog = true) }
                return@launch
            }
            val validDiscount = product.discount.coerceIn(0, 100)
            val listPrice = variant?.price ?: product.price
            val unitPrice = if (product.hasPromotion && validDiscount > 0) {
                listPrice * (1 - validDiscount / 100.0)
            } else {
                listPrice
            }
            pendingCartAddGate.runOrRequestAddress {
                cartRepository.addItem(
                    productId = product.id,
                    name = product.name,
                    price = unitPrice,
                    quantity = qty,
                    imageUrl = product.imageUrl,
                    listPrice = listPrice,
                    hasPromotion = product.hasPromotion,
                    discount = product.discount,
                    pickupLatitude = pickupLatitude,
                    pickupLongitude = pickupLongitude,
                    shopId = shopId,
                    variantId = variant?.id,
                    variantName = variant?.name,
                )
            }
        }
    }
}

private fun String?.toNavPickupDouble(): Double? {
    val s = this ?: return null
    if (s == "none" || s.isBlank()) return null
    return s.toDoubleOrNull()
}
