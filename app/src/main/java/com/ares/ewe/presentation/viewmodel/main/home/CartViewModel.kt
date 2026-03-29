package com.ares.ewe.presentation.viewmodel.main.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe.core.network.toUserFacingMessage
import com.ares.ewe.domain.model.CartItem
import com.ares.ewe.domain.model.toAddressWithColonyOnly
import com.ares.ewe.domain.repository.CartRepository
import com.ares.ewe.domain.repository.OrderRepository
import com.ares.ewe.domain.repository.UserAddressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CartUiState(
    val items: List<CartItem> = emptyList(),
    val grandTotal: Double = 0.0,
    val addressId: String? = null,
    val addressLabel: String = "Home",
    val addressText: String = "",
    val addressDetails: String? = null,
    val estimatedDeliveryTime: String = "30–45 min",
    val paymentMethod: String = "Cash on delivery",
    val isPlacingOrder: Boolean = false,
    val orderPlaced: Boolean = false,
    val placeOrderError: String? = null
)

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val userAddressRepository: UserAddressRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _deliveryState = MutableStateFlow(CartUiState(addressLabel = "Home", addressText = "", addressDetails = null, estimatedDeliveryTime = "30–45 min", paymentMethod = "Cash on delivery"))
    private val deliveryState: StateFlow<CartUiState> = _deliveryState.asStateFlow()

    val uiState: StateFlow<CartUiState> = combine(
        cartRepository.items.map { list ->
            CartUiState(
                items = list,
                grandTotal = list.sumOf { it.lineTotal }
            )
        },
        deliveryState
    ) { cart, delivery ->
        cart.copy(
            addressId = delivery.addressId,
            addressLabel = delivery.addressLabel,
            addressText = delivery.addressText,
            addressDetails = delivery.addressDetails,
            estimatedDeliveryTime = delivery.estimatedDeliveryTime,
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
                addresses.firstOrNull()?.let { addr ->
                    _deliveryState.update {
                        it.copy(
                            addressId = addr.id,
                            addressLabel = addr.label,
                            addressText = addr.address.toAddressWithColonyOnly(),
                            addressDetails = addr.description?.takeIf { d -> d.isNotBlank() }
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
