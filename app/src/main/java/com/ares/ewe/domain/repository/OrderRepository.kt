package com.ares.ewe.domain.repository

import com.ares.ewe.domain.model.ActiveOrder
import com.ares.ewe.domain.model.CartItem
import com.ares.ewe.domain.model.OrderHistoryItem
import com.ares.ewe.domain.model.OrderTracking

interface OrderRepository {
    suspend fun getActiveOrders(): Result<List<ActiveOrder>>
    suspend fun getOrderHistory(): Result<List<OrderHistoryItem>>
    suspend fun getOrderTracking(orderId: String): Result<OrderTracking?>
    suspend fun rateDelivery(orderId: String, stars: Int): Result<Unit>
    suspend fun rateShop(orderId: String, stars: Int): Result<Unit>
    suspend fun rateProduct(orderId: String, productId: String, stars: Int): Result<Unit>
    suspend fun createOrder(addressId: String, items: List<CartItem>, deliveryFee: Double): Result<Unit>
    suspend fun createServicePaymentOrder(
        addressId: String,
        items: List<CartItem>,
        deliveryFee: Double,
    ): Result<Unit>
}
