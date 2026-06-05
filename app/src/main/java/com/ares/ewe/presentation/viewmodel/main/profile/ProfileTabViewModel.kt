package com.ares.ewe.presentation.viewmodel.main.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe.domain.repository.FavoritesRepository
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
    val levelKey: String = "EXPLORADOR",
    val levelName: String = "",
    val xpInLevelProgress: Float = 0f,
    val xpToNextLabel: String? = null,
    val orderStreakDays: Int = 0,
    val totalOrdersDelivered: Int = 0,
    val favoritesCount: Int = 0,
    val recentEvents: List<ProfileRecentEvent> = emptyList(),
) {
    val levelNumber: Int
        get() = consumerLevelNumber(levelKey)

    val badgesUnlockedCount: Int
        get() = listOf(
            totalOrdersDelivered >= 1,
            totalOrdersDelivered >= 3,
            orderStreakDays >= 1,
        ).count { it }
}

data class ProfileRecentEvent(
    val label: String,
    val delta: Int,
    val timeAgo: String,
)

@HiltViewModel
class ProfileTabViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val favoritesRepository: FavoritesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            favoritesRepository.favorites.collect { list ->
                _uiState.update { it.copy(favoritesCount = list.size) }
            }
        }
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
                            levelKey = g.levelKey,
                            levelName = g.levelName,
                            xpInLevelProgress = progress,
                            xpToNextLabel = xpToNext?.let { xp -> "$xp XP para el siguiente nivel" },
                            orderStreakDays = g.orderStreakDays,
                            totalOrdersDelivered = g.totalOrdersDelivered,
                            recentEvents = g.recentEvents.map { e ->
                                ProfileRecentEvent(
                                    label = reasonLabelEs(e.reason),
                                    delta = e.delta,
                                    timeAgo = formatTimeAgoEs(e.createdAt),
                                )
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
        "purchase" -> "Compra completada"
        "first_order" -> "Primer pedido"
        "peak_hour" -> "Hora pico"
        "order_streak" -> "Racha de pedidos"
        "rate_delivery" -> "Valoraste tu entrega"
        else -> reason
    }
}

fun consumerLevelNumber(levelKey: String): Int {
    val order = listOf("EXPLORADOR", "FRECUENTE", "FAN", "VIP", "DOBBY_MASTER")
    val idx = order.indexOf(levelKey.uppercase())
    return if (idx < 0) 1 else idx + 1
}

private fun formatTimeAgoEs(iso: String): String {
    val instant = runCatching {
        java.time.Instant.parse(iso)
    }.getOrNull() ?: return "Reciente"
    val minutes = java.time.Duration.between(instant, java.time.Instant.now()).toMinutes()
    return when {
        minutes < 1 -> "Hace un momento"
        minutes < 60 -> "Hace $minutes min"
        minutes < 60 * 24 -> "Hace ${minutes / 60} h"
        minutes < 60 * 24 * 2 -> "Hace 1 día"
        minutes < 60 * 24 * 7 -> "Hace ${minutes / (60 * 24)} días"
        else -> "Hace más de una semana"
    }
}
