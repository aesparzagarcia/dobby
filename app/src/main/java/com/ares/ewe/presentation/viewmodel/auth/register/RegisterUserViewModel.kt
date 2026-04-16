package com.ares.ewe.presentation.viewmodel.auth.register

import android.util.Patterns
import androidx.lifecycle.SavedStateHandle
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

data class AddUserInfoUiState(
    val name: String = "",
    val lastName: String = "",
    val phone: String = "",
    val email: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AddUserInfoViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val phone: String = savedStateHandle.get<String>("phone").orEmpty()

    private val _uiState = MutableStateFlow(AddUserInfoUiState(phone = phone))
    val uiState: StateFlow<AddUserInfoUiState> = _uiState.asStateFlow()

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, errorMessage = null) }
    }

    fun onLastNameChange(lastName: String) {
        _uiState.update { it.copy(lastName = lastName, errorMessage = null) }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun submit(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            when {
                state.name.isBlank() -> {
                    _uiState.update { it.copy(errorMessage = "El nombre es obligatorio") }
                    return@launch
                }
                state.lastName.isBlank() -> {
                    _uiState.update { it.copy(errorMessage = "Los apellidos son obligatorios") }
                    return@launch
                }
                state.email.isBlank() -> {
                    _uiState.update { it.copy(errorMessage = "El correo es obligatorio") }
                    return@launch
                }
                !Patterns.EMAIL_ADDRESS.matcher(state.email).matches() -> {
                    _uiState.update { it.copy(errorMessage = "Introduce un correo válido") }
                    return@launch
                }
            }
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.completeRegistration(
                phone = state.phone,
                name = state.name,
                lastName = state.lastName,
                email = state.email
            )) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                    onSuccess()
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
