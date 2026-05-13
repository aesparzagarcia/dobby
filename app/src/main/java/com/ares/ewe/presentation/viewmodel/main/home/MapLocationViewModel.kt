package com.ares.ewe.presentation.viewmodel.main.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe.core.location.DeliveryServiceArea
import com.ares.ewe.core.network.toUserFacingMessage
import com.ares.ewe.data.location.FusedLocationProvider
import com.ares.ewe.domain.repository.PlacesAutocompleteRepository
import com.ares.ewe.domain.repository.UserAddressRepository
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class MapLocationUiState(
    val currentLocation: LatLng? = null,
    val userStartLocation: LatLng? = null,
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
    private val deliveryServiceArea: DeliveryServiceArea,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapLocationUiState())
    val uiState: StateFlow<MapLocationUiState> = _uiState.asStateFlow()

    private var reverseGeocodeJob: Job? = null

    init {
        val lat = savedStateHandle.get<String>("lat")?.toDoubleOrNull()
        val lng = savedStateHandle.get<String>("lng")?.toDoubleOrNull()
        val address = savedStateHandle.get<String>("address") ?: ""
        if (lat != null && lng != null) {
            _uiState.update {
                it.copy(
                    currentLocation = LatLng(lat, lng),
                    userStartLocation = LatLng(lat, lng),
                    isLoading = false,
                    isChosenAddress = true,
                    chosenAddressLabel = address,
                    editableAddress = address
                )
            }
            if (address.isBlank()) {
                requestAddressFor(LatLng(lat, lng), updateLatLngInState = false)
            }
        }
    }

    fun onAddressChange(text: String) {
        _uiState.update { it.copy(editableAddress = text) }
    }

    fun onMapCenterChanged(latLng: LatLng) {
        requestAddressFor(latLng, updateLatLngInState = true)
    }

    /**
     * Cancels any in-flight reverse geocode so rapid map drags only apply the latest center.
     * On API failure, [MapLocationUiState.editableAddress] falls back to coordinates so the user
     * can still save (GMS map timeouts are unrelated to our HTTP Geocoding call but often coincide).
     */
    private fun requestAddressFor(latLng: LatLng, updateLatLngInState: Boolean) {
        reverseGeocodeJob?.cancel()
        reverseGeocodeJob = viewModelScope.launch {
            if (updateLatLngInState) {
                _uiState.update {
                    it.copy(
                        currentLocation = latLng,
                        isReverseGeocoding = true,
                        errorMessage = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(isReverseGeocoding = true, errorMessage = null)
                }
            }
            val lat = latLng.latitude
            val lng = latLng.longitude
            val coordFallback = String.format(Locale.US, "%.5f, %.5f", lat, lng)
            val result = placesRepository.getAddressFromLocation(lat, lng)
            if (!isActive) return@launch
            result
                .onSuccess { address ->
                    _uiState.update {
                        it.copy(
                            editableAddress = address.ifBlank { coordFallback },
                            isReverseGeocoding = false
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isReverseGeocoding = false,
                            errorMessage = e.toUserFacingMessage(),
                            editableAddress = coordFallback
                        )
                    }
                }
        }
    }

    fun onSaveAddressClick(mapCenter: LatLng) {
        if (!deliveryServiceArea.contains(mapCenter.latitude, mapCenter.longitude)) {
            _uiState.update { it.copy(errorMessage = null) }
            return
        }
        _uiState.update { it.copy(showDescriptionDialog = true, errorMessage = null) }
    }

    fun onDismissDescriptionDialog() {
        _uiState.update { it.copy(showDescriptionDialog = false) }
    }

    fun saveAddressWithDescription(label: String, description: String?, latLng: LatLng, addressText: String) {
        viewModelScope.launch {
            if (!deliveryServiceArea.contains(latLng.latitude, latLng.longitude)) {
                _uiState.update {
                    it.copy(
                        showDescriptionDialog = false,
                        errorMessage = if (deliveryServiceArea.isConfigBlockingSaves()) {
                            deliveryServiceArea.denialMessage()
                        } else {
                            null
                        }
                    )
                }
                return@launch
            }
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
                        it.copy(
                            currentLocation = latLng,
                            userStartLocation = latLng,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                    requestAddressFor(latLng, updateLatLngInState = false)
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

    fun hasValidServiceAreaPolygon(): Boolean = deliveryServiceArea.hasValidEnforcedPolygon()

    fun isServiceAreaConfigBlocking(): Boolean = deliveryServiceArea.isConfigBlockingSaves()

    fun isInsideServiceArea(latLng: LatLng): Boolean =
        deliveryServiceArea.contains(latLng.latitude, latLng.longitude)
}
