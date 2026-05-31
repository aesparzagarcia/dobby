package com.ares.ewe.presentation.ui.main.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ares.ewe.domain.model.Ad
import com.ares.ewe.core.theme.DobbyColors
import com.ares.ewe.domain.model.FeaturedPlace
import com.ares.ewe.presentation.components.DeliveryAddressCallout
import com.ares.ewe.presentation.components.MainTabContentBottomInset
import com.ares.ewe.presentation.viewmodel.main.home.HomeTabViewModel
import kotlinx.coroutines.delay

private val HomeScreenBackground = Color.White
private const val DESTACADOS_PREVIEW_LIMIT = 4
private const val BEST_SELLERS_PREVIEW_LIMIT = 4

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTabScreen(
    onPlaceClick: (FeaturedPlace) -> Unit = {},
    onAdClick: (String) -> Unit = {},
    onAddressLabelClick: () -> Unit = {},
    onProductClick: (productId: String, shopId: String?) -> Unit = { _, _ -> },
    onCartClick: () -> Unit = {},
    onTrackOrderClick: (String) -> Unit = {},
    onActiveOrdersClick: () -> Unit = {},
    onPromotionsTabClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeTabViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val cartItemCount by viewModel.cartItemCount.collectAsState(0)
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    var quickCategory by remember { mutableStateOf(HomeQuickCategory.All) }

    LaunchedEffect(Unit) {
        viewModel.loadAddresses()
    }

    val query = uiState.searchQuery.trim()
    val categoryPlaces = filterPlacesByCategory(uiState.featuredPlaces, quickCategory)
    val filteredPlaces = categoryPlaces.filter {
        query.isBlank() || it.name.contains(query, ignoreCase = true)
    }
    val categoryProducts = when (quickCategory) {
        HomeQuickCategory.Offers -> uiState.bestSellerProducts.filter {
            it.hasPromotion && it.discount > 0
        }
        else -> uiState.bestSellerProducts
    }
    val filteredProducts = categoryProducts.filter {
        query.isBlank() || it.name.contains(query, ignoreCase = true)
    }
    // Card restaurante/tienda: 20% más compacto que el tamaño base
    val featuredCardWidthPx = (screenWidthDp * 0.56f * 0.8f).toInt().coerceIn(160, 198)
    val featuredCardWidth = featuredCardWidthPx.dp
    val destacadosPreview = filteredPlaces.take(DESTACADOS_PREVIEW_LIMIT)
    val bestSellersPreview = filteredProducts.take(BEST_SELLERS_PREVIEW_LIMIT)
    // Ancho del producto: 10% menos que restaurante + 15% más compacto
    val productCardWidth = (featuredCardWidthPx * 0.9f * 0.85f).toInt().dp

    Column(modifier = modifier.fillMaxSize()) {
        when {
            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadHome() }) {
                            Text("Reintentar")
                        }
                    }
                }
            }
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                val listState = rememberLazyListState()
                LaunchedEffect(uiState.activeOrders.map { it.id }) {
                    listState.scrollToItem(0)
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(HomeScreenBackground),
                ) {
                    uiState.warningMessage?.let { msg ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = DobbyColors.Warning.copy(alpha = 0.15f),
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Row(
                                Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = msg,
                                    color = DobbyColors.Dark,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = { viewModel.clearWarningMessage() }) {
                                    Text("Cerrar")
                                }
                            }
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            HomeAddressSearchHeader(
                                addressLabel = uiState.addressLabel,
                                address = uiState.address,
                                searchQuery = uiState.searchQuery,
                                onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                                onAddressClick = onAddressLabelClick,
                            )
                            if (uiState.addressFetchCompleted &&
                                uiState.needsDeliveryAddressCallout &&
                                uiState.warningMessage == null
                            ) {
                                DeliveryAddressCallout(
                                    title = "Añade tu dirección de entrega",
                                    subtitle = "Toca aquí para agregar una dirección y descubrir qué restaurantes pueden entregarte",
                                    onClick = onAddressLabelClick,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 13.dp)
                                        .padding(bottom = 4.dp),
                                )
                            }
                        }
                        CartIconBadge(
                            itemCount = cartItemCount,
                            onClick = onCartClick,
                            modifier = Modifier.align(Alignment.TopEnd),
                        )
                    }
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        val restaurantsOnly = filteredPlaces.filter {
                            !it.isService && (it.shopType == "RESTAURANT" || it.shopType == null)
                        }
                        val servicesOnly = filteredPlaces.filter { it.isService }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(HomeScreenBackground),
                            contentPadding = PaddingValues(bottom = 0.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp),
                        ) {
                            if (uiState.activeOrders.isNotEmpty()) {
                                item(key = "active_orders") {
                                    ActiveOrdersHomeSection(
                                        activeOrders = uiState.activeOrders,
                                        onTrackOrderClick = onTrackOrderClick,
                                        onMultipleOrdersClick = onActiveOrdersClick,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                    )
                                }
                            }
                            item(key = "categories") {
                                HomeCategoryRow(
                                    selected = quickCategory,
                                    onCategorySelected = { cat ->
                                        quickCategory = cat
                                        if (cat == HomeQuickCategory.Offers) {
                                            onPromotionsTabClick()
                                        }
                                    },
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                            }
                            if (destacadosPreview.isNotEmpty()) {
                                item(key = "featured") {
                                    HomeSectionHeader(
                                        title = "Destacados",
                                        onSeeAllClick = { quickCategory = HomeQuickCategory.All },
                                    )
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        items(destacadosPreview, key = { it.id }) { place ->
                                            HomeFeaturedPlaceCard(
                                                place = place,
                                                modifier = Modifier.width(featuredCardWidth),
                                                onClick = { onPlaceClick(place) },
                                            )
                                        }
                                        item(key = "featured_see_more") {
                                            HomeFeaturedSeeMoreCard(
                                                modifier = Modifier.width(featuredCardWidth),
                                                onClick = { quickCategory = HomeQuickCategory.All },
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                            if (bestSellersPreview.isNotEmpty()) {
                                item(key = "best_sellers") {
                                    HomeSectionHeader(
                                        title = "Más vendidos",
                                        onSeeAllClick = onPromotionsTabClick,
                                    )
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        items(bestSellersPreview, key = { it.id }) { product ->
                                            HomeProductCarouselCard(
                                                product = product,
                                                modifier = Modifier.width(productCardWidth),
                                                onClick = { onProductClick(product.id, product.shopId) },
                                            )
                                        }
                                        item(key = "best_sellers_see_more") {
                                            HomeProductSeeMoreCard(
                                                modifier = Modifier.width(productCardWidth),
                                                onClick = onPromotionsTabClick,
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                            if (query.isNotBlank() && filteredPlaces.isEmpty() && filteredProducts.isEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(
                                        text = "Sin resultados para «$query»",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                }
                            }
                            if (uiState.ads.isEmpty()) {
                                item(key = "promo_banner") {
                                    HomePromoBanner(
                                        title = "Envío gratis en tu primer pedido",
                                        subtitle = "Pide ahora y recibe tu pedido sin costo de envío",
                                        modifier = Modifier.padding(vertical = 8.dp),
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                            if (uiState.ads.isNotEmpty()) {
                                item(key = "ads") {
                                    AdsCarousel(
                                        ads = uiState.ads,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp)
                                            .padding(vertical = 8.dp),
                                        onAdClick = onAdClick,
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                            if (restaurantsOnly.isNotEmpty()) {
                                item(key = "restaurants") {
                                    HomeSectionHeader(
                                        title = "Restaurantes populares",
                                        onSeeAllClick = {
                                            quickCategory = HomeQuickCategory.Restaurants
                                        },
                                    )
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        items(restaurantsOnly, key = { it.id }) { place ->
                                            HomeFeaturedPlaceCard(
                                                place = place,
                                                modifier = Modifier.width(featuredCardWidth),
                                                onClick = { onPlaceClick(place) },
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                            if (servicesOnly.isNotEmpty()) {
                                item(key = "services") {
                                    HomeSectionHeader(
                                        title = "Servicios destacados",
                                        onSeeAllClick = {
                                            quickCategory = HomeQuickCategory.Services
                                        },
                                    )
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        items(servicesOnly, key = { it.id }) { place ->
                                            HomeServicePlaceRow(
                                                place = place,
                                                onClick = { onPlaceClick(place) },
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                            item(key = "bottom_spacer") {
                                Spacer(
                                    modifier = Modifier.height(16.dp + MainTabContentBottomInset),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun filterPlacesByCategory(
    places: List<FeaturedPlace>,
    category: HomeQuickCategory,
): List<FeaturedPlace> = when (category) {
    HomeQuickCategory.All -> places
    HomeQuickCategory.Restaurants -> places.filter { !it.isService && it.shopType != "SHOP" }
    HomeQuickCategory.Shops -> places.filter { !it.isService && it.shopType == "SHOP" }
    HomeQuickCategory.Services -> places.filter { it.isService }
    HomeQuickCategory.Offers -> places
}

@Composable
private fun AdsCarousel(
    ads: List<Ad>,
    modifier: Modifier = Modifier,
    onAdClick: (String) -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(ads.size) {
        if (ads.size <= 1) return@LaunchedEffect
        while (true) {
            delay(2000L)
            val next = (listState.firstVisibleItemIndex + 1) % ads.size
            listState.animateScrollToItem(next)
        }
    }
    BoxWithConstraints(modifier = modifier) {
        val itemWidth = maxWidth - 32.dp
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(ads) { ad ->
                Card(
                    modifier = Modifier
                        .width(itemWidth)
                        .fillMaxWidth()
                        .height(160.dp)
                        .clickable { onAdClick(ad.id) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (ad.imageUrl != null) {
                            AsyncImage(
                                model = ad.imageUrl,
                                contentDescription = ad.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = ad.name.take(1).uppercase(),
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}