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
    Home("Inicio", Icons.Filled.Home),
    Promotions("Promociones", Icons.Filled.LocalOffer),
    Favorites("Favoritos", Icons.Filled.Favorite),
    Profile("Perfil", Icons.Filled.Person)
}