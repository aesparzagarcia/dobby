package com.ares.ewe.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cart")
data class CartInfo(
    @PrimaryKey
    val productId: String,
    val name: String,
    /** Precio unitario que paga el cliente (con descuento si aplica). */
    val price: Double,
    val quantity: Int,
    val imageUrl: String? = null,
    /** Precio de lista (sin descuento); 0 si no hay promo guardada. */
    val listPrice: Double = 0.0,
    val hasPromotion: Boolean = false,
    val discount: Int = 0
)
