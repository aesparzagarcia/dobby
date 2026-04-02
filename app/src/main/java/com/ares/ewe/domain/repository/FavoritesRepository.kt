package com.ares.ewe.domain.repository

import com.ares.ewe.domain.model.FavoriteProduct
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    val favorites: Flow<List<FavoriteProduct>>
    fun isFavorite(productId: String): Flow<Boolean>
    fun toggleFavorite(product: FavoriteProduct)
}
