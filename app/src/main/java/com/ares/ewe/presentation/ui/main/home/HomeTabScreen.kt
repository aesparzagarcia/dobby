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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ares.ewe.domain.model.Ad
import com.ares.ewe.domain.model.BestSellerProduct
import com.ares.ewe.domain.model.FeaturedPlace
import com.ares.ewe.presentation.viewmodel.main.home.HomeTabViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTabScreen(
    onPlaceClick: (FeaturedPlace) -> Unit = {},
    onAdClick: (String) -> Unit = {},
    onAddressLabelClick: () -> Unit = {},
    onProductClick: (String) -> Unit = {},
    onCartClick: () -> Unit = {},
    onTrackOrderClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeTabViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val cartItemCount by viewModel.cartItemCount.collectAsState(0)
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        viewModel.loadAddresses()
    }

    val query = uiState.searchQuery.trim()
    val filteredPlaces = uiState.featuredPlaces.filter {
        query.isBlank() || it.name.contains(query, ignoreCase = true)
    }
    val filteredProducts = uiState.bestSellerProducts.filter {
        query.isBlank() || it.name.contains(query, ignoreCase = true)
    }

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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 8.dp)
                ) {
                    uiState.warningMessage?.let { msg ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(
                                Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = msg,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { viewModel.clearWarningMessage() }) {
                                    Text("Cerrar")
                                }
                            }
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        HomeHeader(
                            addressLabel = uiState.addressLabel,
                            address = uiState.address,
                            searchQuery = uiState.searchQuery,
                            onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                            onAddressLabelClick = onAddressLabelClick,
                            focusManager = focusManager
                        )
                        CartIconBadge(
                            itemCount = cartItemCount,
                            onClick = onCartClick,
                            modifier = Modifier.align(Alignment.TopEnd)
                        )
                    }
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        val restaurantsOnly = filteredPlaces.filter { !it.isService }
                        val servicesOnly = filteredPlaces.filter { it.isService }
                        val cardWidth = ((screenWidthDp - 56) / 3).dp
                        val productCardWidth = ((screenWidthDp - 52) / 2.8).toInt().dp

                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            item {
                                if (uiState.activeOrder != null) {
                                    OrderTrackingSection(
                                        activeOrder = uiState.activeOrder!!,
                                        onViewClick = { onTrackOrderClick(uiState.activeOrder!!.id) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }
                            item {
                                Text(
                                    text = "Featured",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(filteredPlaces) { place ->
                                        FeaturedPlaceCard(
                                            place = place,
                                            modifier = Modifier.width(cardWidth),
                                            onClick = { onPlaceClick(place) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                            }
                            if (filteredProducts.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Best sellers",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                                    )
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(filteredProducts) { product ->
                                            BestSellerProductCard(
                                                product = product,
                                                modifier = Modifier.width(productCardWidth),
                                                onClick = { onProductClick(product.id) }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(20.dp))
                                }
                            }
                            if (query.isNotBlank() && filteredPlaces.isEmpty() && filteredProducts.isEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(
                                        text = "No results for \"$query\"",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                }
                            }
                            if (uiState.ads.isNotEmpty()) {
                                item {
                                    AdsCarousel(
                                        ads = uiState.ads,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp)
                                            .padding(vertical = 12.dp),
                                        onAdClick = onAdClick
                                    )
                                }
                            }
                            if (restaurantsOnly.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Restaurants",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                                    )
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(restaurantsOnly) { place ->
                                            FeaturedPlaceCard(
                                                place = place,
                                                modifier = Modifier.width(cardWidth),
                                                onClick = { onPlaceClick(place) }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(20.dp))
                                }
                            }
                            if (servicesOnly.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Services",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                                    )
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(servicesOnly) { place ->
                                            FeaturedPlaceCard(
                                                place = place,
                                                modifier = Modifier.width(cardWidth),
                                                onClick = { onPlaceClick(place) }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    addressLabel: String?,
    address: String?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAddressLabelClick: () -> Unit,
    focusManager: FocusManager
) {
    val searchHints = remember {
        listOf(
            "tacos",
            "cerveza",
            "la huerta de vega",
            "pizza",
            "café",
            "restaurantes"
        )
    }
    var hintIndex by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(3500L)
            hintIndex = (hintIndex + 1) % searchHints.size
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = 8.dp)
    ) {
        Text(
            text = addressLabel ?: "Casa",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .clickable(onClick = onAddressLabelClick)
        )
        Text(
            text = address ?: "Add your address",
            style = MaterialTheme.typography.bodyMedium,
            color = if (address != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .clickable(onClick = onAddressLabelClick)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = Color.Black.copy(alpha = 0.08f),
                    spotColor = Color.Black.copy(alpha = 0.06f)
                ),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Search for \"${searchHints[hintIndex]}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
            )
        }
    }
}

@Composable
private fun FeaturedPlaceCard(
    place: FeaturedPlace,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (place.imageUrl != null) {
                    AsyncImage(
                        model = place.imageUrl,
                        contentDescription = place.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = place.name.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = place.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RatingDisplay(rate = place.rate)
            }
        }
    }
}

private val BestSellerCardCornerRadius = 16.dp

@Composable
private fun BestSellerProductCard(
    product: BestSellerProduct,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(BestSellerCardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.15f)
                    .clip(
                        RoundedCornerShape(
                            topStart = BestSellerCardCornerRadius,
                            topEnd = BestSellerCardCornerRadius
                        )
                    )
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (product.imageUrl != null) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = product.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = product.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$${String.format("%.2f", product.price)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                RatingDisplay(rate = product.rate)
            }
        }
    }
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