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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.ares.ewe.domain.model.FeaturedPlace
import com.ares.ewe.presentation.viewmodel.main.home.FeaturedPlacesViewModel

@Composable
fun FeaturedPlacesScreen(
    onBack: () -> Unit,
    onPlaceClick: (FeaturedPlace) -> Unit = {},
    viewModel: FeaturedPlacesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val filteredPlaces = uiState.filteredPlaces

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding(),
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
                        Button(onClick = { viewModel.loadFeaturedPlaces() }) {
                            Text("Reintentar")
                        }
                    }
                }
            }
            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    FeaturedPlacesHeader(onBack = onBack)

                    ShopDetailSearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = viewModel::onSearchQueryChange,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    HomeCategoryRow(
                        selected = uiState.selectedCategory,
                        onCategorySelected = viewModel::onCategorySelected,
                        includeOffers = false,
                        scale = 0.9f,
                        spreadToEdges = true,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (filteredPlaces.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = when {
                                    uiState.searchQuery.isNotBlank() ->
                                        "Ningún lugar coincide con la búsqueda"
                                    uiState.selectedCategory != HomeQuickCategory.All ->
                                        "No hay lugares en esta categoría"
                                    else -> "No hay restaurantes ni servicios disponibles"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 24.dp),
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 24.dp,
                            ),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(filteredPlaces, key = { it.id }) { place ->
                                HomeFeaturedPlaceCard(
                                    place = place,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { onPlaceClick(place) },
                                    cardScale = 1f,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
