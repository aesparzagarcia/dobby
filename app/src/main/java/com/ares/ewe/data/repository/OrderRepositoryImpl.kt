package com.ares.ewe.data.repository

import com.ares.ewe.BuildConfig
import com.ares.ewe.data.remote.api.DobbyApi
import com.ares.ewe.data.remote.model.CreateOrderItemRequest
import com.ares.ewe.data.remote.model.CreateOrderRequest
import com.ares.ewe.data.remote.model.RateDeliveryRequest
import com.ares.ewe.data.remote.model.RateProductEntryDto
import com.ares.ewe.data.remote.model.RateProductsRequest
import com.ares.ewe.domain.model.ActiveOrder
import retrofit2.Response
import com.ares.ewe.domain.model.ActiveOrderProductLine
import com.ares.ewe.domain.model.CartItem
import com.ares.ewe.domain.model.OrderHistoryItem
import com.ares.ewe.domain.model.OrderTracking
import com.ares.ewe.domain.model.OrderTrackingDeliveryMan
import com.ares.ewe.domain.model.OrderTrackingItem
import com.ares.ewe.domain.repository.OrderRepository
import javax.inject.Inject

class OrderRepositoryImpl @Inject constructor(
    private val api: DobbyApi
) : OrderRepository {

    override suspend fun getOrderHistory(): Result<List<OrderHistoryItem>> = runCatching {
        val response = api.getOrderHistory()
        when (response.code()) {
            204 -> emptyList()
            200 -> response.body().orEmpty().map { dto ->
                OrderHistoryItem(
                    id = dto.id,
                    status = dto.status,
                    total = dto.total,
                    createdAt = dto.createdAt,
                    shopName = dto.shopName,
                    productLines = dto.items.orEmpty().map { item ->
                        ActiveOrderProductLine(
                            name = item.productName,
                            quantity = item.quantity,
                        )
                    },
                )
            }
            else -> throw Exception(response.message())
        }
    }

    override suspend fun getActiveOrders(): Result<List<ActiveOrder>> = runCatching {
        val response = api.getActiveOrders()
        when (response.code()) {
            204 -> emptyList()
            200 -> response.body().orEmpty().map { dto ->
                ActiveOrder(
                    id = dto.id,
                    status = dto.status,
                    total = dto.total,
                    deliveryAddress = dto.deliveryAddress,
                    createdAt = dto.createdAt,
                    productLines = dto.items.orEmpty().map { item ->
                        ActiveOrderProductLine(
                            name = item.productName,
                            quantity = item.quantity,
                        )
                    },
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
                    deliveryFee = dto.deliveryFee,
                    productsSubtotal = dto.productsSubtotal,
                    deliveryAddress = dto.deliveryAddress,
                    lat = dto.lat,
                    lng = dto.lng,
                    createdAt = dto.createdAt,
                    shopName = dto.shopName,
                    shopAddress = dto.shopAddress,
                    shopLat = dto.shopLat,
                    shopLng = dto.shopLng,
                    estimatedPreparationMinutes = dto.estimatedPreparationMinutes,
                    estimatedDeliveryMinutes = dto.estimatedDeliveryMinutes,
                    arrivedAtCustomerAt = dto.arrivedAtCustomerAt,
                    deliveryCode = dto.deliveryCode?.trim()?.takeIf { it.isNotEmpty() },
                    deliveryRating = dto.deliveryRating,
                    canRateDelivery = dto.canRateDelivery,
                    shopRating = dto.shopRating,
                    canRateShop = dto.canRateShop,
                    items = dto.items.map {
                        OrderTrackingItem(
                            productId = it.productId,
                            productName = it.productName,
                            quantity = it.quantity,
                            price = it.price,
                            imageUrl = it.imageUrl.toOrderTrackingImageUrl(),
                            rating = it.rating,
                            canRate = it.canRate
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

    override suspend fun rateDelivery(orderId: String, stars: Int): Result<Unit> =
        postRating { api.rateDelivery(orderId, RateDeliveryRequest(stars)) }

    override suspend fun rateShop(orderId: String, stars: Int): Result<Unit> =
        postRating { api.rateShop(orderId, RateDeliveryRequest(stars)) }

    override suspend fun rateProduct(orderId: String, productId: String, stars: Int): Result<Unit> =
        postRating {
            api.rateProducts(
                orderId,
                RateProductsRequest(listOf(RateProductEntryDto(productId, stars)))
            )
        }

    private suspend fun postRating(call: suspend () -> Response<com.ares.ewe.data.remote.model.RateDeliveryResponse>): Result<Unit> =
        runCatching {
            val response = call()
            when (response.code()) {
                in 200..299 -> Unit
                else -> {
                    val err = response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                    throw Exception(err ?: response.message())
                }
            }
        }

    override suspend fun createOrder(
        addressId: String,
        items: List<CartItem>,
        deliveryFee: Double
    ): Result<Unit> = runCatching {
        val request = CreateOrderRequest(
            addressId = addressId,
            deliveryFee = deliveryFee,
            items = items.map { item ->
                CreateOrderItemRequest(
                    productId = item.productId,
                    quantity = item.quantity,
                    price = item.chargedUnitPrice
                )
            }
        )
        api.createOrder(request)
    }
}

private fun String?.toOrderTrackingImageUrl(): String? {
    if (this.isNullOrBlank()) return null
    if (startsWith("http")) return this
    val base = BuildConfig.BASE_URL.removeSuffix("api/").trimEnd('/')
    return "$base$this"
}
