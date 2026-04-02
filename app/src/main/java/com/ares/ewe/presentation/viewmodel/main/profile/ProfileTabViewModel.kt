package com.ares.ewe.presentation.viewmodel.main.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val displayName: String = "",
    val email: String = "",
    val phone: String? = null,
    val avatarLetter: String = "?",
    val dobbyXp: Int = 0,
    val levelName: String = "",
    val xpInLevelProgress: Float = 0f,
    val xpToNextLabel: String? = null,
    val orderStreakDays: Int = 0,
    val totalOrdersDelivered: Int = 0,
    val recentEvents: List<Pair<String, Int>> = emptyList(),
)

@HiltViewModel
class ProfileTabViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            profileRepository.getGamification()
                .onSuccess { g ->
                    val next = g.xpForNextLevel
                    val start = g.xpAtLevelStart
                    val current = g.dobbyXp
                    val progress = if (next != null && next > start) {
                        ((current - start).toFloat() / (next - start)).coerceIn(0f, 1f)
                    } else {
                        1f
                    }
                    val xpToNext = if (next != null) (next - current).coerceAtLeast(0) else null
                    val fullName = listOfNotNull(g.name?.trim()?.takeIf { it.isNotEmpty() }, g.lastName?.trim()?.takeIf { it.isNotEmpty() })
                        .joinToString(" ")
                    val display = fullName.ifBlank {
                        g.email.substringBefore("@").ifBlank { "Usuario" }
                    }
                    val initial = display.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = null,
                            displayName = display,
                            email = g.email,
                            phone = g.phone?.takeIf { p -> p.isNotBlank() },
                            avatarLetter = initial,
                            dobbyXp = current,
                            levelName = g.levelName,
                            xpInLevelProgress = progress,
                            xpToNextLabel = xpToNext?.let { xp -> "$xp XP al siguiente nivel" },
                            orderStreakDays = g.orderStreakDays,
                            totalOrdersDelivered = g.totalOrdersDelivered,
                            recentEvents = g.recentEvents.map { e ->
                                reasonLabelEs(e.reason) to e.delta
                            },
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "No se pudo cargar",
                        )
                    }
                }
        }
    }

    private fun reasonLabelEs(reason: String): String = when (reason) {
        "purchase" -> "Compra"
        "first_order" -> "Primer pedido"
        "peak_hour" -> "Hora pico"
        "order_streak" -> "Racha de pedidos"
        "rate_delivery" -> "Valorar reparto"
        else -> reason
    }
}
