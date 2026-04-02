package com.ares.ewe.data.remote.model

import com.google.gson.annotations.SerializedName

data class ActiveOrderDto(
    @SerializedName("id") val id: String,
    @SerializedName("status") val status: String,
    @SerializedName("total") val total: Double = 0.0,
    @SerializedName("delivery_address") val deliveryAddress: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class CreateOrderItemRequest(
    @SerializedName("productId") val productId: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("price") val price: Double
)

data class CreateOrderRequest(
    @SerializedName("addressId") val addressId: String,
    @SerializedName("items") val items: List<CreateOrderItemRequest>
)

data class CreateOrderResponse(
    @SerializedName("id") val id: String,
    @SerializedName("total") val total: Double,
    @SerializedName("status") val status: String,
    @SerializedName("delivery_address") val deliveryAddress: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

// Order tracking for customer (map + bottom sheet)
data class OrderTrackingDto(
    @SerializedName("id") val id: String,
    @SerializedName("status") val status: String,
    @SerializedName("total") val total: Double = 0.0,
    @SerializedName("delivery_address") val deliveryAddress: String? = null,
    @SerializedName("lat") val lat: Double? = null,
    @SerializedName("lng") val lng: Double? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("shop_name") val shopName: String? = null,
    @SerializedName("estimated_preparation_minutes") val estimatedPreparationMinutes: Int? = null,
    @SerializedName("estimated_delivery_minutes") val estimatedDeliveryMinutes: Int? = null,
    @SerializedName("delivery_rating") val deliveryRating: Int? = null,
    @SerializedName("can_rate_delivery") val canRateDelivery: Boolean = false,
    @SerializedName("items") val items: List<OrderTrackingItemDto> = emptyList(),
    @SerializedName("delivery_man") val deliveryMan: OrderTrackingDeliveryManDto? = null
)

data class RateDeliveryRequest(
    @SerializedName("stars") val stars: Int
)

data class RateDeliveryResponse(
    @SerializedName("ok") val ok: Boolean = true
)

data class OrderTrackingItemDto(
    @SerializedName("product_name") val productName: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("price") val price: Double
)

data class OrderTrackingDeliveryManDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("celphone") val celphone: String? = null,
    @SerializedName("profile_photo_url") val profilePhotoUrl: String? = null,
    @SerializedName("lat") val lat: Double? = null,
    @SerializedName("lng") val lng: Double? = null
)
