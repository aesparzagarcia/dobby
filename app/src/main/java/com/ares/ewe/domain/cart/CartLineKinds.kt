package com.ares.ewe.domain.cart

import com.ares.ewe.domain.model.CartItem

object CartLineKinds {
    const val PRODUCT = "PRODUCT"
    const val SERVICE = "SERVICE"

    fun serviceLineId(serviceId: String, serviceNumber: String): String =
        "svc:$serviceId:${serviceNumber.trim()}"

    fun isService(item: CartItem): Boolean =
        item.lineKind.equals(SERVICE, ignoreCase = true)

    fun hasServices(items: List<CartItem>): Boolean = items.any { isService(it) }

    fun hasProducts(items: List<CartItem>): Boolean = items.any { !isService(it) }
}
