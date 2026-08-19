package com.ares.ewe.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.ares.ewe.R
import com.ares.ewe.presentation.ui.MainActivity

object OrderStatusNotificationHelper {
    const val EXTRA_ORDER_ID = "order_id"
    const val EXTRA_PRODUCT_ID = "product_id"
    const val EXTRA_SHOP_ID = "shop_id"

    private const val CHANNEL_ID = "dobby_order_status"
    private const val CHANNEL_NAME = "Estado de pedidos"
    private const val ORDER_NOTIFICATION_ID = 1001
    private const val ORDER_TAG_PREFIX = "order-"
    private const val PROMO_TAG_PREFIX = "promo-"

    /** Matches backend `notificationGroupKey(orderId)`. */
    fun orderNotificationTag(orderId: String): String =
        "$ORDER_TAG_PREFIX${orderId.trim()}".take(64)

    fun clearOrderNotifications(context: Context, orderId: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(orderNotificationTag(orderId), ORDER_NOTIFICATION_ID)
    }

    /** Clears all in-tray order status notifications when the user returns to the app. */
    fun clearAllOrderNotifications(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.activeNotifications
            .filter { it.tag?.startsWith(ORDER_TAG_PREFIX) == true }
            .forEach { status ->
                notificationManager.cancel(status.tag, status.id)
            }
    }

    fun show(context: Context, title: String, body: String, orderId: String?) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            orderId?.let { putExtra(EXTRA_ORDER_ID, it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (orderId?.hashCode() ?: 0),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = baseBuilder(context, title, body, pendingIntent).build()

        publishOrderNotification(notificationManager, orderId, notification)
    }

    fun showProductPromotion(
        context: Context,
        title: String,
        body: String,
        productId: String,
        shopId: String?,
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_PRODUCT_ID, productId)
            shopId?.let { putExtra(EXTRA_SHOP_ID, it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            productId.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = baseBuilder(context, title, body, pendingIntent).build()

        val tag = "$PROMO_TAG_PREFIX${productId.trim()}".take(64)
        notificationManager.notify(tag, productId.hashCode(), notification)
    }

    private fun publishOrderNotification(
        notificationManager: NotificationManager,
        orderId: String?,
        notification: android.app.Notification,
    ) {
        val trimmedOrderId = orderId?.trim()?.takeIf { it.isNotEmpty() }
        if (trimmedOrderId != null) {
            notificationManager.notify(
                orderNotificationTag(trimmedOrderId),
                ORDER_NOTIFICATION_ID,
                notification,
            )
        } else {
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        }
    }

    private fun baseBuilder(
        context: Context,
        title: String,
        body: String,
        pendingIntent: PendingIntent,
    ): NotificationCompat.Builder {
        ensureChannel(context)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
            .setColor(ContextCompat.getColor(context, R.color.dobby_notification_accent))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        )
        notificationManager.createNotificationChannel(channel)
    }

    fun titleAndBodyForProductPromotion(productName: String?, discountPercent: Int?): Pair<String, String> {
        val name = productName?.trim().orEmpty().ifBlank { "Un producto" }
        val pct = discountPercent?.coerceIn(1, 100) ?: 0
        val body = if (pct > 0) {
            "$name tiene $pct% de descuento. ¡Ven y ordénalo aquí!"
        } else {
            "$name tiene una promoción. ¡Ven y ordénalo aquí!"
        }
        return "¡Promoción!" to body
    }

    private fun formatPrepMinutes(minutes: Int): String {
        if (minutes < 60) return "$minutes min"
        val h = minutes / 60
        val m = minutes % 60
        return if (m == 0) "${h} h" else "${h} h $m min"
    }

    fun titleAndBodyForStatus(
        status: String?,
        deliveryManName: String? = null,
        estimatedPreparationMinutes: Int? = null,
        orderType: String? = null,
        shopType: String? = null,
    ): Pair<String, String> {
        val servicePayment = orderType.equals("SERVICE_PAYMENT", ignoreCase = true)
        val carWash = shopType.equals("CAR_WASH", ignoreCase = true)
        return when (status) {
            "PENDING" -> "Pedido recibido" to "Tu pedido está pendiente de confirmación."
            "CONFIRMED" -> if (carWash) {
                "Servicio confirmado" to "El carwash aceptó tu servicio."
            } else {
                "Pedido confirmado" to "La tienda aceptó tu pedido."
            }
            "OUT_FOR_PICKUP" -> {
                val mins = estimatedPreparationMinutes?.takeIf { it > 0 }
                if (mins != null) {
                    "En camino" to
                        "El carwash va en camino a recoger tu carro. Llegada estimada: ${formatPrepMinutes(mins)}."
                } else {
                    "En camino" to "El carwash va en camino a recoger tu carro."
                }
            }
            "PICKED_UP" -> "Recogido" to "Tu carro ya fue recogido y va en camino al autolavado."
            "PREPARING" -> {
                if (carWash) {
                    "Lavando" to "Tu vehículo se está lavando."
                } else {
                    var body = "Tu pedido se está preparando."
                    estimatedPreparationMinutes?.takeIf { it > 0 }?.let { mins ->
                        body += " Tiempo estimado: ${formatPrepMinutes(mins)}."
                    }
                    "En preparación" to body
                }
            }
            "READY_FOR_PICKUP" -> when {
                carWash ->
                    "Secado y Aspirado" to "Tu vehículo ha pasado al área de secado y aspirado."
                servicePayment ->
                    "Buscando repartidor" to "Estamos buscando un repartidor para pagar tus servicios."
                else ->
                    "Listo para envío" to "Tu pedido está listo para salir."
            }
            "ASSIGNED" -> {
                val name = deliveryManName?.trim()
                when {
                    carWash ->
                        "Detallado" to "Tu vehículo está en el área de detallado."
                    servicePayment ->
                        if (!name.isNullOrEmpty()) {
                            "Repartidor en camino" to "El repartidor $name va en camino a pagar tus servicios."
                        } else {
                            "Repartidor asignado" to "El repartidor va en camino a pagar tus servicios."
                        }
                    !name.isNullOrEmpty() ->
                        "Repartidor en camino" to "$name va en camino a recoger tu pedido en la tienda."
                    else ->
                        "Repartidor asignado" to "Un repartidor va en camino a recoger tu pedido en la tienda."
                }
            }
            "ON_DELIVERY" -> when {
                carWash ->
                    "En camino" to "Tu vehículo va en camino a tu domicilio."
                servicePayment ->
                    "En camino" to "El repartidor va en camino a pagar tus servicios."
                else ->
                    "En camino" to "Tu pedido va rumbo a tu domicilio."
            }
            "DELIVERED" -> when {
                servicePayment ->
                    "Entregado" to "Tu pago de servicios fue entregado."
                carWash ->
                    "Servicio entregado" to "El coche ha sido entregado."
                else ->
                    "Entregado" to "Tu pedido fue entregado."
            }
            "CANCELLED" -> "Pedido cancelado" to "Tu pedido fue cancelado."
            else -> "Actualización de pedido" to (status?.let { "Estado: $it" } ?: "Hay novedades en tu pedido.")
        }
    }

    fun titleAndBodyForCourierArrived(
        deliveryManName: String? = null,
        shopType: String? = null,
        orderStatus: String? = null,
    ): Pair<String, String> {
        if (shopType.equals("CAR_WASH", ignoreCase = true)) {
            val pickup = orderStatus.equals("OUT_FOR_PICKUP", ignoreCase = true)
            return if (pickup) {
                "El carwash llegó" to
                    "El carwash llegó a recoger tu carro. Abre la Dobbi y comparte tu código de 6 dígitos."
            } else {
                "Tu coche está afuera" to
                    "Tu coche está afuera, abre la Dobbi y comparte tu código de entrega"
            }
        }
        val name = deliveryManName?.trim()
        return if (!name.isNullOrEmpty()) {
            "Repartidor afuera" to "$name está afuera con tu pedido."
        } else {
            "Repartidor afuera" to "Tu repartidor está afuera con tu pedido."
        }
    }
}
