package com.ares.ewe.presentation.viewmodel.main.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe.domain.model.Ad
import com.ares.ewe.domain.repository.AdsRepository
import com.ares.ewe.domain.repository.CartRepository
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

data class AdDetailUiState(
    val ad: Ad? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class AdDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val adsRepository: AdsRepository,
    cartRepository: CartRepository
) : ViewModel() {

    private val adId: String = checkNotNull(savedStateHandle.get<String>("id"))

    private val _uiState = MutableStateFlow(AdDetailUiState())
    val uiState: StateFlow<AdDetailUiState> = _uiState.asStateFlow()

    val cartItemCount: StateFlow<Int> = cartRepository.items
        .map { it.sumOf { item -> item.quantity } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        loadAd()
    }

    private fun loadAd() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val ad = adsRepository.getAd(adId)
                _uiState.update {
                    it.copy(
                        ad = ad,
                        isLoading = false,
                        errorMessage = if (ad == null) "Ad not found" else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load"
                    )
                }
            }
        }
    }
}
