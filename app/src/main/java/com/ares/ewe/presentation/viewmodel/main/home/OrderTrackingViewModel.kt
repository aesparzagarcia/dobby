package com.ares.ewe.presentation.viewmodel.main.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe.domain.model.OrderTracking
import com.ares.ewe.domain.repository.DirectionsRepository
import com.ares.ewe.domain.repository.OrderRepository
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
    /** Driving route from repartidor to delivery address (Google Directions), or straight segment if API fails. */
    val routePoints: List<LatLng> = emptyList(),
    /** True when Directions did not return a street polyline (key/API issue or empty result). */
    val usingStraightLineRoute: Boolean = false
)

private const val LOCATION_POLL_INTERVAL_MS = 3_000L
/** Avoid Directions API burst: refresh route at most this often while position updates. */
private const val ROUTE_MIN_INTERVAL_MS = 20_000L

@HiltViewModel
class OrderTrackingViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val directionsRepository: DirectionsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val orderId: String = savedStateHandle.get<String>("orderId").orEmpty()

    private val _uiState = MutableStateFlow(OrderTrackingUiState())
    val uiState: StateFlow<OrderTrackingUiState> = _uiState.asStateFlow()

    private var lastDmLat: Double? = null
    private var lastDmLng: Double? = null
    private var lastRouteFetchAt = 0L

    init {
        loadTracking()
        startLocationRefreshPolling()
    }

    /** When order is in progress, refresh tracking periodically so delivery man position updates. */
    private fun startLocationRefreshPolling() {
        viewModelScope.launch {
            while (isActive) {
                delay(LOCATION_POLL_INTERVAL_MS)
                val status = _uiState.value.tracking?.status
                if (status == "ASSIGNED" || status == "ON_DELIVERY") {
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
        val destLat = tracking.lat
        val destLng = tracking.lng
        val dm = tracking.deliveryMan
        val oLat = dm?.lat
        val oLng = dm?.lng
        if (destLat == null || destLng == null || oLat == null || oLng == null) {
            if (_uiState.value.routePoints.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(
                    routePoints = emptyList(),
                    usingStraightLineRoute = false
                )
            }
            return
        }
        val origin = LatLng(oLat, oLng)
        val dest = LatLng(destLat, destLng)
        val now = System.currentTimeMillis()
        val hasRoute = _uiState.value.routePoints.isNotEmpty()
        if (hasRoute && now - lastRouteFetchAt < ROUTE_MIN_INTERVAL_MS) return
        lastRouteFetchAt = now
        viewModelScope.launch {
            directionsRepository.getRoutePoints(origin, dest)
                .onSuccess { points ->
                    val route = if (points.isNotEmpty()) points else listOf(origin, dest)
                    _uiState.value = _uiState.value.copy(
                        routePoints = route,
                        usingStraightLineRoute = points.isEmpty()
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        routePoints = listOf(origin, dest),
                        usingStraightLineRoute = true
                    )
                }
        }
    }

    fun loadTracking() {
        if (orderId.isBlank()) {
            _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Order ID missing")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            orderRepository.getOrderTracking(orderId)
                .onSuccess { tracking ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = if (tracking == null) "Order not found" else null,
                        tracking = tracking
                    )
                    if (tracking != null) {
                        onTrackingRefreshed(tracking)
                    }
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Could not load order"
                    )
                }
        }
    }
}
