package com.ares.ewe.presentation.ui.main.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ares.ewe.R
import com.ares.ewe.core.theme.DobbyColors
import com.ares.ewe.presentation.viewmodel.main.profile.ProfileUiState

private val ProfileMuted = DobbyColors.TextSecondary
private val ProfileCardSurface = DobbyColors.CardSurface
private val ProfileMenuDivider = Color(0xFFE8E8ED)

data class ProfileMission(
    val title: String,
    val subtitle: String,
    val xpLabel: String,
    val icon: ImageVector,
)

val profileTodayMissions = listOf(
    ProfileMission(
        title = "Haz un pedido",
        subtitle = "Completa un pedido hoy",
        xpLabel = "+20 XP",
        icon = Icons.Default.ShoppingBag,
    ),
    ProfileMission(
        title = "Mantén tu racha",
        subtitle = "Pide al menos 1 día seguido",
        xpLabel = "+5 XP",
        icon = Icons.Default.LocalFireDepartment,
    ),
    ProfileMission(
        title = "Valora tu entrega",
        subtitle = "Califica con 5 estrellas",
        xpLabel = "+5 XP",
        icon = Icons.Default.Star,
    ),
)

data class ProfileBadge(
    val title: String,
    val unlocked: Boolean,
    val background: Color,
    val icon: ImageVector,
)

@Composable
fun ProfileHeroCard(
    uiState: ProfileUiState,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DobbyColors.Primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(DobbyColors.Primary),
            )
            Image(
                painter = painterResource(R.drawable.dobby_card),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                modifier = Modifier.matchParentSize(),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = uiState.avatarLetter,
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                ) {
                    Text(
                        text = uiState.displayName,
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        text = uiState.email,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.88f),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    uiState.phone?.let { phone ->
                        Text(
                            text = formatPhoneDisplay(phone),
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.88f),
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(DobbyColors.Dark.copy(alpha = 0.35f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Pets,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "${uiState.levelName} • Nivel ${uiState.levelNumber}",
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "${uiState.dobbyXp} XP",
                style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { uiState.xpInLevelProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.28f),
            )

            uiState.xpToNextLabel?.let { label ->
                Text(
                    text = label,
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            }
        }
    }
}

@Composable
fun ProfileQuickStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ProfileCardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(iconBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = DobbyColors.Dark,
            )
            Text(
                text = title,
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = ProfileMuted,
            )
        }
    }
}

@Composable
fun ProfileSectionHeader(
    title: String,
    actionLabel: String = "Ver todas",
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = DobbyColors.Dark,
        )
        Text(
            text = actionLabel,
            style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = DobbyColors.Primary,
            modifier = if (onActionClick != null) {
                Modifier.clickable(onClick = onActionClick)
            } else {
                Modifier
            },
        )
    }
}

@Composable
fun ProfileMissionRow(
    mission: ProfileMission,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DobbyColors.Light),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = mission.icon,
                contentDescription = null,
                tint = DobbyColors.Primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = mission.title,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = DobbyColors.Dark,
            )
            Text(
                text = mission.subtitle,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = ProfileMuted,
            )
        }
        Text(
            text = mission.xpLabel,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = DobbyColors.Primary,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = ProfileMuted,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
fun ProfileActivityRow(
    label: String,
    delta: Int,
    timeAgo: String,
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = label,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = DobbyColors.Dark,
            )
            Text(
                text = timeAgo,
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = ProfileMuted,
            )
        }
        Text(
            text = if (delta >= 0) "+$delta XP" else "$delta XP",
            style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (delta >= 0) DobbyColors.Primary else Color(0xFFEF4444),
        )
    }
}

@Composable
fun ProfileBadgeChip(
    badge: ProfileBadge,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(width = 72.dp, height = 64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (badge.unlocked) badge.background else Color(0xFFE5E7EB)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = badge.icon,
                contentDescription = null,
                tint = if (badge.unlocked) Color.White else ProfileMuted,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = badge.title,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = if (badge.unlocked) DobbyColors.Dark else ProfileMuted,
            lineHeight = 14.sp,
        )
    }
}

@Composable
fun ProfileMenuRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DobbyColors.Primary,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = title,
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
            color = DobbyColors.Dark,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = ProfileMuted,
        )
    }
}

@Composable
fun ProfileMenuCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ProfileCardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            content()
        }
    }
}

@Composable
fun ProfileMenuDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(ProfileMenuDivider),
    )
}

fun profileBadgesFor(uiState: ProfileUiState): List<ProfileBadge> = listOf(
    ProfileBadge(
        title = "Primer pedido",
        unlocked = uiState.totalOrdersDelivered >= 1,
        background = Color(0xFFFF8A3D),
        icon = Icons.Default.Pets,
    ),
    ProfileBadge(
        title = "Cliente frecuente",
        unlocked = uiState.totalOrdersDelivered >= 3,
        background = Color(0xFFFFB800),
        icon = Icons.Default.Star,
    ),
    ProfileBadge(
        title = "Racha inicial",
        unlocked = uiState.orderStreakDays >= 1,
        background = Color(0xFF8B5CF6),
        icon = Icons.Default.LocalFireDepartment,
    ),
)

fun activityIconFor(label: String): Triple<ImageVector, Color, Color> = when {
    label.contains("pedido", ignoreCase = true) -> Triple(
        Icons.Default.Pets,
        DobbyColors.Primary,
        DobbyColors.Light,
    )
    label.contains("Compra", ignoreCase = true) -> Triple(
        Icons.Default.ShoppingBag,
        Color(0xFF22C55E),
        Color(0xFFDCFCE7),
    )
    label.contains("Racha", ignoreCase = true) -> Triple(
        Icons.Default.LocalFireDepartment,
        Color(0xFFFF8A3D),
        Color(0xFFFFF7ED),
    )
    else -> Triple(
        Icons.Default.Campaign,
        Color(0xFF8B5CF6),
        Color(0xFFF3E8FF),
    )
}

/** Muestra teléfono nacional de 10 dígitos como +52 XXX XXX XXXX; si no, el texto tal cual. */
fun formatPhoneDisplay(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    return if (digits.length == 10) {
        "+52 ${digits.substring(0, 3)} ${digits.substring(3, 6)} ${digits.substring(6)}"
    } else {
        raw
    }
}
