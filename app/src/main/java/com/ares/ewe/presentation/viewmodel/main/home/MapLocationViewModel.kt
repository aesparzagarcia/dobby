package com.ares.ewe.presentation.viewmodel.main.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe.core.network.toUserFacingMessage
import com.ares.ewe.data.location.FusedLocationProvider
import com.ares.ewe.domain.repository.PlacesAutocompleteRepository
import com.ares.ewe.domain.repository.UserAddressRepository
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapLocationUiState(
    val currentLocation: LatLng? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val permissionGranted: Boolean = false,
    val isChosenAddress: Boolean = false,
    val chosenAddressLabel: String = "",
    val editableAddress: String = "",
    val isReverseGeocoding: Boolean = false,
    val addressSaved: Boolean = false,
    val showDescriptionDialog: Boolean = false
)

@HiltViewModel
class MapLocationViewModel @Inject constructor(
    private val locationProvider: FusedLocationProvider,
    private val placesRepository: PlacesAutocompleteRepository,
    private val userAddressRepository: UserAddressRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapLocationUiState())
    val uiState: StateFlow<MapLocationUiState> = _uiState.asStateFlow()

    init {
        val lat = savedStateHandle.get<String>("lat")?.toDoubleOrNull()
        val lng = savedStateHandle.get<String>("lng")?.toDoubleOrNull()
        val address = savedStateHandle.get<String>("address") ?: ""
        if (lat != null && lng != null) {
            _uiState.update {
                it.copy(
                    currentLocation = LatLng(lat, lng),
                    isLoading = false,
                    isChosenAddress = true,
                    chosenAddressLabel = address,
                    editableAddress = address
                )
            }
        }
    }

    fun onAddressChange(text: String) {
        _uiState.update { it.copy(editableAddress = text) }
    }

    fun onMapCenterChanged(latLng: LatLng) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    currentLocation = latLng,
                    isReverseGeocoding = true,
                    errorMessage = null
                )
            }
            placesRepository.getAddressFromLocation(latLng.latitude, latLng.longitude)
                .onSuccess { address ->
                    _uiState.update {
                        it.copy(
                            editableAddress = address,
                            isReverseGeocoding = false
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isReverseGeocoding = false,
                            errorMessage = e.toUserFacingMessage()
                        )
                    }
                }
        }
    }

    fun onSaveAddressClick() {
        _uiState.update { it.copy(showDescriptionDialog = true, errorMessage = null) }
    }

    fun onDismissDescriptionDialog() {
        _uiState.update { it.copy(showDescriptionDialog = false) }
    }

    fun saveAddressWithDescription(label: String, description: String?, latLng: LatLng, addressText: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(showDescriptionDialog = false) }
            val labelStr = label.ifBlank { "Casa" }
            val descStr = description?.trim()?.takeIf { it.isNotBlank() }
            val finalAddress = if (addressText.isNotBlank()) addressText else {
                placesRepository.getAddressFromLocation(latLng.latitude, latLng.longitude).getOrNull() ?: ""
            }
            userAddressRepository.createAddress(
                label = labelStr,
                description = descStr,
                address = finalAddress,
                lat = latLng.latitude,
                lng = latLng.longitude,
                isDefault = true
            )
                .onSuccess {
                    _uiState.update { it.copy(addressSaved = true) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(errorMessage = e.toUserFacingMessage())
                    }
                }
        }
    }

    fun clearAddressSaved() {
        _uiState.update { it.copy(addressSaved = false) }
    }

    fun onPermissionResult(granted: Boolean) {
        if (_uiState.value.isChosenAddress) return
        _uiState.update { it.copy(permissionGranted = granted) }
        if (granted) {
            fetchCurrentLocation()
        } else {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Ubicación: concede permiso para mostrar tu posición en el mapa."
                )
            }
        }
    }

    fun fetchCurrentLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            locationProvider.getLastLocation()
                .onSuccess { latLng ->
                    _uiState.update {
                        it.copy(currentLocation = latLng, isLoading = false, errorMessage = null)
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message?.takeIf { it.isNotBlank() }
                                ?: "Ubicación: no se pudo obtener. Comprueba que el GPS esté activado."
                        )
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
