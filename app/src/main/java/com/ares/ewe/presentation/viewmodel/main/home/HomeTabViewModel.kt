package com.ares.ewe.presentation.viewmodel.main.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe.domain.model.Ad
import com.ares.ewe.domain.model.ActiveOrder
import com.ares.ewe.domain.model.BestSellerProduct
import com.ares.ewe.domain.model.FeaturedPlace
import com.ares.ewe.domain.model.toAddressWithColonyOnly
import com.ares.ewe.domain.repository.AdsRepository
import com.ares.ewe.domain.repository.CartRepository
import com.ares.ewe.domain.repository.OrderRepository
import com.ares.ewe.domain.repository.PlacesRepository
import com.ares.ewe.domain.repository.UserAddressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

data class HomeTabUiState(
    val featuredPlaces: List<FeaturedPlace> = emptyList(),
    val bestSellerProducts: List<BestSellerProduct> = emptyList(),
    val ads: List<Ad> = emptyList(),
    val activeOrder: ActiveOrder? = null,
    val searchQuery: String = "",
    val addressLabel: String? = null,
    val address: String? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class HomeTabViewModel @Inject constructor(
    private val placesRepository: PlacesRepository,
    private val adsRepository: AdsRepository,
    private val userAddressRepository: UserAddressRepository,
    private val orderRepository: OrderRepository,
    cartRepository: CartRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeTabUiState())
    val uiState: StateFlow<HomeTabUiState> = _uiState.asStateFlow()

    val cartItemCount: StateFlow<Int> = cartRepository.items
        .map { it.sumOf { item -> item.quantity } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        loadHome()
        loadAds()
        loadActiveOrder()
    }

    fun loadActiveOrder() {
        viewModelScope.launch {
            orderRepository.getActiveOrder()
                .onSuccess { order ->
                    _uiState.update { it.copy(activeOrder = order) }
                }
                .onFailure { _ ->
                    _uiState.update { it.copy(activeOrder = null) }
                }
        }
    }

    fun loadAddresses() {
        viewModelScope.launch {
            userAddressRepository.getAddresses()
                .onSuccess { list ->
                    val displayAddress = list.firstOrNull()?.address?.toAddressWithColonyOnly()
                    val addressLabel = list.firstOrNull()?.label ?: "Casa"
                    _uiState.update {
                        it.copy(
                            addressLabel = addressLabel,
                            address = displayAddress
                        )
                    }
                }
                .onFailure { _ ->
                    _uiState.update {
                        it.copy(addressLabel = "Casa", address = null)
                    }
                }
        }
    }

    private fun loadAds() {
        viewModelScope.launch {
            try {
                val ads = adsRepository.getAds()
                _uiState.update { it.copy(ads = ads) }
            } catch (_: Exception) { }
        }
    }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val home = placesRepository.getHome()
                _uiState.update {
                    it.copy(
                        featuredPlaces = home.featuredPlaces,
                        bestSellerProducts = home.bestSellerProducts,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load"
                    )
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            coroutineScope {
                val homeDeferred = async {
                    try {
                        val home = placesRepository.getHome()
                        _uiState.update {
                            it.copy(
                                featuredPlaces = home.featuredPlaces,
                                bestSellerProducts = home.bestSellerProducts,
                                errorMessage = null
                            )
                        }
                    } catch (e: Exception) {
                        _uiState.update { it.copy(errorMessage = e.message ?: "Failed to load") }
                    }
                }
                val addressesDeferred = async {
                    userAddressRepository.getAddresses()
                        .onSuccess { list ->
                            val displayAddress = list.firstOrNull()?.address?.toAddressWithColonyOnly()
                            val addressLabel = list.firstOrNull()?.label ?: "Casa"
                            _uiState.update {
                                it.copy(
                                    addressLabel = addressLabel,
                                    address = displayAddress
                                )
                            }
                        }
                        .onFailure { _ ->
                            _uiState.update { it.copy(addressLabel = "Casa", address = null) }
                        }
                }
                val adsDeferred = async {
                    try {
                        val ads = adsRepository.getAds()
                        _uiState.update { it.copy(ads = ads) }
                    } catch (_: Exception) { }
                }
                val activeOrderDeferred = async {
                    orderRepository.getActiveOrder()
                        .onSuccess { order -> _uiState.update { it.copy(activeOrder = order) } }
                        .onFailure { _ -> _uiState.update { it.copy(activeOrder = null) } }
                }
                homeDeferred.await()
                addressesDeferred.await()
                adsDeferred.await()
                activeOrderDeferred.await()
            }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }
}
