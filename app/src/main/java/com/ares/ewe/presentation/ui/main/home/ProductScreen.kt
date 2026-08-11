package com.ares.ewe.presentation.ui.main.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ares.ewe.core.delivery.DeliveryEtaEstimator
import com.ares.ewe.presentation.ui.components.CarWashSingleProductDialog
import com.ares.ewe.presentation.viewmodel.main.home.ProductViewModel

@Composable
fun ProductScreen(
    onBack: () -> Unit,
    onAddToCartClick: () -> Unit = {},
    onCartClick: () -> Unit = {},
    userLatitude: Double? = null,
    userLongitude: Double? = null,
    viewModel: ProductViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val cartItemCount by viewModel.cartItemCount.collectAsState(0)

    Box(
        modifier = Modifier
            .fillMaxSize()
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
                        Button(onClick = { viewModel.loadProduct() }) {
                            Text("Reintentar")
                        }
                    }
                }
            }
            uiState.product != null -> {
                val product = uiState.product!!
                val deliveryEtaLabel = DeliveryEtaEstimator.estimateLabelForPickup(
                    userLat = userLatitude,
                    userLng = userLongitude,
                    pickupLat = viewModel.pickupLatitude,
                    pickupLng = viewModel.pickupLongitude,
                )

                Column(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        ProductDetailHero(
                            imageUrls = product.imageUrls,
                            cartItemCount = cartItemCount,
                            isFavorite = uiState.isFavorite,
                            onBack = onBack,
                            onCartClick = onCartClick,
                            onFavoriteClick = { viewModel.toggleFavorite() },
                        )
                        ProductDetailInfoCard(
                            product = product,
                            ratingCount = uiState.ratingCount,
                            deliveryEtaLabel = deliveryEtaLabel,
                            isProductAvailable = uiState.isProductAvailable,
                            modifier = Modifier.padding(bottom = 16.dp),
                        )
                        Spacer(modifier = Modifier.height(96.dp))
                    }

                    ProductDetailBottomBar(
                        quantity = uiState.quantity,
                        lineTotal = uiState.total,
                        isProductAvailable = uiState.isProductAvailable,
                        onDecrement = { viewModel.decrementQuantity() },
                        onIncrement = { viewModel.incrementQuantity() },
                        onAddToCart = {
                            viewModel.addToCart(onAdded = onAddToCartClick)
                        },
                    )
                }
            }
        }
    }

    if (uiState.showCarWashSingleProductDialog) {
        CarWashSingleProductDialog(
            onDismiss = { viewModel.dismissCarWashSingleProductDialog() },
        )
    }
}
