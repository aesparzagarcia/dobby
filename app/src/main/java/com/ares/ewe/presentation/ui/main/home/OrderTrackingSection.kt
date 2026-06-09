package com.ares.ewe.presentation.ui.main.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ares.ewe.core.theme.DobbyColors
import com.ares.ewe.core.theme.DobbyPureScale
import com.ares.ewe.domain.model.ActiveOrder

/** Escala pura aplicada al tracking en home. */
private object OrderTrackingSectionPalette {
    val HeaderTitle = DobbyColors.TextPrimary
    val CurrentLabel = DobbyColors.TextPrimary
    val InactiveBorder = DobbyPureScale.Mist
    val InactiveIcon = DobbyColors.TextSecondary
    val InactiveLabel = DobbyColors.TextSecondary
    val OnPrimary = DobbyColors.CardSurface
}

private data class TrackingStage(
    val label: String,
    val icon: ImageVector,
)

private val TRACKING_STAGES = listOf(
    TrackingStage("Pendiente", Icons.Default.Check),
    TrackingStage("Confirmado", Icons.Default.ShoppingBag),
    TrackingStage("En preparación", Icons.Default.Inventory2),
    TrackingStage("Listo para recoger", Icons.Default.Store),
    TrackingStage("Asignado", Icons.Default.Person),
    TrackingStage("En camino", Icons.Default.TwoWheeler),
    TrackingStage("Entregado", Icons.Default.Check),
)

private const val TRACKING_LAST_STEP = 6
private val StageColumnWidth = 72.dp
private val ConnectorWidth = 14.dp
private val StageIconSlotSize = 48.dp
private val StageCircleSize = 40.dp
private val StageIconSize = 18.dp
private val ConnectorLineHeight = 3.dp

private enum class ConnectorStyle {
    SolidPurple,
    DashedPurple,
    DashedGrey,
    SolidGrey,
}

@Composable
fun OrderTrackingSection(
    activeOrder: ActiveOrder,
    onViewClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    headerTitle: String = "Tu pedido",
) {
    val stepIndex = activeOrder.stepIndex.coerceIn(0, TRACKING_LAST_STEP)
    val scroll = rememberScrollState()
    val primary = MaterialTheme.colorScheme.primary
    val currentHalo = primary.copy(alpha = 0.22f)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = OrderTrackingSectionPalette.OnPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = headerTitle,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrderTrackingSectionPalette.HeaderTitle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Row(
                    modifier = Modifier
                        .clickable(onClick = onViewClick)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Ver detalles",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = primary,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scroll),
                verticalAlignment = Alignment.Top,
            ) {
                TRACKING_STAGES.forEachIndexed { index, stage ->
                    if (index > 0) {
                        OrderTrackingConnector(
                            primary = primary,
                            style = connectorStyle(leftStageIndex = index - 1, currentStepIndex = stepIndex),
                            modifier = Modifier
                                .width(ConnectorWidth)
                                .padding(top = (StageIconSlotSize - ConnectorLineHeight) / 2),
                        )
                    }
                    OrderTrackingStage(
                        label = stage.label,
                        icon = stage.icon,
                        isCompleted = index < stepIndex,
                        isCurrent = index == stepIndex,
                        primary = primary,
                        currentHalo = currentHalo,
                        modifier = Modifier.width(StageColumnWidth),
                    )
                }
            }
        }
    }
}

private fun connectorStyle(leftStageIndex: Int, currentStepIndex: Int): ConnectorStyle = when {
    leftStageIndex < currentStepIndex - 1 -> ConnectorStyle.SolidPurple
    leftStageIndex == currentStepIndex - 1 -> ConnectorStyle.DashedPurple
    leftStageIndex == currentStepIndex -> ConnectorStyle.DashedGrey
    else -> ConnectorStyle.SolidGrey
}

@Composable
private fun OrderTrackingStage(
    label: String,
    icon: ImageVector,
    isCompleted: Boolean,
    isCurrent: Boolean,
    primary: Color,
    currentHalo: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(StageIconSlotSize),
            contentAlignment = Alignment.Center,
        ) {
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(currentHalo),
                )
            }
            val circleModifier = Modifier
                .size(StageCircleSize)
                .clip(CircleShape)
                .then(
                    when {
                        isCompleted -> Modifier.background(primary)
                        isCurrent -> Modifier
                            .background(OrderTrackingSectionPalette.OnPrimary)
                            .border(2.dp, primary, CircleShape)
                        else -> Modifier
                            .background(OrderTrackingSectionPalette.OnPrimary)
                            .border(1.5.dp, OrderTrackingSectionPalette.InactiveBorder, CircleShape)
                    },
                )
            Box(
                modifier = circleModifier,
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(StageIconSize),
                    tint = when {
                        isCompleted -> OrderTrackingSectionPalette.OnPrimary
                        isCurrent -> primary
                        else -> OrderTrackingSectionPalette.InactiveIcon
                    },
                )
            }
        }
        Text(
            text = label,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            textAlign = TextAlign.Center,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isCurrent -> OrderTrackingSectionPalette.CurrentLabel
                isCompleted -> primary
                else -> OrderTrackingSectionPalette.InactiveLabel
            },
            maxLines = 2,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun OrderTrackingConnector(
    primary: Color,
    style: ConnectorStyle,
    modifier: Modifier = Modifier,
) {
    val color = when (style) {
        ConnectorStyle.SolidPurple, ConnectorStyle.DashedPurple -> primary
        ConnectorStyle.DashedGrey, ConnectorStyle.SolidGrey -> OrderTrackingSectionPalette.InactiveBorder
    }
    val dashed = style == ConnectorStyle.DashedPurple || style == ConnectorStyle.DashedGrey
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(ConnectorLineHeight),
    ) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = size.height,
            pathEffect = if (dashed) {
                PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
            } else {
                null
            },
        )
    }
}
