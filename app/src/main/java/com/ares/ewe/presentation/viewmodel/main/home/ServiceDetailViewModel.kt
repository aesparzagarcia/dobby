package com.ares.ewe.presentation.viewmodel.main.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe.core.network.toUserFacingMessage
import com.ares.ewe.domain.cart.PendingCartAddGate
import com.ares.ewe.domain.model.ServiceDetail
import com.ares.ewe.domain.repository.CartRepository
import com.ares.ewe.domain.repository.PlacesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ServiceDetailUiState(
    val service: ServiceDetail? = null,
    val serviceNumber: String = "",
    val amountToPay: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val payError: String? = null,
) {
    val canPay: Boolean
        get() {
            if (serviceNumber.isBlank()) return false
            val amount = amountToPay.replace(",", ".").toDoubleOrNull() ?: return false
            return amount > 0.0
        }
}

@HiltViewModel
class ServiceDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val placesRepository: PlacesRepository,
    private val cartRepository: CartRepository,
    private val pendingCartAddGate: PendingCartAddGate,
) : ViewModel() {

    private val serviceId: String = checkNotNull(savedStateHandle.get<String>("id"))

    private val _uiState = MutableStateFlow(ServiceDetailUiState())
    val uiState: StateFlow<ServiceDetailUiState> = _uiState.asStateFlow()

    val cartItemCount: StateFlow<Int> = cartRepository.items
        .map { it.sumOf { item -> item.quantity } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        loadService()
    }

    fun loadService() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val service = placesRepository.getService(serviceId)
                _uiState.update {
                    it.copy(
                        service = service,
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

    fun onServiceNumberChange(value: String) {
        _uiState.update { it.copy(serviceNumber = value, payError = null) }
    }

    fun onAmountChange(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' || it == ',' }
        _uiState.update { it.copy(amountToPay = filtered, payError = null) }
    }

    fun payService(onAdded: () -> Unit) {
        val state = _uiState.value
        val service = state.service ?: return
        if (!state.canPay) return
        val amount = state.amountToPay.replace(",", ".").toDoubleOrNull() ?: return
        val number = state.serviceNumber.trim()
        if (number.isEmpty()) return
        val lat = service.lat
        val lng = service.lng
        if (lat == null || lng == null) {
            _uiState.update {
                it.copy(payError = "Este servicio no tiene ubicación configurada. Intenta más tarde.")
            }
            return
        }
        viewModelScope.launch {
            pendingCartAddGate.runOrRequestAddress {
                cartRepository.addServiceItem(
                    serviceId = service.id,
                    serviceName = service.name,
                    serviceNumber = number,
                    amount = amount,
                    imageUrl = service.imageUrl,
                    pickupLatitude = lat,
                    pickupLongitude = lng,
                )
                onAdded()
            }
        }
    }
}
