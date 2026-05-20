package com.ares.ewe.domain.model

data class OrderTracking(
    val id: String,
    val status: String,
    val total: Double,
    val deliveryFee: Double = 0.0,
    val productsSubtotal: Double = 0.0,
    val deliveryAddress: String?,
    val lat: Double?,
    val lng: Double?,
    val createdAt: String?,
    val shopName: String?,
    /** Minutes the shop indicated for preparation; null if not set. */
    val estimatedPreparationMinutes: Int? = null,
    /** Minutes until delivery (courier ETA), updated while ON_DELIVERY; null if not set. */
    val estimatedDeliveryMinutes: Int? = null,
    /** ISO timestamp when courier tapped "Llegué" at the customer's address. */
    val arrivedAtCustomerAt: String? = null,
    val deliveryRating: Int? = null,
    val canRateDelivery: Boolean = false,
    val shopRating: Int? = null,
    val canRateShop: Boolean = false,
    val items: List<OrderTrackingItem>,
    val deliveryMan: OrderTrackingDeliveryMan?
) {
    val isDelivered: Boolean get() = status.equals("DELIVERED", ignoreCase = true)

    val courierArrivedAtCustomer: Boolean
        get() = !arrivedAtCustomerAt.isNullOrBlank()

    val hasPendingRatings: Boolean
        get() = canRateDelivery || canRateShop || items.any { it.canRate }
}

data class OrderTrackingItem(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val price: Double,
    val rating: Int? = null,
    val canRate: Boolean = false
)

data class OrderTrackingDeliveryMan(
    val id: String,
    val name: String,
    val celphone: String?,
    val profilePhotoUrl: String?,
    val lat: Double?,
    val lng: Double?
)
