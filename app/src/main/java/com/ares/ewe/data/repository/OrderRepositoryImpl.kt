package com.ares.ewe.data.repository

import com.ares.ewe.data.remote.api.DobbyApi
import com.ares.ewe.data.remote.model.CreateOrderItemRequest
import com.ares.ewe.data.remote.model.CreateOrderRequest
import com.ares.ewe.domain.model.ActiveOrder
import com.ares.ewe.domain.model.CartItem
import com.ares.ewe.domain.model.OrderTracking
import com.ares.ewe.domain.model.OrderTrackingDeliveryMan
import com.ares.ewe.domain.model.OrderTrackingItem
import com.ares.ewe.domain.repository.OrderRepository
import javax.inject.Inject

class OrderRepositoryImpl @Inject constructor(
    private val api: DobbyApi
) : OrderRepository {

    override suspend fun getActiveOrder(): Result<ActiveOrder?> = runCatching {
        val response = api.getActiveOrder()
        when (response.code()) {
            204 -> null
            200 -> response.body()?.let { dto ->
                ActiveOrder(
                    id = dto.id,
                    status = dto.status,
                    total = dto.total,
                    deliveryAddress = dto.deliveryAddress,
                    createdAt = dto.createdAt
                )
            }
            else -> throw Exception(response.message())
        }
    }

    override suspend fun getOrderTracking(orderId: String): Result<OrderTracking?> = runCatching {
        val response = api.getOrderTracking(orderId)
        when (response.code()) {
            404 -> null
            200 -> response.body()?.let { dto ->
                OrderTracking(
                    id = dto.id,
                    status = dto.status,
                    total = dto.total,
                    deliveryAddress = dto.deliveryAddress,
                    lat = dto.lat,
                    lng = dto.lng,
                    createdAt = dto.createdAt,
                    shopName = dto.shopName,
                    estimatedPreparationMinutes = dto.estimatedPreparationMinutes,
                    estimatedDeliveryMinutes = dto.estimatedDeliveryMinutes,
                    items = dto.items.map {
                        OrderTrackingItem(
                            productName = it.productName,
                            quantity = it.quantity,
                            price = it.price
                        )
                    },
                    deliveryMan = dto.deliveryMan?.let {
                        OrderTrackingDeliveryMan(
                            id = it.id,
                            name = it.name,
                            celphone = it.celphone,
                            profilePhotoUrl = it.profilePhotoUrl,
                            lat = it.lat,
                            lng = it.lng
                        )
                    }
                )
            }
            else -> throw Exception(response.body()?.toString() ?: response.message())
        }
    }

    override suspend fun createOrder(addressId: String, items: List<CartItem>): Result<Unit> = runCatching {
        val request = CreateOrderRequest(
            addressId = addressId,
            items = items.map { item ->
                CreateOrderItemRequest(
                    productId = item.productId,
                    quantity = item.quantity,
                    price = item.price
                )
            }
        )
        api.createOrder(request)
    }
}
