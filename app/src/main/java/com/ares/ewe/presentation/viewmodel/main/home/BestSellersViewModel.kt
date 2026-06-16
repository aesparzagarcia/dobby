package com.ares.ewe.presentation.viewmodel.main.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe.core.network.toUserFacingMessage
import com.ares.ewe.core.util.ProductCategory
import com.ares.ewe.domain.model.FeaturedPlace
import com.ares.ewe.domain.model.ShopProduct
import com.ares.ewe.domain.repository.CartRepository
import com.ares.ewe.domain.repository.PlacesRepository
import com.ares.ewe.presentation.ui.main.home.isProductShopAvailableForOrders
import com.ares.ewe.presentation.ui.main.home.sortShopProductsByShopAvailability
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BestSellersUiState(
    val products: List<ShopProduct> = emptyList(),
    val featuredPlaces: List<FeaturedPlace> = emptyList(),
    val searchQuery: String = "",
    val selectedCategoryId: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
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
class BestSellersViewModel @Inject constructor(
    private val placesRepository: PlacesRepository,
    private val cartRepository: CartRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BestSellersUiState(isLoading = true))
    val uiState: StateFlow<BestSellersUiState> = _uiState.asStateFlow()

    val cartItemCount: StateFlow<Int> = cartRepository.items
        .map { it.sumOf { item -> item.quantity } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        loadBestSellers()
    }

    fun loadBestSellers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val productsDeferred = async { placesRepository.getBestSellers() }
                val homeDeferred = async { placesRepository.getHome() }
                val products = productsDeferred.await()
                val featuredPlaces = homeDeferred.await().featuredPlaces
                _uiState.update {
                    it.copy(
                        products = sortShopProductsByShopAvailability(products, featuredPlaces),
                        featuredPlaces = featuredPlaces,
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

    fun isProductAvailable(product: ShopProduct): Boolean =
        isProductShopAvailableForOrders(product.shopId, _uiState.value.featuredPlaces)

    fun addToCart(product: ShopProduct) {
        if (!isProductAvailable(product)) return
        val shopId = product.shopId?.trim().orEmpty().ifEmpty { return }
        val place = _uiState.value.featuredPlaces.find { it.id == shopId && !it.isService }
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
            pickupLatitude = place?.latitude,
            pickupLongitude = place?.longitude,
            shopId = shopId,
        )
    }
}
