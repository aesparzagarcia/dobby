package com.ares.ewe.presentation.ui.main.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ares.ewe.core.util.toDisplayImageUrl
import coil.compose.AsyncImage

/** Escala del card de producto (ancho adicional desde [HomeTabScreen]). */
private const val ProductCardScale = 0.85f

@Composable
fun UniversalProductCard(
    name: String,
    imageUrl: String?,
    price: Double,
    rate: Float,
    hasPromotion: Boolean,
    discount: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val resolvedImageUrl = imageUrl.toDisplayImageUrl()
    val validDiscount = discount.coerceIn(0, 100)
    val showPromotion = hasPromotion && validDiscount > 0
    val discountedPrice = if (showPromotion) price * (1 - validDiscount / 100.0) else price
    val corner = (16 * ProductCardScale).dp
    val imageHeight = (120 * ProductCardScale).dp

    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(corner),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = (10 * ProductCardScale).dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(imageHeight)
                    .clip(RoundedCornerShape(topStart = corner, topEnd = corner))
                    .background(Color(0xFFF3F4F6)),
                contentAlignment = Alignment.Center,
            ) {
                if (resolvedImageUrl != null) {
                    AsyncImage(
                        model = resolvedImageUrl,
                        contentDescription = name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        text = name.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (showPromotion) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 0.dp,
                                    topEnd = (12 * ProductCardScale).dp,
                                    bottomEnd = (12 * ProductCardScale).dp,
                                    bottomStart = 0.dp,
                                ),
                            )
                            .background(Color(0xFFFFE34D))
                            .padding(
                                horizontal = (8 * ProductCardScale).dp,
                                vertical = (4 * ProductCardScale).dp,
                            ),
                        horizontalArrangement = Arrangement.spacedBy((6 * ProductCardScale).dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "-$validDiscount%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "$${String.format("%.2f", price)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textDecoration = TextDecoration.LineThrough,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                }
            }

            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = (10 * ProductCardScale).dp,
                        vertical = (8 * ProductCardScale).dp,
                    ),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = (10 * ProductCardScale).dp,
                        vertical = (3 * ProductCardScale).dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((6 * ProductCardScale).dp),
            ) {
                Text(
                    text = "$${String.format("%.2f", discountedPrice)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    RatingDisplay(rate = rate)
                }
            }
        }
    }
}

@Composable
fun HomeProductSeeMoreCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val scale = ProductCardScale
    val corner = (16 * scale).dp
    val imageHeight = (120 * scale).dp
    val primary = MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(corner),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = (10 * scale).dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(0f),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(imageHeight),
                )
                ProductCardFooterPlaceholder(scale = scale)
            }
            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((8 * scale).dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size((20 * scale).dp),
                )
                Text(
                    text = "Ver más",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size((20 * scale).dp),
                )
            }
        }
    }
}

@Composable
private fun ProductCardFooterPlaceholder(scale: Float) {
    Text(
        text = "\u00A0",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(0f)
            .padding(
                horizontal = (10 * scale).dp,
                vertical = (8 * scale).dp,
            ),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(0f)
            .padding(
                horizontal = (10 * scale).dp,
                vertical = (3 * scale).dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((6 * scale).dp),
    ) {
        Text(
            text = "\u00A0",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd,
        ) {
            RatingDisplay(rate = 0f)
        }
    }
}
