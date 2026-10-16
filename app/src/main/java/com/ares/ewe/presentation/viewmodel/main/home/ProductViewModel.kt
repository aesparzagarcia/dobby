package com.ares.ewe.presentation.viewmodel.main.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe.core.network.toUserFacingMessage
import com.ares.ewe.core.util.normalizeImageUrlForStorage
import com.ares.ewe.domain.cart.CartCarWashSingleProductPolicy
import com.ares.ewe.domain.cart.CartShopSwitchPolicy
import com.ares.ewe.domain.cart.PendingCartAddGate
import com.ares.ewe.domain.model.FavoriteProduct
import com.ares.ewe.domain.model.ProductDetail
import com.ares.ewe.domain.model.ProductVariant
import com.ares.ewe.domain.repository.CartRepository
import com.ares.ewe.domain.repository.FavoritesRepository
import com.ares.ewe.domain.repository.PlacesRepository
import com.ares.ewe.presentation.ui.main.home.formatShopReopensLabel
import com.ares.ewe.presentation.ui.main.home.isShopAvailableForOrders
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductUiState(
    val product: ProductDetail? = null,
    val selectedVariantId: String = "",
    val quantity: Int = 1,
    val ratingCount: Int = 0,
    val isProductAvailable: Boolean = true,
    val shopStatus: String? = null,
    val shopReopensLabel: String? = null,
    val shopId: String? = null,
    val shopName: String? = null,
    val shopLogoUrl: String? = null,
    val shopType: String? = null,
    val shopRate: Float = 0f,
    val shopRatingCount: Int = 0,
    val isFavorite: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showCarWashSingleProductDialog: Boolean = false,
    val showShopSwitchDialog: Boolean = false,
) {
    val showShopClosedBanner: Boolean
        get() = !isProductAvailable

    val selectedVariant: ProductVariant?
        get() {
            val p = product ?: return null
            if (!p.hasMultiplePresentations) return null
            return p.variants.find { it.id == selectedVariantId }
                ?: p.variants.minByOrNull { it.price }
        }

    val listPrice: Double
        get() = selectedVariant?.price ?: product?.price ?: 0.0

    /** Precio por unidad que paga el cliente (con descuento si aplica). */
    val unitPriceEffective: Double
        get() {
            val p = product ?: return 0.0
            val d = p.discount.coerceIn(0, 100)
            return if (p.hasPromotion && d > 0) listPrice * (1 - d / 100.0) else listPrice
        }

    val total: Double
        get() = unitPriceEffective * quantity
}

@HiltViewModel
class ProductViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val placesRepository: PlacesRepository,
    private val cartRepository: CartRepository,
    private val favoritesRepository: FavoritesRepository,
    private val pendingCartAddGate: PendingCartAddGate,
) : ViewModel() {

    private val productId: String = checkNotNull(savedStateHandle.get<String>("id"))
    val pickupLatitude: Double? = savedStateHandle.get<String>("pickupLat").toNavPickupDouble()
    val pickupLongitude: Double? = savedStateHandle.get<String>("pickupLng").toNavPickupDouble()
    private val navShopId: String? = savedStateHandle.get<String>("shopId").toNavShopId()
    private val shopAvailable: Boolean = savedStateHandle.get<Boolean>("shopAvailable") ?: true

    private val _uiState = MutableStateFlow(ProductUiState())
    val uiState: StateFlow<ProductUiState> = _uiState.asStateFlow()

    val cartItemCount: StateFlow<Int> = cartRepository.items
        .map { it.sumOf { item -> item.quantity } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        loadProduct()
        observeFavorite()
    }

    private fun observeFavorite() {
        viewModelScope.launch {
            favoritesRepository.isFavorite(productId).collect { favorite ->
                _uiState.update { it.copy(isFavorite = favorite) }
            }
        }
    }

    fun loadProduct() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val product = placesRepository.getProduct(productId)
                val shopId = product.shopId?.trim()?.takeIf { it.isNotEmpty() }
                    ?: navShopId
                var available = shopAvailable
                var status: String? = null
                var reopens: String? = null
                var shopName = product.shopName
                var shopLogoUrl = product.shopLogoUrl
                var shopType = product.shopType
                var shopRate = product.shopRate
                var shopRatingCount = product.shopRatingCount
                if (!shopId.isNullOrBlank()) {
                    try {
                        val page = placesRepository.getShopProducts(shopId)
                        available = isShopAvailableForOrders(
                            shopStatus = page.shopStatus,
                            openingHour = page.openingHour,
                            closingHour = page.closingHour,
                            openingDays = page.openingDays,
                            openingSchedules = page.openingSchedules,
                        )
                        status = page.shopStatus
                        reopens = formatShopReopensLabel(
                            shopStatus = page.shopStatus,
                            openingHour = page.openingHour,
                            openingDays = page.openingDays,
                        )
                        shopName = page.shopName?.trim()?.takeIf { it.isNotEmpty() } ?: shopName
                        shopLogoUrl = page.logoUrl ?: shopLogoUrl
                        shopType = page.shopType ?: shopType
                        shopRate = page.rate
                        shopRatingCount = page.ratingCount
                    } catch (_: Exception) {
                        // Keep nav shopAvailable; banner can still show without reopens text.
                    }
                }
                val cheapestId = product.variants.minByOrNull { it.price }?.id.orEmpty()
                _uiState.update {
                    it.copy(
                        product = product,
                        selectedVariantId = cheapestId,
                        quantity = 1,
                        ratingCount = product.ratingCount,
                        isProductAvailable = available,
                        shopStatus = status,
                        shopReopensLabel = reopens,
                        shopId = shopId,
                        shopName = shopName,
                        shopLogoUrl = shopLogoUrl,
                        shopType = shopType,
                        shopRate = shopRate,
                        shopRatingCount = shopRatingCount,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.toUserFacingMessage()
                    )
                }
            }
        }
    }

    fun selectVariant(variant: ProductVariant) {
        _uiState.update { it.copy(selectedVariantId = variant.id) }
    }

    fun incrementQuantity() {
        val state = _uiState.value
        if (CartCarWashSingleProductPolicy.isCarWash(state.product?.shopType)) {
            if (state.quantity >= 1) {
                _uiState.update { it.copy(showCarWashSingleProductDialog = true) }
                return
            }
        }
        _uiState.update { it.copy(quantity = (it.quantity + 1).coerceAtMost(999)) }
    }

    fun decrementQuantity() {
        _uiState.update { it.copy(quantity = (it.quantity - 1).coerceAtLeast(1)) }
    }

    fun dismissCarWashSingleProductDialog() {
        _uiState.update { it.copy(showCarWashSingleProductDialog = false) }
    }

    fun dismissShopSwitchDialog() {
        _uiState.update { it.copy(showShopSwitchDialog = false) }
    }

    fun confirmShopSwitchAndAdd(onAdded: () -> Unit = {}) {
        viewModelScope.launch {
            cartRepository.clearNow()
            _uiState.update { it.copy(showShopSwitchDialog = false) }
            addToCart(onAdded)
        }
    }

    fun addToCart(onAdded: () -> Unit = {}) {
        val product = _uiState.value.product ?: return
        if (!_uiState.value.isProductAvailable) return
        val quantity = _uiState.value.quantity
        if (quantity <= 0) return
        viewModelScope.launch {
            val cartItems = cartRepository.items.first()
            val shopId = product.shopId ?: navShopId
            if (CartShopSwitchPolicy.needsConfirmationIfShopKnown(cartItems, shopId)) {
                _uiState.update { it.copy(showShopSwitchDialog = true) }
                return@launch
            }
            if (CartCarWashSingleProductPolicy.blocksAdd(
                    shopType = product.shopType,
                    cartItems = cartItems,
                    productId = product.id,
                    shopId = shopId,
                    quantityToAdd = quantity,
                )
            ) {
                _uiState.update { it.copy(showCarWashSingleProductDialog = true) }
                return@launch
            }
            val imageUrl = product.imageUrls.firstOrNull()
            val state = _uiState.value
            val unitPrice = state.unitPriceEffective
            val variant = state.selectedVariant
            pendingCartAddGate.runOrRequestAddress {
                cartRepository.addItem(
                    productId = product.id,
                    name = product.name,
                    price = unitPrice,
                    quantity = quantity,
                    imageUrl = imageUrl,
                    listPrice = state.listPrice,
                    hasPromotion = product.hasPromotion,
                    discount = product.discount,
                    pickupLatitude = pickupLatitude,
                    pickupLongitude = pickupLongitude,
                    shopId = shopId,
                    variantId = variant?.id,
                    variantName = variant?.name,
                )
                onAdded()
            }
        }
    }

    fun toggleFavorite() {
        val product = _uiState.value.product ?: return
        favoritesRepository.toggleFavorite(
            FavoriteProduct(
                productId = product.id,
                name = product.name,
                price = product.price,
                imageUrl = product.imageUrls.firstOrNull().normalizeImageUrlForStorage(),
                rate = product.rate,
                hasPromotion = product.hasPromotion,
                discount = product.discount,
            )
        )
    }
}

private fun String?.toNavPickupDouble(): Double? {
    val s = this ?: return null
    if (s == "none" || s.isBlank()) return null
    return s.toDoubleOrNull()
}

private fun String?.toNavShopId(): String? {
    val s = this ?: return null
    if (s == "none" || s.isBlank()) return null
    return s
}
