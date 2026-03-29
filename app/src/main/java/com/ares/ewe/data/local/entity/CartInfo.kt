package com.ares.ewe.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cart")
data class CartInfo(
    @PrimaryKey
    val productId: String,
    val name: String,
    val price: Double,
    val quantity: Int,
    val imageUrl: String? = null
)
