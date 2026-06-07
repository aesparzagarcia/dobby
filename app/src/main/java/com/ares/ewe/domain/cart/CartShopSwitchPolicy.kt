package com.ares.ewe.domain.cart

import com.ares.ewe.domain.model.CartItem

object CartShopSwitchPolicy {
    fun needsConfirmation(cartItems: List<CartItem>, targetShopId: String): Boolean {
        if (cartItems.isEmpty()) return false
        val target = targetShopId.trim()
        if (target.isEmpty()) return true
        val cartShopIds = cartItems
            .mapNotNull { it.shopId?.trim()?.takeIf { id -> id.isNotEmpty() } }
            .toSet()
        if (cartShopIds.isEmpty()) return true
        return !cartShopIds.all { it == target }
    }
}
