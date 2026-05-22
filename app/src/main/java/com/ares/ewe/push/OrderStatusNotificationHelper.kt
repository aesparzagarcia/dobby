package com.ares.ewe.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ares.ewe.R
import com.ares.ewe.presentation.ui.MainActivity

object OrderStatusNotificationHelper {
    const val EXTRA_ORDER_ID = "order_id"
    const val EXTRA_PRODUCT_ID = "product_id"
    const val EXTRA_SHOP_ID = "shop_id"

    private const val CHANNEL_ID = "dobby_order_status"
    private const val CHANNEL_NAME = "Estado de pedidos"

    fun show(context: Context, title: String, body: String, orderId: String?) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH,
            )
            notificationManager.createNotificationChannel(channel)
        }

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

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationId = orderId?.hashCode() ?: System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notification)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH,
            )
            notificationManager.createNotificationChannel(channel)
        }

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

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(productId.hashCode(), notification)
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
    ): Pair<String, String> = when (status) {
        "PENDING" -> "Pedido recibido" to "Tu pedido está pendiente de confirmación."
        "CONFIRMED" -> "Pedido confirmado" to "La tienda aceptó tu pedido."
        "PREPARING" -> {
            var body = "Tu pedido se está preparando."
            estimatedPreparationMinutes?.takeIf { it > 0 }?.let { mins ->
                body += " Tiempo estimado: ${formatPrepMinutes(mins)}."
            }
            "En preparación" to body
        }
        "READY_FOR_PICKUP" -> "Listo para envío" to "Tu pedido está listo para salir."
        "ASSIGNED" -> {
            val name = deliveryManName?.trim()
            if (!name.isNullOrEmpty()) {
                "Repartidor en camino" to "$name va en camino a recoger tu pedido en la tienda."
            } else {
                "Repartidor asignado" to "Un repartidor va en camino a recoger tu pedido en la tienda."
            }
        }
        "ON_DELIVERY" -> "En camino" to "Tu pedido va rumbo a tu domicilio."
        "DELIVERED" -> "Entregado" to "Tu pedido fue entregado."
        "CANCELLED" -> "Pedido cancelado" to "Tu pedido fue cancelado."
        else -> "Actualización de pedido" to (status?.let { "Estado: $it" } ?: "Hay novedades en tu pedido.")
    }

    fun titleAndBodyForCourierArrived(deliveryManName: String? = null): Pair<String, String> {
        val name = deliveryManName?.trim()
        return if (!name.isNullOrEmpty()) {
            "Repartidor afuera" to "$name está afuera con tu pedido."
        } else {
            "Repartidor afuera" to "Tu repartidor está afuera con tu pedido."
        }
    }
}
