package com.ares.ewe.presentation.ui.main.promotions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ares.ewe.presentation.ui.main.home.UniversalProductCard
import com.ares.ewe.presentation.viewmodel.main.promotions.PromotionsTabViewModel

@Composable
fun PromotionsTabScreen(
    onProductClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: PromotionsTabViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val productCardWidth = (screenWidthDp * 0.38f).coerceAtLeast(110.dp).coerceAtMost(150.dp)

    when {
        uiState.isLoading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        uiState.errorMessage != null -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadPromotions() }) {
                        Text("Reintentar")
                    }
                }
            }
        }
        uiState.products.isEmpty() -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No hay promociones disponibles por ahora.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        else -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(top = 12.dp),
            ) {
                Text(
                    text = "Promociones",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(uiState.products) { product ->
                        UniversalProductCard(
                            name = product.name,
                            imageUrl = product.imageUrl,
                            price = product.price,
                            rate = product.rate,
                            hasPromotion = product.hasPromotion,
                            discount = product.discount,
                            modifier = Modifier.width(productCardWidth),
                            onClick = { onProductClick(product.id) },
                        )
                    }
                }
            }
        }
    }
}