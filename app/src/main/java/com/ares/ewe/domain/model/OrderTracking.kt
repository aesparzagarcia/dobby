package com.ares.ewe.domain.model

data class OrderTracking(
    val id: String,
    val status: String,
    val total: Double,
    val deliveryAddress: String?,
    val lat: Double?,
    val lng: Double?,
    val createdAt: String?,
    val shopName: String?,
    /** Minutes the shop indicated for preparation; null if not set. */
    val estimatedPreparationMinutes: Int? = null,
    /** Minutes until delivery (courier ETA), updated while ON_DELIVERY; null if not set. */
    val estimatedDeliveryMinutes: Int? = null,
    val items: List<OrderTrackingItem>,
    val deliveryMan: OrderTrackingDeliveryMan?
)

data class OrderTrackingItem(
    val productName: String,
    val quantity: Int,
    val price: Double
)

data class OrderTrackingDeliveryMan(
    val id: String,
    val name: String,
    val celphone: String?,
    val profilePhotoUrl: String?,
    val lat: Double?,
    val lng: Double?
)
