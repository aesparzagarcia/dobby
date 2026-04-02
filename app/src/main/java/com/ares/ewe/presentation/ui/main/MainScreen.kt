package com.ares.ewe.presentation.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ares.ewe.domain.model.FeaturedPlace
import com.ares.ewe.presentation.components.FloatingBottomNavBar
import com.ares.ewe.presentation.components.MainTab
import com.ares.ewe.presentation.ui.main.favorites.FavoritesTabScreen
import com.ares.ewe.presentation.ui.main.home.HomeTabScreen
import com.ares.ewe.presentation.ui.main.profile.ProfileTabScreen
import com.ares.ewe.presentation.ui.main.promotions.PromotionsTabScreen
import com.ares.ewe.presentation.viewmodel.main.MainViewModel

@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onPlaceClick: (FeaturedPlace) -> Unit = {},
    onAdClick: (String) -> Unit = {},
    onAddressLabelClick: () -> Unit = {},
    onProductClick: (String) -> Unit = {},
    onCartClick: () -> Unit = {},
    onTrackOrderClick: (String) -> Unit = {},
    viewModel: MainViewModel = hiltViewModel()
) {
    var currentTab by remember { mutableStateOf(MainTab.Home) }
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(bottom = 10.dp)
            ) {
                when (currentTab) {
                    MainTab.Home -> HomeTabScreen(
                        onPlaceClick = onPlaceClick,
                        onAdClick = onAdClick,
                        onAddressLabelClick = onAddressLabelClick,
                        onProductClick = onProductClick,
                        onCartClick = onCartClick,
                        onTrackOrderClick = onTrackOrderClick
                    )
                    MainTab.Promotions -> PromotionsTabScreen(
                        onProductClick = onProductClick
                    )
                    MainTab.Favorites -> FavoritesTabScreen(
                        onProductClick = onProductClick
                    )
                    MainTab.Profile -> ProfileTabScreen(onLogout = { viewModel.logout(onLogout) })
                }
            }
            FloatingBottomNavBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
            )
        }
    }

}
