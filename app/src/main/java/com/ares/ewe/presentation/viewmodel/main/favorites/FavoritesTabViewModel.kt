package com.ares.ewe.presentation.viewmodel.main.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe.domain.model.FavoriteProduct
import com.ares.ewe.domain.repository.FavoritesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class FavoritesTabUiState(
    val favorites: List<FavoriteProduct> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class FavoritesTabViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesTabUiState())
    val uiState: StateFlow<FavoritesTabUiState> = _uiState.asStateFlow()

    init {
        observeFavorites()
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            favoritesRepository.favorites.collectLatest { list ->
                _uiState.value = FavoritesTabUiState(
                    favorites = list,
                    isLoading = false,
                )
            }
        }
    }
}
