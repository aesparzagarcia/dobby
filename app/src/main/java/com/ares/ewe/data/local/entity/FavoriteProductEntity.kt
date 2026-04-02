package com.ares.ewe.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_products")
data class FavoriteProductEntity(
    @PrimaryKey
    val productId: String,
    val name: String,
    val price: Double,
    val imageUrl: String? = null,
    val rate: Float = 0f,
    val hasPromotion: Boolean = false,
    val discount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)
