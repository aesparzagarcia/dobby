package com.ares.ewe.presentation.ui.main.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ares.ewe.domain.model.ActiveOrder

private val STAGE_LABELS = listOf(
    "Pendiente",
    "Confirmado",
    "Preparando",
    "Listo",
    "Asignado",
    "En camino",
    "Entregado"
)
private val STAGE_ICONS = listOf(
    Icons.Default.Schedule,
    Icons.Default.ShoppingBag,
    Icons.Default.Inventory,
    Icons.Default.LocalShipping,
    Icons.Default.Person,
    Icons.Default.TwoWheeler,
    Icons.Default.CheckCircle
)

private const val TRACKING_LAST_STEP = 6
/// Step index for [ActiveOrder.status] `ASSIGNED` — no map/courier until then.
private const val TRACKING_STEP_ASSIGNED = 4

@Composable
fun OrderTrackingSection(
    activeOrder: ActiveOrder,
    onViewClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val stepIndex = activeOrder.stepIndex.coerceIn(0, TRACKING_LAST_STEP)
    val showMapButton = stepIndex >= TRACKING_STEP_ASSIGNED
    val scroll = rememberScrollState()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Tu pedido",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scroll),
                verticalAlignment = Alignment.CenterVertically
            ) {
                STAGE_LABELS.forEachIndexed { index, label ->
                    OrderTrackingStage(
                        label = label,
                        icon = STAGE_ICONS[index],
                        isCompleted = index < stepIndex,
                        isCurrent = index == stepIndex,
                        modifier = Modifier.width(68.dp)
                    )
                    if (index < STAGE_LABELS.lastIndex) {
                        OrderTrackingConnector(
                            completed = index < stepIndex,
                            modifier = Modifier
                                .width(16.dp)
                                .padding(horizontal = 2.dp)
                        )
                    }
                }
            }
            if (showMapButton) {
                Button(
                    onClick = onViewClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Text("Ver mapa y detalles")
                }
            }
        }
    }
}

@Composable
private fun OrderTrackingStage(
    label: String,
    icon: ImageVector,
    isCompleted: Boolean,
    isCurrent: Boolean,
    modifier: Modifier = Modifier
) {
    val circleColor by animateColorAsState(
        targetValue = when {
            isCompleted || isCurrent -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        },
        animationSpec = tween(durationMillis = 300, easing = LinearEasing),
        label = "circleColor"
    )
    val iconColor by animateColorAsState(
        targetValue = when {
            isCompleted || isCurrent -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        },
        animationSpec = tween(durationMillis = 300, easing = LinearEasing),
        label = "iconColor"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .scale(if (isCurrent) scale else 1f)
                .clip(CircleShape)
                .background(circleColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp),
                tint = iconColor
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isCompleted || isCurrent) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            },
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun OrderTrackingConnector(
    completed: Boolean,
    modifier: Modifier = Modifier
) {
    val lineColor by animateColorAsState(
        targetValue = if (completed) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        },
        animationSpec = tween(durationMillis = 300, easing = LinearEasing),
        label = "connectorColor"
    )
    Box(
        modifier = modifier
            .height(3.dp)
            .background(lineColor, androidx.compose.foundation.shape.RoundedCornerShape(2.dp)),
        contentAlignment = Alignment.Center
    ) {}
}
