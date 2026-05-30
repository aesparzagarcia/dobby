package com.ares.ewe.presentation.ui.main.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import coil.compose.AsyncImage
import com.ares.ewe.core.util.ProductCategory
import com.ares.ewe.domain.model.ShopProduct

private val SearchBg = Color(0xFFF3F4F6)
private val MutedText = Color(0xFF6B7280)
private val CardBorder = Color(0xFFE8E8ED)
private val PromoOrange = Color(0xFFFF8A3D)
private val FooterBg = Color(0xFFF5F3FF)

private const val ShopDetailProductCardScale = 0.9f

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

@Composable
fun ShopDetailProductCard(
    product: ShopProduct,
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
    val scale = ShopDetailProductCardScale
    val corner = (16 * scale).dp
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
                    .height((180 * scale).dp)
                    .clip(imageShape)
                    .background(Color(0xFFF3F4F6))
                    .clickable(onClick = onClick),
            ) {
                if (product.imageUrl != null) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        text = product.name.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
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
                    .background(FooterBg)
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
                        tint = primary,
                        modifier = Modifier.size((16 * scale).dp),
                    )
                    Spacer(modifier = Modifier.size((6 * scale).dp))
                    Text(
                        text = "Disponible",
                        style = MaterialTheme.typography.labelSmall,
                        color = primary,
                        fontWeight = FontWeight.Medium,
                    )
                }
                VerticalDivider(
                    modifier = Modifier.height((18 * scale).dp),
                    color = primary.copy(alpha = 0.25f),
                    thickness = 1.dp,
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onAddClick),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size((16 * scale).dp),
                    )
                    Spacer(modifier = Modifier.size((6 * scale).dp))
                    Text(
                        text = "Agregar",
                        style = MaterialTheme.typography.labelSmall,
                        color = primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
