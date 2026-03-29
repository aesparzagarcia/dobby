package com.ares.ewe.domain.model

data class CartItem(
    val productId: String,
    val name: String,
    val price: Double,
    val quantity: Int,
    val imageUrl: String? = null
) {
    val lineTotal: Double get() = price * quantity
}
