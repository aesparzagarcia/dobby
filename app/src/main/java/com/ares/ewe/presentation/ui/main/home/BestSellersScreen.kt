package com.ares.ewe.presentation.ui.main.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ares.ewe.presentation.viewmodel.main.home.BestSellersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BestSellersScreen(
    onBack: () -> Unit,
    onProductClick: (productId: String, pickupLat: Double?, pickupLng: Double?, shopId: String, shopAvailable: Boolean) -> Unit = { _, _, _, _, _ -> },
    onCartClick: () -> Unit = {},
    viewModel: BestSellersViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val cartItemCount by viewModel.cartItemCount.collectAsState(0)
    val filteredProducts = uiState.filteredProducts

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Más vendidos",
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    CartIconBadge(itemCount = cartItemCount, onClick = onCartClick)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                ),
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White),
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadBestSellers() }) {
                                Text("Reintentar")
                            }
                        }
                    }
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        ShopDetailSearchBar(
                            query = uiState.searchQuery,
                            onQueryChange = viewModel::onSearchQueryChange,
                        )
                        ShopDetailCategoryRow(
                            selectedCategoryId = uiState.selectedCategoryId,
                            onCategorySelected = viewModel::onCategorySelected,
                        )

                        if (filteredProducts.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = when {
                                        uiState.searchQuery.isNotBlank() ->
                                            "Ningún producto coincide con la búsqueda"
                                        uiState.selectedCategoryId != null ->
                                            "No hay productos en esta categoría"
                                        else -> "No hay productos disponibles por ahora"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 24.dp),
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    bottom = 24.dp,
                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(filteredProducts, key = { it.id }) { product ->
                                    val shopAvailable = viewModel.isProductAvailable(product)
                                    val shopId = product.shopId?.trim().orEmpty()
                                    val place = uiState.featuredPlaces.find { it.id == shopId && !it.isService }
                                    ShopDetailProductCard(
                                        product = product,
                                        isProductAvailable = shopAvailable,
                                        onClick = {
                                            onProductClick(
                                                product.id,
                                                place?.latitude,
                                                place?.longitude,
                                                shopId,
                                                shopAvailable,
                                            )
                                        },
                                        onAddClick = { viewModel.addToCart(product) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
