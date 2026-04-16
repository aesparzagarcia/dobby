package com.ares.ewe.domain.model

data class CartItem(
    val productId: String,
    val name: String,
    /** Valor persistido (unitario); con promo suele ser el efectivo o el de lista según versión. */
    val price: Double,
    val quantity: Int,
    val imageUrl: String? = null,
    /** Precio de catálogo sin descuento (como `price` en API cuando hay promo). */
    val listPrice: Double = 0.0,
    val hasPromotion: Boolean = false,
    val discount: Int = 0
) {
    private val discountPercent: Int get() = discount.coerceIn(0, 100)

    /** Coincide con API: `has_promotion` + `discount` > 0. */
    val hasDiscount: Boolean get() = hasPromotion && discountPercent > 0

    /** Precio de lista (tachado). Sin `listPrice` en DB, se usa `price` como catálogo. */
    val originalUnitPrice: Double
        get() {
            if (!hasDiscount) return price
            if (listPrice > 0) return listPrice
            return price
        }

    /**
     * Precio unitario que paga el cliente (descuento aplicado).
     * Con `listPrice` > 0: `listPrice * (1 - discount/100)`.
     * Sin `listPrice`: asume que `price` es el de catálogo y aplica el %.
     */
    val chargedUnitPrice: Double
        get() {
            if (!hasDiscount) return price
            return if (listPrice > 0) {
                listPrice * (1 - discountPercent / 100.0)
            } else {
                price * (1 - discountPercent / 100.0)
            }
        }

    val lineTotal: Double get() = chargedUnitPrice * quantity
}
