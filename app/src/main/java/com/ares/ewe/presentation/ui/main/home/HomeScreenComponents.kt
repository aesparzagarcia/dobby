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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ares.ewe.presentation.components.LoadingAsyncImage
import com.ares.ewe.core.theme.DobbyColors
import com.ares.ewe.core.theme.DobbyPureScale
import com.ares.ewe.core.util.serviceCategoryLabelEs
import com.ares.ewe.domain.model.BestSellerProduct
import com.ares.ewe.domain.model.FeaturedPlace

private val HomeBg = DobbyPureScale.Pure
private val SearchBg = DobbyColors.SurfaceMuted
private val CardBorder = Color(0xFFE8E8ED)
private val OpenGreen = Color(0xFF22C55E)
private val ClosedGray = Color(0xFF6B7280)
private val MutedText = DobbyColors.TextSecondary

enum class HomeQuickCategory {
    All,
    Restaurants,
    Shops,
    Services,
    Offers,
}

private data class HomeCategoryItem(
    val category: HomeQuickCategory,
    val label: String,
    val icon: ImageVector,
)

private const val HomeCategoryRowScale = 0.9f

@Composable
fun HomeAddressSearchHeader(
    addressLabel: String?,
    address: String?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAddressClick: () -> Unit,
    modifier: Modifier = Modifier,
    addressCallout: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(HomeBg)
            .padding(bottom = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onAddressClick)
                .padding(start = 16.dp, end = 56.dp, top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = addressLabel ?: "Casa",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = address ?: "Añade tu dirección",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (address != null) MutedText else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        addressCallout()

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(14.dp),
            color = SearchBg,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MutedText,
                    modifier = Modifier.size(22.dp),
                )
                androidx.compose.foundation.text.BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    singleLine = true,
                    decorationBox = { inner ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Buscar restaurantes, productos o servicios",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        inner()
                    },
                )
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Filtros",
                    tint = MutedText,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun HomeCategoryItemColumn(
    item: HomeCategoryItem,
    isSelected: Boolean,
    scale: Float,
    onClick: () -> Unit,
) {
    val s = scale
    val itemWidth = (72 * s).dp
    val iconBoxSize = (56 * s).dp
    val iconBoxCorner = (16 * s).dp
    val iconSize = (26 * s).dp
    val labelGap = (6 * s).dp
    val boxBackground = if (isSelected) DobbyPureScale.Pure else DobbyPureScale.Onyx
    val iconTint = if (isSelected) DobbyPureScale.Onyx else DobbyPureScale.Pure

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(itemWidth)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(iconBoxSize)
                .clip(RoundedCornerShape(iconBoxCorner))
                .background(boxBackground)
                .then(
                    if (isSelected) {
                        Modifier.border(
                            (2 * s).dp,
                            DobbyPureScale.Onyx,
                            RoundedCornerShape(iconBoxCorner),
                        )
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = iconTint,
                modifier = Modifier.size(iconSize),
            )
        }
        Spacer(modifier = Modifier.height(labelGap))
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) DobbyPureScale.Onyx else DobbyColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun HomeCategoryRow(
    selected: HomeQuickCategory,
    onCategorySelected: (HomeQuickCategory) -> Unit,
    modifier: Modifier = Modifier,
    includeOffers: Boolean = true,
    scale: Float = HomeCategoryRowScale,
    spreadToEdges: Boolean = false,
) {
    val items = buildList {
        add(HomeCategoryItem(HomeQuickCategory.Restaurants, "Restaurantes", Icons.Default.Restaurant))
        add(HomeCategoryItem(HomeQuickCategory.Shops, "Tiendas", Icons.Default.ShoppingBag))
        add(HomeCategoryItem(HomeQuickCategory.Services, "Servicios", Icons.Default.Handyman))
        if (includeOffers) {
            add(HomeCategoryItem(HomeQuickCategory.Offers, "Ofertas", Icons.Default.LocalOffer))
        }
        add(HomeCategoryItem(HomeQuickCategory.All, "Ver todos", Icons.Default.Apps))
    }
    val s = scale
    val rowPaddingH = (16 * s).dp
    val rowSpacing = (12 * s).dp
    val rowModifier = modifier
        .fillMaxWidth()
        .background(DobbyPureScale.Pure)

    if (spreadToEdges) {
        Row(
            modifier = rowModifier
                .padding(horizontal = rowPaddingH)
                .padding(vertical = (6 * s).dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            items.forEach { item ->
                HomeCategoryItemColumn(
                    item = item,
                    isSelected = selected == item.category,
                    scale = s,
                    onClick = { onCategorySelected(item.category) },
                )
            }
        }
    } else {
        androidx.compose.foundation.lazy.LazyRow(
            modifier = rowModifier,
            contentPadding = PaddingValues(horizontal = rowPaddingH),
            horizontalArrangement = Arrangement.spacedBy(rowSpacing),
        ) {
            items(items.size) { index ->
                val item = items[index]
                HomeCategoryItemColumn(
                    item = item,
                    isSelected = selected == item.category,
                    scale = s,
                    onClick = { onCategorySelected(item.category) },
                )
            }
        }
    }
}

@Composable
fun HomeSectionHeader(
    title: String,
    onSeeAllClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(DobbyPureScale.Pure)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Escala visual del card de restaurante/tienda (ancho se aplica desde [HomeTabScreen]). */
private const val FeaturedPlaceCardScale = 0.8f

private fun featuredPlaceCardCorner(cardScale: Float) = (16 * cardScale).dp.coerceAtLeast(12.dp)

private fun featuredPlaceImageAspect(cardScale: Float) = 1.65f / cardScale.coerceAtLeast(0.8f)

@Composable
fun HomeFeaturedSeeMoreCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    cardScale: Float = FeaturedPlaceCardScale,
) {
    val corner = featuredPlaceCardCorner(cardScale)
    val imageAspect = featuredPlaceImageAspect(cardScale)
    val primary = MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(corner),
        colors = CardDefaults.cardColors(containerColor = HomeBg),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (cardScale >= 1f) 2.dp else 4.dp,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = (8 * cardScale).dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(0f),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(imageAspect),
                )
                FeaturedPlaceCardFooterPlaceholder(scale = cardScale)
            }
            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((8 * cardScale).dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size((20 * cardScale).dp),
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
                    modifier = Modifier.size((20 * cardScale).dp),
                )
            }
        }
    }
}

@Composable
private fun FeaturedPlaceCardFooterPlaceholder(scale: Float) {
    Text(
        text = "\u00A0",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(0f)
            .padding(
                horizontal = (10 * scale).dp,
                vertical = (6 * scale).dp,
            ),
    )
    Text(
        text = "\u00A0",
        style = MaterialTheme.typography.bodySmall,
        color = MutedText,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(0f)
            .padding(horizontal = (10 * scale).dp),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(0f)
            .padding(
                horizontal = (10 * scale).dp,
                vertical = (4 * scale).dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((6 * scale).dp),
    ) {
        RatingDisplay(
            rate = 0f,
            modifier = Modifier.wrapContentWidth(),
        )
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((4 * scale).dp),
        ) {
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                tint = MutedText,
                modifier = Modifier.size((13 * scale).dp),
            )
            Text(
                text = "\u00A0",
                style = MaterialTheme.typography.labelSmall,
                color = MutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = true),
            )
        }
    }
}

@Composable
fun HomeFeaturedPlaceCard(
    place: FeaturedPlace,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    cardScale: Float = FeaturedPlaceCardScale,
) {
    val isOpen = rememberIsPlaceOpenNow(place.openingHour, place.closingHour)
    val hoursLabel = formatPlaceHoursRange(place.openingHour, place.closingHour)
    val subtitle = placeSubtitle(place)
    val corner = featuredPlaceCardCorner(cardScale)
    val imageAspect = featuredPlaceImageAspect(cardScale)

    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(corner),
        colors = CardDefaults.cardColors(containerColor = HomeBg),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (cardScale >= 1f) 2.dp else 4.dp,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = (8 * cardScale).dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(imageAspect)
                    .clip(RoundedCornerShape(topStart = corner, topEnd = corner))
                    .background(Color(0xFFF3F4F6)),
            ) {
                LoadingAsyncImage(
                    model = place.imageUrl,
                    contentDescription = place.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = {
                        Text(
                            text = place.name.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding((8 * cardScale).dp),
                ) {
                    when (isOpen) {
                        true -> StatusPill(text = "Abierto", color = OpenGreen, scale = cardScale)
                        false -> StatusPill(text = "Cerrado", color = ClosedGray, scale = cardScale)
                        null -> {}
                    }
                }
            }
            Text(
                text = place.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = (10 * cardScale).dp,
                        vertical = (6 * cardScale).dp,
                    ),
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = (10 * cardScale).dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = (10 * cardScale).dp,
                        vertical = (4 * cardScale).dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((6 * cardScale).dp),
            ) {
                RatingDisplay(
                    rate = place.rate,
                    modifier = Modifier.wrapContentWidth(),
                )
                if (hoursLabel != null) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy((4 * cardScale).dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = MutedText,
                            modifier = Modifier.size((13 * cardScale).dp),
                        )
                        Text(
                            text = hoursLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = true),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeServicePlaceRow(
    place: FeaturedPlace,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val category = serviceCategoryLabelEs(place.serviceCategory) ?: place.typeLabel
    Card(
        modifier = modifier
            .width(260.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = HomeBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF3F4F6)),
                contentAlignment = Alignment.Center,
            ) {
                LoadingAsyncImage(
                    model = place.imageUrl,
                    contentDescription = place.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = {
                        Text(
                            text = place.name.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                RatingDisplay(
                    rate = place.rate,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
fun HomeProductCarouselCard(
    product: BestSellerProduct,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    UniversalProductCard(
        name = product.name,
        imageUrl = product.imageUrl,
        price = product.price,
        rate = product.rate,
        hasPromotion = product.hasPromotion,
        discount = product.discount,
        modifier = modifier,
        onClick = onClick,
    )
}

@Composable
fun HomePromoBanner(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = DobbyColors.Primary,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f),
            )
        }
    }
}

@Composable
private fun StatusPill(
    text: String,
    color: Color,
    scale: Float = FeaturedPlaceCardScale,
) {
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape((6 * scale).dp))
            .background(color)
            .padding(horizontal = (6 * scale).dp, vertical = (2 * scale).dp),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = Color.White,
    )
}

private fun placeSubtitle(place: FeaturedPlace): String {
    if (place.isService) {
        val cat = serviceCategoryLabelEs(place.serviceCategory)
        return if (cat != null) "Servicio • $cat" else place.typeLabel
    }
    return place.typeLabel
}

fun filterPlacesByCategory(
    places: List<FeaturedPlace>,
    category: HomeQuickCategory,
): List<FeaturedPlace> = when (category) {
    HomeQuickCategory.All -> places
    HomeQuickCategory.Restaurants -> places.filter { !it.isService && it.shopType != "SHOP" }
    HomeQuickCategory.Shops -> places.filter { !it.isService && it.shopType == "SHOP" }
    HomeQuickCategory.Services -> places.filter { it.isService }
    HomeQuickCategory.Offers -> places
}
