package com.ares.ewe.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

enum class MainTab(
    val label: String,
    val icon: ImageVector
) {
    Home("Home", Icons.Filled.Home),
    Promotions("Promotions", Icons.Filled.LocalOffer),
    Favorites("Favorites", Icons.Filled.Favorite),
    Profile("Profile", Icons.Filled.Person)
}