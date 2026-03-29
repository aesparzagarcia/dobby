package com.ares.ewe.presentation.viewmodel.main.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val amountToPay: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ServiceDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val placesRepository: PlacesRepository,
    cartRepository: CartRepository
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
                        errorMessage = e.message ?: "Failed to load service"
                    )
                }
            }
        }
    }

    fun onAmountChange(value: String) {
        _uiState.update { it.copy(amountToPay = value) }
    }
}
