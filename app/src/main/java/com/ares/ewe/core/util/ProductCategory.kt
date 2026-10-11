package com.ares.ewe.core.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector

object ProductCategory {
    const val BEBIDAS = "bebidas"
    const val ALCOHOL = "alcohol"
    const val POSTRES = "postres"
    const val COMIDAS = "comidas"
    const val SNACKS = "snacks"
    const val MISCELANEOS = "miscelaneos"
    const val MEDICAMENTOS = "medicamentos"
    const val OTROS = "otros"

    val DEFAULT = MISCELANEOS

    data class Chip(
        val filterId: String?,
        val label: String,
        val icon: ImageVector,
    )

    /** Best sellers / vistas cruzadas: todas las categorías de filtro. */
    val allFilterChips: List<Chip> = listOf(
        Chip(null, "Todos", Icons.Default.Apps),
        Chip(BEBIDAS, "Bebidas", Icons.Default.LocalBar),
        Chip(ALCOHOL, "Alcohol", Icons.Default.LocalBar),
        Chip(COMIDAS, "Comidas", Icons.Default.Restaurant),
        Chip(POSTRES, "Postres", Icons.Default.Cake),
        Chip(MEDICAMENTOS, "Medicamentos", Icons.Default.LocalPharmacy),
        Chip(OTROS, "Otros", Icons.Default.MoreHoriz),
    )

    /** Filtros del detalle de tienda según tipo (`SHOP` vs `RESTAURANT`). */
    fun filterChips(shopType: String?): List<Chip> {
        val isShop = shopType?.trim()?.equals("SHOP", ignoreCase = true) == true
        val chips = mutableListOf(
            Chip(null, "Todos", Icons.Default.Apps),
            Chip(BEBIDAS, "Bebidas", Icons.Default.LocalBar),
            Chip(ALCOHOL, "Alcohol", Icons.Default.LocalBar),
        )
        if (isShop) {
            chips += Chip(MEDICAMENTOS, "Medicamentos", Icons.Default.LocalPharmacy)
        } else {
            chips += Chip(COMIDAS, "Comidas", Icons.Default.Restaurant)
            chips += Chip(POSTRES, "Postres", Icons.Default.Cake)
        }
        chips += Chip(OTROS, "Otros", Icons.Default.MoreHoriz)
        return chips
    }

    fun normalize(raw: String?): String {
        val slug = raw?.trim()?.lowercase().orEmpty()
        return when (slug) {
            BEBIDAS, ALCOHOL, POSTRES, COMIDAS, SNACKS, MISCELANEOS, MEDICAMENTOS -> slug
            else -> DEFAULT
        }
    }

    fun matchesFilter(productCategory: String?, filterId: String?): Boolean {
        if (filterId == null) return true
        val normalized = normalize(productCategory)
        return when (filterId) {
            OTROS -> normalized == SNACKS || normalized == MISCELANEOS
            else -> normalized == filterId
        }
    }
}
