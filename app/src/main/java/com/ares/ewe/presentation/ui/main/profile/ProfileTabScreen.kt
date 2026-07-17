package com.ares.ewe.presentation.ui.main.profile

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.ares.ewe.core.theme.DobbyColors
import com.ares.ewe.presentation.components.MainTabContentBottomInset
import com.ares.ewe.presentation.viewmodel.main.profile.ProfileTabViewModel

private const val PROFILE_SUPPORT_URL = "https://dobby-frontend-wwru.onrender.com/"

@Composable
fun ProfileTabScreen(
    onLogout: () -> Unit,
    onOrdersClick: () -> Unit = {},
    onGoHome: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ProfileTabViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val scroll = rememberScrollState()
    val context = LocalContext.current

    val openNotificationSettings = {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData("package:${context.packageName}".toUri())
        }
        runCatching { context.startActivity(intent) }
        Unit
    }

    val openSupport = {
        val intent = Intent(Intent.ACTION_VIEW, PROFILE_SUPPORT_URL.toUri())
        runCatching { context.startActivity(intent) }
        Unit
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DobbyColors.Light)
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Perfil",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = DobbyColors.Dark,
            )
            IconButton(onClick = openNotificationSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Ajustes",
                    tint = DobbyColors.Dark,
                )
            }
        }

        Text(
            text = "Sigue acumulando XP y desbloquea recompensas increíbles 🎉",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6B7280),
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )

        when {
            uiState.isLoading -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(color = DobbyColors.Primary)
                }
            }
            uiState.error != null -> {
                Text(
                    text = uiState.error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { viewModel.refresh() }) {
                    Text("Reintentar")
                }
            }
            else -> {
                ProfileHeroCard(uiState = uiState)

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ProfileQuickStatCard(
                        title = "Racha",
                        value = streakValueLabel(uiState.orderStreakDays),
                        icon = Icons.Default.LocalFireDepartment,
                        iconTint = Color(0xFFFF8A3D),
                        iconBackground = Color(0xFFFFF7ED),
                        modifier = Modifier.weight(1f),
                    )
                    ProfileQuickStatCard(
                        title = "Pedidos",
                        value = ordersValueLabel(uiState.totalOrdersDelivered),
                        icon = Icons.Default.ShoppingBag,
                        iconTint = DobbyColors.Primary,
                        iconBackground = DobbyColors.Light,
                        modifier = Modifier.weight(1f),
                        onClick = onOrdersClick,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ProfileQuickStatCard(
                        title = "Favoritos",
                        value = "${uiState.favoritesCount} favoritos",
                        icon = Icons.Default.Favorite,
                        iconTint = Color(0xFFEF4444),
                        iconBackground = Color(0xFFFFF1F2),
                        modifier = Modifier.weight(1f),
                    )
                    ProfileQuickStatCard(
                        title = "Insignias",
                        value = "${uiState.badgesUnlockedCount} insignias",
                        icon = Icons.Default.Star,
                        iconTint = Color(0xFF8B5CF6),
                        iconBackground = Color(0xFFF3E8FF),
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                ProfileSectionHeader(title = "Misiones de hoy")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        profileTodayMissions.forEachIndexed { index, mission ->
                            ProfileMissionRow(
                                mission = mission,
                                modifier = Modifier.clickable(onClick = onGoHome),
                            )
                            if (index < profileTodayMissions.lastIndex) {
                                ProfileMenuDivider()
                            }
                        }
                    }
                }

                if (uiState.recentEvents.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    ProfileSectionHeader(title = "Actividad reciente")
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            uiState.recentEvents.forEach { event ->
                                val (icon, tint, bg) = activityIconFor(event.label)
                                ProfileActivityRow(
                                    label = event.label,
                                    delta = event.delta,
                                    timeAgo = event.timeAgo,
                                    icon = icon,
                                    iconTint = tint,
                                    iconBackground = bg,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                ProfileSectionHeader(title = "Tus insignias")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    profileBadgesFor(uiState).forEach { badge ->
                        ProfileBadgeChip(badge = badge)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                ProfileMenuCard(modifier = Modifier.padding(top = 4.dp)) {
                    ProfileMenuRow(
                        title = "Notificaciones",
                        icon = Icons.Default.Notifications,
                        onClick = openNotificationSettings,
                    )
                    ProfileMenuDivider()
                    ProfileMenuRow(
                        title = "Ayuda y soporte",
                        icon = Icons.Default.Help,
                        onClick = openSupport,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF2F2F7),
                contentColor = Color(0xFFEF4444),
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        ) {
            Text(
                text = "Cerrar sesión",
                fontWeight = FontWeight.SemiBold,
            )
        }

        // Scrollable tail inset — bottom padding on the modifier does not extend scroll range.
        Spacer(modifier = Modifier.height(MainTabContentBottomInset + 24.dp))
    }
}

private fun streakValueLabel(days: Int): String = when (days) {
    1 -> "1 día"
    else -> "$days días"
}

private fun ordersValueLabel(count: Int): String = when (count) {
    1 -> "1 pedido"
    else -> "$count pedidos"
}
