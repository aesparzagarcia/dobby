package com.ares.ewe.presentation.viewmodel.main.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe.core.network.toUserFacingMessage
import com.ares.ewe.domain.model.OrderTracking
import com.ares.ewe.domain.repository.DirectionsRepository
import com.ares.ewe.domain.repository.OrderRepository
import com.ares.ewe.push.ConsumerOrderRealtimeBus
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrderTrackingUiState(
    val tracking: OrderTracking? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val rateSubmitting: Boolean = false,
    val rateError: String? = null,
    val draftShopStars: Int? = null,
    val draftDeliveryStars: Int? = null,
    val draftProductStars: Map<String, Int> = emptyMap(),
    /** Driving route from repartidor to delivery address (Google Directions), or straight segment if API fails. */
    val routePoints: List<LatLng> = emptyList(),
    /** True when Directions did not return a street polyline (key/API issue or empty result). */
    val usingStraightLineRoute: Boolean = false
)

private const val LOCATION_POLL_INTERVAL_MS = 3_000L
private const val DELIVERY_CODE_POLL_INTERVAL_MS = 1_000L
/** Avoid Directions API burst: refresh route at most this often while position updates. */
private const val ROUTE_MIN_INTERVAL_MS = 20_000L

@HiltViewModel
class OrderTrackingViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val directionsRepository: DirectionsRepository,
    private val orderRealtimeBus: ConsumerOrderRealtimeBus,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val orderId: String = savedStateHandle.get<String>("orderId").orEmpty()

    private val _uiState = MutableStateFlow(OrderTrackingUiState())
    val uiState: StateFlow<OrderTrackingUiState> = _uiState.asStateFlow()

    private var lastDmLat: Double? = null
    private var lastDmLng: Double? = null
    private var lastRouteFetchAt = 0L
    private var lastRouteStatus: String? = null

    init {
        loadTracking()
        startLocationRefreshPolling()
        viewModelScope.launch {
            orderRealtimeBus.events.collect { event ->
                if (event.orderId == null || event.orderId == orderId) {
                    loadTracking()
                }
            }
        }
    }

    /** When order is in progress, refresh tracking periodically so delivery man position updates. */
    private fun startLocationRefreshPolling() {
        viewModelScope.launch {
            while (isActive) {
                val current = _uiState.value.tracking
                val awaitingDeliveryCode =
                    current?.courierArrivedAtCustomer == true &&
                        (current.isOnDelivery || current.isOutForPickup) &&
                        current.deliveryCode.isNullOrBlank()
                val interval = if (awaitingDeliveryCode) {
                    DELIVERY_CODE_POLL_INTERVAL_MS
                } else {
                    LOCATION_POLL_INTERVAL_MS
                }
                delay(interval)
                val status = _uiState.value.tracking?.status?.uppercase()
                if (status == "ASSIGNED" || status == "ON_DELIVERY" || status == "OUT_FOR_PICKUP" || status == "PICKED_UP") {
                    orderRepository.getOrderTracking(orderId).onSuccess { tracking ->
                        if (tracking != null) {
                            onTrackingRefreshed(tracking)
                        }
                    }
                }
            }
        }
    }

    private fun onTrackingRefreshed(tracking: OrderTracking) {
        val dm = tracking.deliveryMan
        val lat = dm?.lat
        val lng = dm?.lng
        if (lat == null || lng == null) {
            lastDmLat = null
            lastDmLng = null
            _uiState.value = _uiState.value.copy(tracking = tracking)
            maybeRefreshRoute(tracking)
            return
        }
        val moved = lastDmLat == null || lastDmLng == null ||
            abs(lat - lastDmLat!!) > 1e-5 || abs(lng - lastDmLng!!) > 1e-5
        if (moved) {
            lastDmLat = lat
            lastDmLng = lng
            _uiState.value = _uiState.value.copy(tracking = tracking)
            maybeRefreshRoute(tracking)
            return
        }
        _uiState.value = _uiState.value.copy(tracking = tracking)
        maybeRefreshRoute(tracking)
    }

    private fun maybeRefreshRoute(tracking: OrderTracking) {
        // Dobby no dibuja la ruta en el mapa de seguimiento.
        if (_uiState.value.routePoints.isNotEmpty() || _uiState.value.usingStraightLineRoute) {
            _uiState.value = _uiState.value.copy(
                routePoints = emptyList(),
                usingStraightLineRoute = false,
            )
        }
    }

    fun loadTracking() {
        if (orderId.isBlank()) {
            _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Solicitud: falta el identificador del pedido")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            orderRepository.getOrderTracking(orderId)
                .onSuccess { tracking ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = if (tracking == null) "No encontrado: el pedido no existe o no tienes acceso" else null,
                        tracking = tracking
                    )
                    if (tracking != null) {
                        onTrackingRefreshed(tracking)
                    }
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.toUserFacingMessage()
                    )
                }
        }
    }

    fun selectDeliveryRating(stars: Int) {
        if (stars !in 1..5) return
        _uiState.value = _uiState.value.copy(draftDeliveryStars = stars, rateError = null)
    }

    fun selectShopRating(stars: Int) {
        if (stars !in 1..5) return
        _uiState.value = _uiState.value.copy(draftShopStars = stars, rateError = null)
    }

    fun selectProductRating(productId: String, stars: Int) {
        if (stars !in 1..5 || productId.isBlank()) return
        _uiState.value = _uiState.value.copy(
            draftProductStars = _uiState.value.draftProductStars + (productId to stars),
            rateError = null,
        )
    }

    /** Guarda las estrellas elegidas y luego cierra el seguimiento. */
    fun finish(onDone: () -> Unit) {
        val state = _uiState.value
        val tracking = state.tracking ?: return
        if (!tracking.isDelivered || state.rateSubmitting || orderId.isBlank()) return
        if (!tracking.hasPendingRatings) {
            onDone()
            return
        }
        if (!draftsComplete(tracking, state)) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(rateSubmitting = true, rateError = null)
            if (tracking.canRateShop) {
                val stars = _uiState.value.draftShopStars ?: run {
                    failRating(null)
                    return@launch
                }
                val result = orderRepository.rateShop(orderId, stars)
                if (result.isFailure && !result.isAlreadyRated()) {
                    failRating(result.exceptionOrNull())
                    return@launch
                }
            }
            if (tracking.canRateDelivery) {
                val stars = _uiState.value.draftDeliveryStars ?: run {
                    failRating(null)
                    return@launch
                }
                val result = orderRepository.rateDelivery(orderId, stars)
                if (result.isFailure && !result.isAlreadyRated()) {
                    failRating(result.exceptionOrNull())
                    return@launch
                }
            }
            val products = tracking.items.mapNotNull { item ->
                if (!item.canRate) return@mapNotNull null
                val stars = _uiState.value.draftProductStars[item.productId] ?: run {
                    failRating(null)
                    return@launch
                }
                item.productId to stars
            }
            if (products.isNotEmpty()) {
                val result = orderRepository.rateProducts(orderId, products)
                if (result.isFailure) {
                    failRating(result.exceptionOrNull())
                    return@launch
                }
            }
            _uiState.value = _uiState.value.copy(rateSubmitting = false, rateError = null)
            onDone()
        }
    }

    private fun failRating(error: Throwable?) {
        _uiState.value = _uiState.value.copy(
            rateSubmitting = false,
            rateError = error?.toUserFacingMessage() ?: "No se pudo guardar la valoración",
        )
    }

    private fun draftsComplete(tracking: OrderTracking, state: OrderTrackingUiState): Boolean {
        if (tracking.canRateShop && state.draftShopStars !in 1..5) return false
        if (tracking.canRateDelivery && state.draftDeliveryStars !in 1..5) return false
        return tracking.items.none { it.canRate && state.draftProductStars[it.productId] !in 1..5 }
    }

    fun clearRateError() {
        _uiState.value = _uiState.value.copy(rateError = null)
    }
}

private fun Result<Unit>.isAlreadyRated(): Boolean =
    exceptionOrNull()?.message?.contains("ya valoraste", ignoreCase = true) == true
