package com.ares.ewe.presentation.viewmodel.auth.otp

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe.domain.model.AuthResult
import com.ares.ewe.domain.model.VerifyOtpOutcome
import com.ares.ewe.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OtpUiState(
    val code: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class OtpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val phone: String = savedStateHandle.get<String>("phone").orEmpty()

    private val _uiState = MutableStateFlow(OtpUiState())
    val uiState: StateFlow<OtpUiState> = _uiState.asStateFlow()

    fun onCodeChange(code: String) {
        val digitsOnly = code.filter { it.isDigit() }.take(6)
        _uiState.update { it.copy(code = digitsOnly, errorMessage = null) }
    }

    fun verifyCode(onLoggedIn: () -> Unit, onRequiresRegistration: () -> Unit) {
        viewModelScope.launch {
            val code = _uiState.value.code
            if (code.length < 4) {
                _uiState.update { it.copy(errorMessage = "Enter the code you received") }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.verifyOtp(phone, code)) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                    when (result.data) {
                        is VerifyOtpOutcome.LoggedIn -> onLoggedIn()
                        is VerifyOtpOutcome.RequiresRegistration -> onRequiresRegistration()
                    }
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
