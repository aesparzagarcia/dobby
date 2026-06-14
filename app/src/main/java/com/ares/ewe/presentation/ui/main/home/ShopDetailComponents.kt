package com.ares.ewe.presentation.ui.main.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.RoomService
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ares.ewe.presentation.components.LoadingAsyncImage
import com.ares.ewe.core.util.ProductCategory
import com.ares.ewe.domain.model.ShopProduct

private val SearchBg = Color(0xFFF3F4F6)
private val MutedText = Color(0xFF6B7280)
private val CardBorder = Color(0xFFE8E8ED)
private val PromoOrange = Color(0xFFFF8A3D)
private val ClosedGray = Color(0xFF6B7280)
private val FooterBg = Color(0xFFF5F3FF)
private val UnavailableFooterBg = Color(0xFFF3F4F6)

private const val ShopDetailProductCardScale = 0.9f
/** Image band aspect ratio (width : height). 4:3 gives room for tall products without a full-width square. */
private const val ShopDetailProductImageAspectRatio = 4f / 3f

@Composable
fun ShopDetailSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(SearchBg)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MutedText,
                modifier = Modifier.size(20.dp),
            )
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                text = "Buscar productos...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedText,
                            )
                        }
                        inner()
                    }
                },
            )
        }
    }
}

@Composable
fun ShopClosedBanner(
    reopensLabel: String?,
    modifier: Modifier = Modifier,
) {
    val closedRed = Color(0xFFEF4444)
    val bannerBg = Color(0xFFFFF1F2)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bannerBg)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(50))
                .background(closedRed),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = "Tienda cerrada",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = closedRed,
            )
            if (reopensLabel != null) {
                Spacer(modifier = Modifier.height(4.dp))
                val timePrefix = "Abre hoy a las "
                val opensText = buildAnnotatedString {
                    if (reopensLabel.startsWith(timePrefix)) {
                        append(timePrefix)
                        withStyle(SpanStyle(color = closedRed, fontWeight = FontWeight.SemiBold)) {
                            append(reopensLabel.removePrefix(timePrefix))
                        }
                    } else {
                        append(reopensLabel)
                    }
                }
                Text(
                    text = opensText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Los productos estarán disponibles cuando la tienda abra.",
                style = MaterialTheme.typography.labelSmall,
                color = MutedText,
            )
        }

        //ShopClosedStoreIllustration(modifier = Modifier.size(width = 56.dp, height = 52.dp))
    }
}

@Composable
private fun ShopClosedStoreIllustration(modifier: Modifier = Modifier) {
    val awningPink = Color(0xFFF9A8D4)
    val awningWhite = Color(0xFFFDF2F8)
    val signRed = Color(0xFFEF4444)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .background(if (index % 2 == 0) awningPink else awningWhite),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                .background(Color(0xFFFCE7F3)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(signRed)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "CERRADO",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
fun ShopDetailCategoryRow(
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(ProductCategory.filterChips, key = { it.label }) { chip ->
            val selected = selectedCategoryId == chip.filterId
            val shape = RoundedCornerShape(50)
            val primary = MaterialTheme.colorScheme.primary

            Row(
                modifier = Modifier
                    .height(40.dp)
                    .clip(shape)
                    .then(
                        if (selected) {
                            Modifier.background(primary, shape)
                        } else {
                            Modifier
                                .border(1.dp, CardBorder, shape)
                                .background(Color.White, shape)
                        },
                    )
                    .clickable { onCategorySelected(chip.filterId) }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = chip.icon,
                    contentDescription = null,
                    tint = if (selected) Color.White else MutedText,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = chip.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopDetailProductCard(
    product: ShopProduct,
    isProductAvailable: Boolean,
    onClick: () -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val validDiscount = product.discount.coerceIn(0, 100)
    val showPromotion = product.hasPromotion && validDiscount > 0
    val discountedPrice = if (showPromotion) {
        product.price * (1 - validDiscount / 100.0)
    } else {
        product.price
    }
    val primary = MaterialTheme.colorScheme.primary
    val statusColor = if (isProductAvailable) primary else ClosedGray
    val footerBg = if (isProductAvailable) FooterBg else UnavailableFooterBg
    val scale = ShopDetailProductCardScale
    val corner = (16 * scale).dp
    val imagePadding = (8 * scale).dp
    val imageShape = RoundedCornerShape(topStart = corner, topEnd = corner)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(corner),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ShopDetailProductImageAspectRatio)
                    .clip(imageShape)
                    .background(Color.White)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                LoadingAsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(imagePadding),
                    contentScale = ContentScale.Fit,
                    placeholderBackground = Color.White,
                    error = {
                        Text(
                            text = product.name.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(
                        horizontal = (14 * scale).dp,
                        vertical = (12 * scale).dp,
                    ),
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = product.description?.takeIf { it.isNotBlank() }.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = (4 * scale).dp),
                )

                HorizontalDivider(
                    color = CardBorder,
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = (10 * scale).dp),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy((8 * scale).dp),
                    ) {
                        Text(
                            text = "$${String.format("%.2f", discountedPrice)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (showPromotion) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(PromoOrange)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = "-$validDiscount%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                                Text(
                                    text = "$${String.format("%.2f", product.price)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    textDecoration = TextDecoration.LineThrough,
                                )
                            }
                        }
                    }
                    RatingDisplay(
                        rate = product.rate,
                        ratingCount = product.ratingCount,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(footerBg)
                    .padding(vertical = (10 * scale).dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.RoomService,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size((16 * scale).dp),
                    )
                    Spacer(modifier = Modifier.size((6 * scale).dp))
                    Text(
                        text = if (isProductAvailable) "Disponible" else "No disponible",
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Medium,
                    )
                }
                VerticalDivider(
                    modifier = Modifier.height((18 * scale).dp),
                    color = statusColor.copy(alpha = 0.25f),
                    thickness = 1.dp,
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (isProductAvailable) {
                                Modifier.clickable(onClick = onAddClick)
                            } else {
                                Modifier
                            },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = statusColor.copy(alpha = if (isProductAvailable) 1f else 0.45f),
                        modifier = Modifier.size((16 * scale).dp),
                    )
                    Spacer(modifier = Modifier.size((6 * scale).dp))
                    Text(
                        text = "Agregar",
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor.copy(alpha = if (isProductAvailable) 1f else 0.45f),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
