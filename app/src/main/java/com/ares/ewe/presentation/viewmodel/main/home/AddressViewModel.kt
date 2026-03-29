package com.ares.ewe.presentation.viewmodel.main.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe.core.network.toUserFacingMessage
import com.ares.ewe.domain.model.UserAddress
import com.ares.ewe.domain.repository.PlacesAutocompleteRepository
import com.ares.ewe.domain.repository.UserAddressRepository
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddressSearchResult(
    val id: String,
    val title: String,
    val subtitle: String? = null
)

data class NavigateToMapData(
    val latLng: LatLng,
    val addressLabel: String
)

data class AddressUiState(
    val searchQuery: String = "",
    val searchResults: List<AddressSearchResult> = emptyList(),
    val myAddresses: List<UserAddress> = emptyList(),
    val showMyAddressesSheet: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val navigateToMapWithLocation: NavigateToMapData? = null,
    val navigateBackToHome: Boolean = false,
    val isLoadingPlaceDetails: Boolean = false
)

private const val DEBOUNCE_MS = 350L
private const val MIN_QUERY_LENGTH = 2

@HiltViewModel
class AddressViewModel @Inject constructor(
    private val placesAutocompleteRepository: PlacesAutocompleteRepository,
    private val userAddressRepository: UserAddressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddressUiState())
    val uiState: StateFlow<AddressUiState> = _uiState.asStateFlow()

    private var autocompleteJob: Job? = null

    fun onMyAddressesClick() {
        viewModelScope.launch {
            userAddressRepository.getAddresses()
                .onSuccess { list ->
                    _uiState.update {
                        it.copy(myAddresses = list, showMyAddressesSheet = true)
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            showMyAddressesSheet = true,
                            myAddresses = emptyList(),
                            errorMessage = e.toUserFacingMessage()
                        )
                    }
                }
        }
    }

    fun onDismissMyAddressesSheet() {
        _uiState.update { it.copy(showMyAddressesSheet = false) }
    }

    fun onMyAddressSelected(address: UserAddress) {
        viewModelScope.launch {
            _uiState.update { it.copy(showMyAddressesSheet = false) }
            userAddressRepository.setDefaultAddress(address.id)
                .onSuccess {
                    _uiState.update { it.copy(navigateBackToHome = true) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            showMyAddressesSheet = true,
                            errorMessage = e.toUserFacingMessage()
                        )
                    }
                }
        }
    }

    fun onNavigatedBackToHome() {
        _uiState.update { it.copy(navigateBackToHome = false) }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update {
            it.copy(
                searchQuery = query,
                errorMessage = null,
                searchResults = if (query.length < MIN_QUERY_LENGTH) emptyList() else it.searchResults
            )
        }
        if (query.length < MIN_QUERY_LENGTH) {
            autocompleteJob?.cancel()
            autocompleteJob = null
            _uiState.update { it.copy(isLoading = false) }
            return
        }
        autocompleteJob?.cancel()
        autocompleteJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            val currentQuery = _uiState.value.searchQuery
            if (currentQuery.length < MIN_QUERY_LENGTH) return@launch
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            placesAutocompleteRepository.getAddressPredictions(currentQuery)
                .onSuccess { predictions ->
                    val results = predictions.map { p ->
                        AddressSearchResult(id = p.placeId, title = p.mainText, subtitle = p.secondaryText)
                    }
                    _uiState.update {
                        it.copy(searchResults = results, isLoading = false, errorMessage = null)
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            searchResults = emptyList(),
                            isLoading = false,
                            errorMessage = e.toUserFacingMessage()
                        )
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onAddressClick(placeId: String, addressLabel: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPlaceDetails = true, errorMessage = null) }
            placesAutocompleteRepository.getPlaceLocation(placeId)
                .onSuccess { latLng ->
                    _uiState.update {
                        it.copy(
                            navigateToMapWithLocation = NavigateToMapData(latLng, addressLabel),
                            isLoadingPlaceDetails = false
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoadingPlaceDetails = false,
                            errorMessage = e.toUserFacingMessage()
                        )
                    }
                }
        }
    }

    fun onNavigatedToMap() {
        _uiState.update { it.copy(navigateToMapWithLocation = null) }
    }

    fun onCurrentLocationClick() {
        // Handled by navigation to MapLocation without lat/lng
    }
}
