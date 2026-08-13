package com.ares.ewe.domain.model

data class OrderTracking(
    val id: String,
    val status: String,
    /** SHOP | SERVICE_PAYMENT */
    val orderType: String = "SHOP",
    val total: Double,
    val serviceFee: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val productsSubtotal: Double = 0.0,
    val deliveryAddress: String?,
    val lat: Double?,
    val lng: Double?,
    val createdAt: String?,
    val shopName: String?,
    val shopAddress: String? = null,
    /** Restaurant pickup coordinates (for route while ASSIGNED). */
    val shopLat: Double? = null,
    val shopLng: Double? = null,
    /** Minutes the shop indicated for preparation; null if not set. */
    val estimatedPreparationMinutes: Int? = null,
    /** Minutes until delivery (courier ETA), updated while ON_DELIVERY; null if not set. */
    val estimatedDeliveryMinutes: Int? = null,
    /** ISO timestamp when courier tapped "Llegué" at the customer's address. */
    val arrivedAtCustomerAt: String? = null,
    /** 6-digit code shown to customer when courier arrives; share with driver to complete delivery. */
    val deliveryCode: String? = null,
    val deliveryRating: Int? = null,
    val canRateDelivery: Boolean = false,
    val shopRating: Int? = null,
    val canRateShop: Boolean = false,
    val items: List<OrderTrackingItem>,
    val deliveryMan: OrderTrackingDeliveryMan?
) {
    val isServicePayment: Boolean
        get() = orderType.equals("SERVICE_PAYMENT", ignoreCase = true)

    val isDelivered: Boolean get() = status.equals("DELIVERED", ignoreCase = true)

    val courierArrivedAtCustomer: Boolean
        get() = !arrivedAtCustomerAt.isNullOrBlank()

    val hasPendingRatings: Boolean
        get() = canRateDelivery || canRateShop || items.any { it.canRate }

    /** Route destination: shop while courier heads to restaurant; customer address on delivery. */
    fun routeDestinationLatLng(): Pair<Double, Double>? = when (status.uppercase()) {
        "ASSIGNED" -> shopLatLngPair()
        "ON_DELIVERY", "DELIVERED" -> customerLatLngPair()
        else -> null
    }

    val isAssignedToCourier: Boolean
        get() = status.equals("ASSIGNED", ignoreCase = true)

    val isOnDelivery: Boolean
        get() = status.equals("ON_DELIVERY", ignoreCase = true)

    /** After shop confirmed: show restaurant + customer home on the map (no courier yet). */
    val showsRestaurantAndCustomerOnMap: Boolean
        get() = status.uppercase() in PRE_COURIER_BOTH_MARKERS

    fun shopLatLngPair(): Pair<Double, Double>? {
        if (shopLat == null || shopLng == null) return null
        return shopLat to shopLng
    }

    fun customerLatLngPair(): Pair<Double, Double>? {
        if (lat == null || lng == null) return null
        return lat to lng
    }

    /** Coordinates used to fit the map camera (restaurant + home, or route + courier). */
    fun mapCameraFitPoints(): List<Pair<Double, Double>> {
        val points = mutableListOf<Pair<Double, Double>>()
        if (showsRestaurantAndCustomerOnMap) {
            shopLatLngPair()?.let { points.add(it) }
            customerLatLngPair()?.let { points.add(it) }
        } else {
            routeDestinationLatLng()?.let { points.add(it) }
        }
        val dm = deliveryMan
        if (dm?.lat != null && dm.lng != null) {
            points.add(dm.lat to dm.lng)
        }
        return points
    }

    private companion object {
        val PRE_COURIER_BOTH_MARKERS = setOf("CONFIRMED", "PREPARING", "READY_FOR_PICKUP")
    }
}

data class OrderTrackingItem(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val price: Double,
    val imageUrl: String? = null,
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
