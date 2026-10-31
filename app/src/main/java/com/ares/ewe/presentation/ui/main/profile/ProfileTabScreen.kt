package com.ares.ewe.presentation.ui.main.profile

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Help
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.ares.ewe.BuildConfig
import com.ares.ewe.core.DevServerStore
import com.ares.ewe.core.theme.DobbyColors
import com.ares.ewe.presentation.components.MainTabContentBottomInset
import com.ares.ewe.presentation.viewmodel.main.profile.ProfileTabViewModel
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PROFILE_SUPPORT_URL = "https://dobby-frontend-wwru.onrender.com/soporte"

@Composable
fun ProfileTabScreen(
    onLogout: () -> Unit,
    onRequireLogin: () -> Unit = {},
    onOrdersClick: () -> Unit = {},
    onGoHome: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ProfileTabViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val scroll = rememberScrollState()
    val serverDialog = rememberDevServerDialogState()
    var serverCheck by remember { mutableIntStateOf(0) }
    LaunchedEffect(serverDialog.show, serverDialog.showRestartHint) {
        if (!serverDialog.show) serverCheck++
    }
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDeleteFinalConfirm by remember { mutableStateOf(false) }

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

    if (showDeleteConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminar cuenta") },
            text = {
                Text("Se eliminará tu cuenta y los datos personales asociados de forma permanente. Esta acción no se puede deshacer.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        showDeleteFinalConfirm = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                ) {
                    Text("Continuar")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            },
        )
    }

    if (showDeleteFinalConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteFinalConfirm = false },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Seguro que quieres eliminar tu cuenta de Dobbi?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteFinalConfirm = false
                        viewModel.deleteAccount(onDeleted = onLogout)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                ) {
                    Text("Eliminar definitivamente")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteFinalConfirm = false }) {
                    Text("Cancelar")
                }
            },
        )
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

        if (!uiState.isGuest) {
            Text(
                text = "Sigue acumulando XP y desbloquea recompensas increíbles 🎉",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )
        }

        when {
            uiState.isGuest -> {
                GuestProfileBody(
                    serverCheck = serverCheck,
                    onLogin = onRequireLogin,
                    onSupport = openSupport,
                    onServerConnection = if (BuildConfig.ENVIRONMENT == "dev") {
                        { serverDialog.open() }
                    } else {
                        null
                    },
                )
                if (BuildConfig.ENVIRONMENT == "dev") {
                    DevServerConnectionDialogs(serverDialog)
                }
            }
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
                if (com.ares.ewe.BuildConfig.ENVIRONMENT == "dev") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { serverDialog.open() }) {
                        Text("Configurar IP")
                    }
                    DevServerConnectionDialogs(serverDialog)
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
                        iconTint = Color(0xFF6B7280),
                        iconBackground = Color(0xFFF2F2F7),
                    )
                    ProfileMenuDivider()
                    ProfileMenuRow(
                        title = "Ayuda y soporte",
                        icon = Icons.Default.Help,
                        onClick = openSupport,
                        iconTint = Color(0xFF6B7280),
                        iconBackground = Color(0xFFF2F2F7),
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                ProfileAccountSection(
                    isDeletingAccount = uiState.isDeletingAccount,
                    deleteAccountError = uiState.deleteAccountError,
                    onLogout = onLogout,
                    onDeleteAccount = { showDeleteConfirm = true },
                )
            }
        }

        // Scrollable tail inset — bottom padding on the modifier does not extend scroll range.
        Spacer(modifier = Modifier.height(MainTabContentBottomInset + 24.dp))
    }
}

@Composable
private fun GuestProfileBody(
    serverCheck: Int,
    onLogin: () -> Unit,
    onSupport: () -> Unit,
    onServerConnection: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GuestAvatar()
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Inicia sesión para continuar",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1C1C1E),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Sigue acumulando XP y desbloquea\nrecompensas increíbles 🎉",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF8E8E93),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1C1C1E),
                contentColor = Color.White,
            ),
        ) {
            Text("Iniciar sesión", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.size(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        GuestBenefitCard(
            title = "Acumula XP",
            subtitle = "Gana puntos con cada pedido",
            icon = Icons.Default.EmojiEvents,
            iconTint = Color(0xFF7C6BF2),
            iconBackground = Color(0xFFF1EEFF),
            onClick = onLogin,
        )
        Spacer(modifier = Modifier.height(10.dp))
        GuestBenefitCard(
            title = "Desbloquea recompensas",
            subtitle = "Obtén descuentos y beneficios exclusivos",
            icon = Icons.Default.CardGiftcard,
            iconTint = Color(0xFFE25B73),
            iconBackground = Color(0xFFFFF0F3),
            onClick = onLogin,
        )
        Spacer(modifier = Modifier.height(10.dp))
        GuestBenefitCard(
            title = "Tu historial de pedidos",
            subtitle = "Consulta y reordena fácilmente",
            icon = Icons.Default.Schedule,
            iconTint = Color(0xFF3FA36A),
            iconBackground = Color(0xFFEAF8EF),
            onClick = onLogin,
        )
        Spacer(modifier = Modifier.height(10.dp))
        GuestBenefitCard(
            title = "Ayuda y soporte",
            subtitle = "Resuelve tus dudas",
            icon = Icons.Default.HelpOutline,
            iconTint = Color(0xFF8E8E93),
            iconBackground = Color(0xFFF4F4F6),
            onClick = onSupport,
        )
        Spacer(modifier = Modifier.height(22.dp))
        GuestServerStatus(refreshKey = serverCheck, onClick = onServerConnection)
    }
}

@Composable
private fun GuestAvatar() {
    Box(
        modifier = Modifier.size(132.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = Color(0xFFC9C4F5),
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 8.dp, y = 18.dp)
                .size(16.dp),
        )
        Text(
            text = "+",
            color = Color(0xFFD5D2F2),
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-6).dp, y = 28.dp),
        )
        Text(
            text = "+",
            color = Color(0xFFD5D2F2),
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(y = 28.dp),
        )
        Box(
            modifier = Modifier
                .size(104.dp)
                .clip(CircleShape)
                .background(Color(0xFFF3F4F8)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color(0xFFB7BCC7),
                modifier = Modifier.size(64.dp),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-14).dp, y = (-10).dp)
                .size(34.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, Color(0xFFE6E6EB), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Color(0xFF8E8E93),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun GuestBenefitCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, Color(0xFFECECF1), RoundedCornerShape(18.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1C1C1E),
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8E8E93),
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFFC7C7CC),
        )
    }
}

@Composable
private fun GuestServerStatus(refreshKey: Int, onClick: (() -> Unit)?) {
    var online by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(refreshKey) {
        online = null
        online = pingServerHealth(DevServerStore.baseUrl())
    }
    val statusColor = when (online) {
        true -> Color(0xFF34C759)
        false -> Color(0xFFFF3B30)
        null -> Color(0xFFAEAEB2)
    }
    val statusLabel = when (online) {
        true -> "En línea"
        false -> "No conectado"
        null -> "Comprobando…"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Conexión con servidor",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFAEAEB2),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(statusColor),
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = statusColor,
            )
        }
    }
}

private suspend fun pingServerHealth(apiBaseUrl: String): Boolean = withContext(Dispatchers.IO) {
    val origin = apiBaseUrl.removeSuffix("api/").trimEnd('/')
    val url = "$origin/health"
    try {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 4000
            readTimeout = 4000
            requestMethod = "GET"
            instanceFollowRedirects = true
        }
        try {
            connection.responseCode in 200..299
        } finally {
            connection.disconnect()
        }
    } catch (_: Exception) {
        false
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
