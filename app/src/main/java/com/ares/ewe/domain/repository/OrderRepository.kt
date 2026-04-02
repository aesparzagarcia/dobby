package com.ares.ewe.domain.repository

import com.ares.ewe.domain.model.ActiveOrder
import com.ares.ewe.domain.model.CartItem
import com.ares.ewe.domain.model.OrderTracking

interface OrderRepository {
    suspend fun getActiveOrder(): Result<ActiveOrder?>
    suspend fun getOrderTracking(orderId: String): Result<OrderTracking?>
    suspend fun rateDelivery(orderId: String, stars: Int): Result<Unit>
    suspend fun createOrder(addressId: String, items: List<CartItem>): Result<Unit>
}
