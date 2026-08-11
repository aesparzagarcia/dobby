package com.ares.ewe.domain.cart

import com.ares.ewe.domain.model.CartItem

/**
 * Autolavado (CAR_WASH) shops only allow one product (quantity 1) per purchase.
 */
object CartCarWashSingleProductPolicy {
    const val MESSAGE = "Este comercio acepta un solo producto por compra."

    fun isCarWash(shopType: String?): Boolean =
        shopType?.trim()?.equals("CAR_WASH", ignoreCase = true) == true

    fun blocksAdd(
        shopType: String?,
        cartItems: List<CartItem>,
        productId: String,
        shopId: String?,
        quantityToAdd: Int,
    ): Boolean {
        if (!isCarWash(shopType)) return false
        if (quantityToAdd > 1) return true
        val sid = shopId?.trim().orEmpty()
        if (sid.isEmpty()) {
            // Without shopId, still cap total cart size for safety when type is known
            return cartItems.isNotEmpty() || quantityToAdd > 1
        }
        val fromShop = cartItems.filter { it.shopId?.trim() == sid }
        if (fromShop.isEmpty()) return false
        val existing = fromShop.find { it.productId == productId }
        return if (existing != null) {
            existing.quantity + quantityToAdd > 1
        } else {
            true
        }
    }
}
