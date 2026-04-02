package com.ares.ewe.presentation.ui.auth.phone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe.domain.model.AuthResult
import com.ares.ewe.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val MX_NATIONAL_LENGTH = 10

data class PhoneUiState(
    /** National digits only (no country code), 10 digits for Mexico. */
    val nationalDigits: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class PhoneViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhoneUiState())
    val uiState: StateFlow<PhoneUiState> = _uiState.asStateFlow()

    fun onPhoneChange(raw: String) {
        val digits = raw.filter { it.isDigit() }.take(MX_NATIONAL_LENGTH)
        _uiState.update { it.copy(nationalDigits = digits, errorMessage = null) }
    }

    fun sendCode(onResult: (phone: String, userExists: Boolean) -> Unit) {
        viewModelScope.launch {
            val phone = _uiState.value.nationalDigits
            if (phone.length < MX_NATIONAL_LENGTH) {
                _uiState.update {
                    it.copy(errorMessage = "Enter a 10-digit phone number")
                }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.requestOtp(phone)) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                    onResult(phone, result.data.userExists)
                }
                is AuthResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
