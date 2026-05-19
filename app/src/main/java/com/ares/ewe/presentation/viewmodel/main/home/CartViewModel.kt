package com.ares.ewe.presentation.viewmodel.main.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe.core.delivery.DeliveryEtaEstimator
import com.ares.ewe.core.network.toUserFacingMessage
import com.ares.ewe.core.pricing.DeliveryPricingCalculator
import com.ares.ewe.core.pricing.DeliveryPricingInput
import com.ares.ewe.core.pricing.GeoDistance
import com.ares.ewe.core.pricing.OrderPricing
import com.ares.ewe.core.pricing.roundMoney
import com.ares.ewe.domain.model.CartItem
import com.ares.ewe.domain.model.toAddressWithColonyOnly
import com.ares.ewe.domain.repository.CartRepository
import com.ares.ewe.domain.repository.DeliveryPricingConfigRepository
import com.ares.ewe.domain.repository.OrderRepository
import com.ares.ewe.domain.repository.PlacesRepository
import com.ares.ewe.domain.repository.UserAddressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CartUiState(
    val items: List<CartItem> = emptyList(),
    val productsSubtotal: Double = 0.0,
    /** Desglose de envío; `null` si no hay coords de entrega o pickups. */
    val pricing: OrderPricing? = null,
    val grandTotal: Double = 0.0,
    val addressId: String? = null,
    val addressLabel: String = "Casa",
    val addressText: String = "",
    val addressDetails: String? = null,
    val userLatitude: Double? = null,
    val userLongitude: Double? = null,
    /** Valor por defecto del data class; en UI el `combine` asigna el resultado de [DeliveryEtaEstimator]. */
    val estimatedDeliveryTime: String = "30–45 min",
    val paymentMethod: String = "Efectivo contra entrega",
    val isPlacingOrder: Boolean = false,
    val orderPlaced: Boolean = false,
    val placeOrderError: String? = null
)

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val userAddressRepository: UserAddressRepository,
    private val orderRepository: OrderRepository,
    private val placesRepository: PlacesRepository,
    private val deliveryPricingConfigRepository: DeliveryPricingConfigRepository,
) : ViewModel() {

    init {
        viewModelScope.launch { deliveryPricingConfigRepository.refresh() }
    }

    private val _deliveryState = MutableStateFlow(
        CartUiState(
            addressLabel = "Casa",
            addressText = "",
            addressDetails = null,
            userLatitude = null,
            userLongitude = null,
            estimatedDeliveryTime = "30–45 min",
            paymentMethod = "Efectivo contra entrega"
        )
    )
    private val deliveryState: StateFlow<CartUiState> = _deliveryState.asStateFlow()

    private val shopCoordsByShopId: StateFlow<Map<String, Pair<Double, Double>>> = cartRepository.items
        .map { items -> items.mapNotNull { it.shopId }.toSet() }
        .distinctUntilChanged()
        .flatMapLatest { shopIds ->
            flow {
                if (shopIds.isEmpty()) {
                    emit(emptyMap())
                } else {
                    emit(
                        try {
                            placesRepository.getShopCoordinatesByShopId()
                        } catch (_: Exception) {
                            emptyMap()
                        }
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    val uiState: StateFlow<CartUiState> = combine(
        cartRepository.items.map { list ->
            CartUiState(
                items = list,
                productsSubtotal = roundMoney(list.sumOf { it.lineTotal }),
            )
        },
        deliveryState,
        shopCoordsByShopId,
        deliveryPricingConfigRepository.settings,
    ) { cart, delivery, shopCoords, pricingSettings ->
        val eta = DeliveryEtaEstimator.estimateLabel(
            userLat = delivery.userLatitude,
            userLng = delivery.userLongitude,
            items = cart.items,
            shopCoordsByShopId = shopCoords,
        )
        val productsSubtotal = cart.productsSubtotal
        val distanceKm = GeoDistance.maxRoadKmFromPickups(
            userLat = delivery.userLatitude,
            userLng = delivery.userLongitude,
            items = cart.items,
            shopCoordsByShopId = shopCoords,
        )
        val orderPricing = distanceKm?.let { km ->
            val deliveryBreakdown = DeliveryPricingCalculator.calculate(
                DeliveryPricingInput(
                    distanceKm = km,
                    demandMultiplier = pricingSettings.defaultDemandMultiplier,
                    isRaining = pricingSettings.defaultIsRaining,
                ),
                config = pricingSettings,
            )
            OrderPricing(
                productsSubtotal = productsSubtotal,
                delivery = deliveryBreakdown,
            )
        }
        val grandTotal = orderPricing?.grandTotal ?: productsSubtotal
        cart.copy(
            productsSubtotal = productsSubtotal,
            pricing = orderPricing,
            grandTotal = grandTotal,
            addressId = delivery.addressId,
            addressLabel = delivery.addressLabel,
            addressText = delivery.addressText,
            addressDetails = delivery.addressDetails,
            userLatitude = delivery.userLatitude,
            userLongitude = delivery.userLongitude,
            estimatedDeliveryTime = eta,
            paymentMethod = delivery.paymentMethod,
            isPlacingOrder = delivery.isPlacingOrder,
            orderPlaced = delivery.orderPlaced,
            placeOrderError = delivery.placeOrderError
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CartUiState()
    )

    init {
        viewModelScope.launch {
            userAddressRepository.getAddresses().onSuccess { addresses ->
                val addr = addresses.firstOrNull()
                if (addr != null) {
                    _deliveryState.update {
                        it.copy(
                            addressId = addr.id,
                            addressLabel = addr.label,
                            addressText = addr.address.toAddressWithColonyOnly(),
                            addressDetails = addr.description?.takeIf { d -> d.isNotBlank() },
                            userLatitude = addr.lat,
                            userLongitude = addr.lng,
                        )
                    }
                } else {
                    _deliveryState.update {
                        it.copy(
                            addressId = null,
                            addressLabel = "Casa",
                            addressText = "",
                            addressDetails = null,
                            userLatitude = null,
                            userLongitude = null,
                        )
                    }
                }
            }
        }
    }

    fun placeOrder(addressId: String?, items: List<CartItem>) {
        if (addressId == null || items.isEmpty()) {
            _deliveryState.update {
                it.copy(placeOrderError = if (items.isEmpty()) "Carrito vacío: agrega productos para pedir." else "Dirección: selecciona una dirección de entrega.")
            }
            return
        }
        viewModelScope.launch {
            _deliveryState.update {
                it.copy(isPlacingOrder = true, placeOrderError = null)
            }
            orderRepository.createOrder(addressId, items)
                .onSuccess {
                    cartRepository.clear()
                    // Paridad iOS: mantener overlay ~5s tras éxito antes de cerrar carrito.
                    delay(5_000)
                    _deliveryState.update {
                        it.copy(isPlacingOrder = false, orderPlaced = true, placeOrderError = null)
                    }
                }
                .onFailure { e ->
                    _deliveryState.update {
                        it.copy(
                            isPlacingOrder = false,
                            placeOrderError = e.toUserFacingMessage()
                        )
                    }
                }
        }
    }

    fun clearOrderPlaced() {
        _deliveryState.update { it.copy(orderPlaced = false) }
    }

    fun clearPlaceOrderError() {
        _deliveryState.update { it.copy(placeOrderError = null) }
    }

    fun removeItem(productId: String) {
        cartRepository.removeItem(productId)
    }

    fun updateQuantity(productId: String, quantity: Int) {
        cartRepository.updateQuantity(productId, quantity)
    }

    fun clearCart() {
        cartRepository.clear()
    }
}
