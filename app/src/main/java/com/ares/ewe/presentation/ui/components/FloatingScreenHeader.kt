package com.ares.ewe.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ares.ewe.presentation.ui.main.home.CartIconBadge

object FloatingScreenHeaderDefaults {
    val TitleColor = Color(0xFF111827)
    val BackButtonBackground = Color(0xFFECECEC)
    val CardCornerRadius = 18.dp
    val BackButtonCornerRadius = 12.dp
    val BackButtonSize = 40.dp
    val SideSlotWidth = 48.dp
    /** 5% smaller than the original 20sp title. */
    val TitleFontSize = 19.sp
}

/**
 * Floating top bar: white rounded card, gray back button, centered bold title, optional cart.
 * Reusable on map/detail screens that overlay content edge-to-edge.
 */
@Composable
fun FloatingScreenHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backContentDescription: String = "Volver",
    onCartClick: (() -> Unit)? = null,
    cartItemCount: Int = 0,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(FloatingScreenHeaderDefaults.CardCornerRadius),
        color = Color.White,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.width(FloatingScreenHeaderDefaults.SideSlotWidth),
                contentAlignment = Alignment.CenterStart,
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(FloatingScreenHeaderDefaults.BackButtonSize)
                        .clip(RoundedCornerShape(FloatingScreenHeaderDefaults.BackButtonCornerRadius))
                        .background(FloatingScreenHeaderDefaults.BackButtonBackground),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = backContentDescription,
                        tint = FloatingScreenHeaderDefaults.TitleColor,
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = FloatingScreenHeaderDefaults.TitleFontSize,
                    color = FloatingScreenHeaderDefaults.TitleColor,
                ),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Box(
                modifier = Modifier.width(FloatingScreenHeaderDefaults.SideSlotWidth),
                contentAlignment = Alignment.CenterEnd,
            ) {
                if (onCartClick != null) {
                    CartIconBadge(
                        itemCount = cartItemCount,
                        onClick = onCartClick,
                    )
                }
            }
        }
    }
}
