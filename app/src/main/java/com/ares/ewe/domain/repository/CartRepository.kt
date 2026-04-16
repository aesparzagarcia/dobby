package com.ares.ewe.domain.repository

import com.ares.ewe.domain.model.CartItem
import kotlinx.coroutines.flow.Flow

interface CartRepository {
    val items: Flow<List<CartItem>>
    fun addItem(
        productId: String,
        name: String,
        price: Double,
        quantity: Int,
        imageUrl: String? = null,
        listPrice: Double = 0.0,
        hasPromotion: Boolean = false,
        discount: Int = 0
    )
    fun removeItem(productId: String)
    fun updateQuantity(productId: String, quantity: Int)
    fun clear()
}
