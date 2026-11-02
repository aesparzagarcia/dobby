package com.ares.ewe.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DobbyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var pushTokenRegistrar: PushTokenRegistrar

    @Inject
    lateinit var orderRealtimeBus: ConsumerOrderRealtimeBus

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch {
            pushTokenRegistrar.registerToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        when (message.data["type"]) {
            "order_status" -> {
                val orderId = message.data["order_id"]
                val status = message.data["status"]
                val deliveryManName = message.data["delivery_man_name"]
                val prepMinutes = message.data["estimated_preparation_minutes"]?.toIntOrNull()
                val fallback = OrderStatusNotificationHelper.titleAndBodyForStatus(
                    status,
                    deliveryManName,
                    prepMinutes,
                    message.data["order_type"],
                    message.data["shop_type"],
                )
                val title = message.notification?.title
                    ?: message.data["title"]
                    ?: fallback.first
                val body = message.notification?.body
                    ?: message.data["body"]
                    ?: fallback.second
                OrderStatusNotificationHelper.show(this, title, body, orderId)
                orderRealtimeBus.notifyOrderChanged(orderId)
            }
            "courier_arrived" -> {
                val orderId = message.data["order_id"]
                val deliveryManName = message.data["delivery_man_name"]
                val shopType = message.data["shop_type"]
                val orderStatus = message.data["order_status"]
                val fallback = OrderStatusNotificationHelper.titleAndBodyForCourierArrived(
                    deliveryManName,
                    shopType,
                    orderStatus,
                )
                val title = message.notification?.title
                    ?: message.data["title"]
                    ?: fallback.first
                val body = message.notification?.body
                    ?: message.data["body"]
                    ?: fallback.second
                OrderStatusNotificationHelper.show(this, title, body, orderId)
                orderRealtimeBus.notifyOrderChanged(orderId)
            }
            "new_restaurant" -> {
                val shopId = message.data["shop_id"]?.trim().orEmpty()
                if (shopId.isEmpty()) return
                val shopName = message.data["shop_name"]
                val title = message.notification?.title
                    ?: message.data["title"]
                    ?: "Nuevo restaurante"
                val body = message.notification?.body
                    ?: message.data["body"]
                    ?: shopName?.takeIf { it.isNotBlank() }?.let { "$it ya está en Dobby." }
                    ?: "Hay un restaurante nuevo en Dobby."
                OrderStatusNotificationHelper.showNewRestaurant(this, title, body, shopId, shopName)
            }
            "new_product" -> {
                val productId = message.data["product_id"]?.trim().orEmpty()
                if (productId.isEmpty()) return
                val shopId = message.data["shop_id"]?.trim()?.takeIf { it.isNotEmpty() }
                val productName = message.data["product_name"]
                val title = message.notification?.title ?: message.data["title"] ?: "Nuevo producto"
                val body = message.notification?.body
                    ?: message.data["body"]
                    ?: productName?.takeIf { it.isNotBlank() }?.let { "«$it» ya está disponible." }
                    ?: "Hay un producto nuevo."
                OrderStatusNotificationHelper.showNewProduct(this, title, body, productId, shopId)
            }
            "product_promotion" -> {
                val productId = message.data["product_id"]?.trim().orEmpty()
                if (productId.isEmpty()) return
                val shopId = message.data["shop_id"]?.trim()?.takeIf { it.isNotEmpty() }
                val discount = message.data["discount"]?.toIntOrNull()
                val productName = message.data["product_name"]
                val fallback = OrderStatusNotificationHelper.titleAndBodyForProductPromotion(
                    productName = productName,
                    discountPercent = discount,
                )
                val title = message.notification?.title ?: fallback.first
                val body = message.notification?.body ?: fallback.second
                OrderStatusNotificationHelper.showProductPromotion(this, title, body, productId, shopId)
            }
        }
    }
}
