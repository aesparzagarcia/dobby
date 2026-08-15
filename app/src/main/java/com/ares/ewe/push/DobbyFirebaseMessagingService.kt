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
                val title = message.notification?.title ?: fallback.first
                val body = message.notification?.body ?: fallback.second
                OrderStatusNotificationHelper.show(this, title, body, orderId)
                orderRealtimeBus.notifyOrderChanged(orderId)
            }
            "courier_arrived" -> {
                val orderId = message.data["order_id"]
                val deliveryManName = message.data["delivery_man_name"]
                val fallback = OrderStatusNotificationHelper.titleAndBodyForCourierArrived(deliveryManName)
                val title = message.notification?.title ?: fallback.first
                val body = message.notification?.body ?: fallback.second
                OrderStatusNotificationHelper.show(this, title, body, orderId)
                orderRealtimeBus.notifyOrderChanged(orderId)
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
