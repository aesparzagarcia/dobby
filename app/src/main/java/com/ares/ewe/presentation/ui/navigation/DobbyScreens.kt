package com.ares.ewe.presentation.ui.navigation

import android.net.Uri

object DobbyScreens {
    const val Splash = "splash"
    const val Phone = "phone"
    const val Otp = "otp/{phone}/{userExists}"
    const val AddUserInfo = "add_user_info/{phone}"
    const val Home = "home"
    const val ShopDetail = "shop/{id}/{name}/{pickupLat}/{pickupLng}"
    const val ProductDetail = "product/{id}/{pickupLat}/{pickupLng}/{shopId}/{shopAvailable}"
    const val Cart = "cart"
    const val ServiceDetail = "service/{id}"
    const val AdDetail = "ad/{id}"
    const val DeliveryAddress = "delivery_address"
    const val CurrentLocationMap = "current_location_map"
    const val CurrentLocationMapWithLocation = "current_location_map/{lat}/{lng}/{address}"
    const val OrderTracking = "order_tracking/{orderId}/{returnToHistory}"
    const val ActiveOrders = "active_orders"
    const val OrderHistory = "order_history"

    fun otp(phone: String, userExists: Boolean) = "otp/${Uri.encode(phone)}/$userExists"
    fun orderTracking(orderId: String, returnToHistory: Boolean = false) =
        "order_tracking/${Uri.encode(orderId)}/$returnToHistory"
    fun currentLocationMapWithLocation(lat: Double, lng: Double, address: String) =
        "current_location_map/$lat/$lng/${Uri.encode(address)}"
    fun addUserInfo(phone: String) = "add_user_info/${Uri.encode(phone)}"
    fun shopDetail(shopId: String, shopName: String, pickupLat: Double?, pickupLng: Double?) =
        "shop/${Uri.encode(shopId)}/${Uri.encode(shopName)}/${pickupLat?.toString() ?: "none"}/${pickupLng?.toString() ?: "none"}"

    fun productDetail(
        productId: String,
        pickupLat: Double?,
        pickupLng: Double?,
        shopId: String?,
        shopAvailable: Boolean = true,
    ) = "product/${Uri.encode(productId)}/${pickupLat?.toString() ?: "none"}/${pickupLng?.toString() ?: "none"}/${Uri.encode(shopId ?: "none")}/$shopAvailable"
    fun serviceDetail(serviceId: String) = "service/${Uri.encode(serviceId)}"
    fun adDetail(adId: String) = "ad/${Uri.encode(adId)}"
}