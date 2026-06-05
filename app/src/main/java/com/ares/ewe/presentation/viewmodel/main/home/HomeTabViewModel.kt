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
import com.ares.ewe.core.network.toUserFacingMessage
import com.ares.ewe.domain.repository.UserAddressRepository
import com.ares.ewe.push.ConsumerOrderRealtimeBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

data class HomeTabUiState(
    val featuredPlaces: List<FeaturedPlace> = emptyList(),
    val bestSellerProducts: List<BestSellerProduct> = emptyList(),
    val ads: List<Ad> = emptyList(),
    val activeOrders: List<ActiveOrder> = emptyList(),
    val searchQuery: String = "",
    val addressLabel: String? = null,
    val address: String? = null,
    /** True after the first `getAddresses` completes (success or failure). */
    val addressFetchCompleted: Boolean = false,
    /** True when the user has no saved addresses (API returned an empty list). */
    val needsDeliveryAddressCallout: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    /** Error secundario (anuncios, direcciones) mientras el home ya cargó; no bloquea la pantalla. */
    val warningMessage: String? = null,
)

@HiltViewModel
class HomeTabViewModel @Inject constructor(
    private val placesRepository: PlacesRepository,
    private val adsRepository: AdsRepository,
    private val userAddressRepository: UserAddressRepository,
    private val orderRepository: OrderRepository,
    private val orderRealtimeBus: ConsumerOrderRealtimeBus,
    cartRepository: CartRepository,
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
        viewModelScope.launch {
            orderRealtimeBus.events.collect {
                refreshActiveOrder(clearOnFailure = false)
            }
        }
    }

    fun loadActiveOrder() {
        viewModelScope.launch { refreshActiveOrder() }
    }

    /** Espera el API antes de volver al home tras checkout (paridad iOS). */
    suspend fun refreshActiveOrder(clearOnFailure: Boolean = true) {
        orderRepository.getActiveOrders()
            .onSuccess { orders ->
                _uiState.update { it.copy(activeOrders = orders) }
            }
            .onFailure { _ ->
                if (clearOnFailure) {
                    _uiState.update { it.copy(activeOrders = emptyList()) }
                }
            }
    }

    /**
     * Tras crear un pedido, el GET /active puede tardar un instante (o perder una carrera con Firestore).
     * Reintenta antes de volver al home para que el tracking aparezca sin pull-to-refresh.
     */
    suspend fun refreshActiveOrderAfterCheckout() {
        repeat(6) { attempt ->
            refreshActiveOrder(clearOnFailure = false)
            if (_uiState.value.activeOrders.isNotEmpty()) return
            if (attempt < 5) delay(400)
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
                            address = displayAddress,
                            addressFetchCompleted = true,
                            needsDeliveryAddressCallout = list.isEmpty(),
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            addressLabel = "Casa",
                            address = null,
                            addressFetchCompleted = true,
                            needsDeliveryAddressCallout = false,
                            warningMessage = e.toUserFacingMessage()
                        )
                    }
                }
        }
    }

    private fun loadAds() {
        viewModelScope.launch {
            try {
                val ads = adsRepository.getAds()
                _uiState.update { it.copy(ads = ads) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(ads = emptyList(), warningMessage = e.toUserFacingMessage())
                }
            }
        }
    }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, warningMessage = null) }
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
                        errorMessage = e.toUserFacingMessage()
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
                        _uiState.update { it.copy(errorMessage = e.toUserFacingMessage()) }
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
                                    address = displayAddress,
                                    addressFetchCompleted = true,
                                    needsDeliveryAddressCallout = list.isEmpty(),
                                )
                            }
                        }
                        .onFailure { e ->
                            _uiState.update {
                                it.copy(
                                    addressLabel = "Casa",
                                    address = null,
                                    addressFetchCompleted = true,
                                    needsDeliveryAddressCallout = false,
                                    warningMessage = e.toUserFacingMessage()
                                )
                            }
                        }
                }
                val adsDeferred = async {
                    try {
                        val ads = adsRepository.getAds()
                        _uiState.update { it.copy(ads = ads) }
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(ads = emptyList(), warningMessage = e.toUserFacingMessage())
                        }
                    }
                }
                val activeOrderDeferred = async { refreshActiveOrder(clearOnFailure = false) }
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

    fun clearWarningMessage() {
        _uiState.update { it.copy(warningMessage = null) }
    }
}
