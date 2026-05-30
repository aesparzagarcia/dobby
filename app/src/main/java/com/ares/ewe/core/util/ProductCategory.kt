package com.ares.ewe.core.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector

object ProductCategory {
    const val BEBIDAS = "bebidas"
    const val POSTRES = "postres"
    const val COMIDAS = "comidas"
    const val SNACKS = "snacks"
    const val MISCELANEOS = "miscelaneos"
    const val OTROS = "otros"

    val DEFAULT = MISCELANEOS

    data class Chip(
        val filterId: String?,
        val label: String,
        val icon: ImageVector,
    )

    val filterChips: List<Chip> = listOf(
        Chip(null, "Todos", Icons.Default.Apps),
        Chip(BEBIDAS, "Bebidas", Icons.Default.LocalBar),
        Chip(COMIDAS, "Comidas", Icons.Default.Restaurant),
        Chip(POSTRES, "Postres", Icons.Default.Cake),
        Chip(OTROS, "Otros", Icons.Default.MoreHoriz),
    )

    fun normalize(raw: String?): String {
        val slug = raw?.trim()?.lowercase().orEmpty()
        return when (slug) {
            BEBIDAS, POSTRES, COMIDAS, SNACKS, MISCELANEOS -> slug
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
