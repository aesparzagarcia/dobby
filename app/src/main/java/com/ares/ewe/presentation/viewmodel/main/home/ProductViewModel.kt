package com.ares.ewe.presentation.viewmodel.main.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe.core.network.toUserFacingMessage
import com.ares.ewe.domain.model.FavoriteProduct
import com.ares.ewe.domain.model.ProductDetail
import com.ares.ewe.domain.repository.CartRepository
import com.ares.ewe.domain.repository.FavoritesRepository
import com.ares.ewe.domain.repository.PlacesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductUiState(
    val product: ProductDetail? = null,
    val quantity: Int = 0,
    val isFavorite: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val total: Double
        get() = (product?.price ?: 0.0) * quantity
}

@HiltViewModel
class ProductViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val placesRepository: PlacesRepository,
    private val cartRepository: CartRepository,
    private val favoritesRepository: FavoritesRepository,
) : ViewModel() {

    private val productId: String = checkNotNull(savedStateHandle.get<String>("id"))

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
                _uiState.update {
                    it.copy(
                        product = product,
                        quantity = 0,
                        isLoading = false,
                        errorMessage = null
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

    fun incrementQuantity() {
        _uiState.update { it.copy(quantity = (it.quantity + 1).coerceAtMost(999)) }
    }

    fun decrementQuantity() {
        _uiState.update { it.copy(quantity = (it.quantity - 1).coerceAtLeast(0)) }
    }

    fun addToCart() {
        val product = _uiState.value.product ?: return
        val quantity = _uiState.value.quantity
        if (quantity <= 0) return
        val imageUrl = product.imageUrls.firstOrNull()
        cartRepository.addItem(
            productId = product.id,
            name = product.name,
            price = product.price,
            quantity = quantity,
            imageUrl = imageUrl
        )
    }

    fun toggleFavorite() {
        val product = _uiState.value.product ?: return
        favoritesRepository.toggleFavorite(
            FavoriteProduct(
                productId = product.id,
                name = product.name,
                price = product.price,
                imageUrl = product.imageUrls.firstOrNull(),
                rate = product.rate,
                hasPromotion = product.hasPromotion,
                discount = product.discount,
            )
        )
    }
}
