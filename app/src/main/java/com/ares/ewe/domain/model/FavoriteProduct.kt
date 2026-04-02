package com.ares.ewe.domain.model

data class FavoriteProduct(
    val productId: String,
    val name: String,
    val price: Double,
    val imageUrl: String? = null,
    val rate: Float = 0f,
    val hasPromotion: Boolean = false,
    val discount: Int = 0,
)
