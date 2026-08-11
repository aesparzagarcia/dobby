package com.ares.ewe.presentation.viewmodel.main.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe.core.network.toUserFacingMessage
import com.ares.ewe.domain.model.FeaturedPlace
import com.ares.ewe.domain.repository.PlacesRepository
import com.ares.ewe.presentation.ui.main.home.HomeQuickCategory
import com.ares.ewe.presentation.ui.main.home.filterPlacesByCategory
import com.ares.ewe.presentation.ui.main.home.sortFeaturedPlacesByAvailability
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeaturedPlacesUiState(
    val places: List<FeaturedPlace> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: HomeQuickCategory = HomeQuickCategory.All,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
) {
    val filteredPlaces: List<FeaturedPlace>
        get() {
            val byCategory = filterPlacesByCategory(places, selectedCategory)
            val query = searchQuery.trim()
            return if (query.isBlank()) {
                byCategory
            } else {
                byCategory.filter { it.name.contains(query, ignoreCase = true) }
            }
        }
}

@HiltViewModel
class FeaturedPlacesViewModel @Inject constructor(
    private val placesRepository: PlacesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeaturedPlacesUiState(isLoading = true))
    val uiState: StateFlow<FeaturedPlacesUiState> = _uiState.asStateFlow()

    init {
        loadFeaturedPlaces()
    }

    fun loadFeaturedPlaces() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val places = placesRepository.getFeaturedPlaces()
                _uiState.update {
                    it.copy(
                        places = sortFeaturedPlacesByAvailability(places),
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = null,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = e.toUserFacingMessage(),
                    )
                }
            }
        }
    }

    fun refresh() {
        if (_uiState.value.isRefreshing || _uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            try {
                val places = placesRepository.getFeaturedPlaces()
                _uiState.update {
                    it.copy(
                        places = sortFeaturedPlacesByAvailability(places),
                        isRefreshing = false,
                        errorMessage = null,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        errorMessage = e.toUserFacingMessage(),
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onCategorySelected(category: HomeQuickCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
    }
}
