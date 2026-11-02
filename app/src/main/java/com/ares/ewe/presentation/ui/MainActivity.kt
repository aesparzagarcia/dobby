package com.ares.ewe.presentation.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.ares.ewe.R
import com.ares.ewe.core.theme.DobbyTheme
import com.ares.ewe.presentation.ui.navigation.DobbyNavigation
import com.ares.ewe.push.ConsumerOrderRealtimeBus
import com.ares.ewe.push.ConsumerRealtimeCoordinator
import com.ares.ewe.push.OrderStatusNotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var consumerRealtimeCoordinator: ConsumerRealtimeCoordinator

    @Inject
    lateinit var orderRealtimeBus: ConsumerOrderRealtimeBus

    private var pendingOrderTrackingId by mutableStateOf<String?>(null)
    private var pendingProductId by mutableStateOf<String?>(null)
    private var pendingProductShopId by mutableStateOf<String?>(null)
    private var pendingProductNonce by mutableIntStateOf(0)
    private var pendingShopId by mutableStateOf<String?>(null)
    private var pendingShopName by mutableStateOf<String?>(null)
    private var pendingShopNonce by mutableIntStateOf(0)

    /** True tras [onStop] (app en background u otra activity encima). */
    private var wasStopped = false

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ ->
        syncRealtime()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Dobby)
        super.onCreate(savedInstanceState)
        consumeOrderIdFromIntent(intent)
        enableEdgeToEdge()
        // Light app UI: dark status/nav bar icons even when the phone is in system dark mode
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        requestNotifPermissionIfNeeded()
        setContent {
            DobbyTheme(dynamicColor = false) {
                DobbyApp(
                    pendingOrderTrackingId = pendingOrderTrackingId,
                    onPendingOrderTrackingNavigated = { pendingOrderTrackingId = null },
                    pendingProductId = pendingProductId,
                    pendingProductShopId = pendingProductShopId,
                    pendingProductNonce = pendingProductNonce,
                    onPendingProductNavigated = {
                        pendingProductId = null
                        pendingProductShopId = null
                    },
                    pendingShopId = pendingShopId,
                    pendingShopName = pendingShopName,
                    pendingShopNonce = pendingShopNonce,
                    onPendingShopNavigated = {
                        pendingShopId = null
                        pendingShopName = null
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        consumeOrderIdFromIntent(intent)
        OrderStatusNotificationHelper.clearAllOrderNotifications(this)
    }

    override fun onStop() {
        wasStopped = true
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        if (wasStopped) {
            wasStopped = false
            // Background FCM often never hits the realtime bus; force refresh on return.
            consumerRealtimeCoordinator.resumeAfterBackground()
            orderRealtimeBus.notifyOrderChanged()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeOrderIdFromIntent(intent)
    }

    private fun consumeOrderIdFromIntent(intent: Intent?) {
        if (intent == null) return
        val extras = intent.extras

        fun extra(key: String): String? =
            intent.getStringExtra(key)?.trim()?.takeIf { it.isNotEmpty() }
                ?: extras?.getString(key)?.trim()?.takeIf { it.isNotEmpty() }

        val data = intent.data
        if (data?.scheme == "dobby" && data.host == "push") {
            val kind = data.pathSegments.getOrNull(0)
            val id = data.pathSegments.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
            if (kind == "shop" && id != null) {
                pendingShopId = id
                pendingShopName = data.getQueryParameter("name")?.trim()?.takeIf { it.isNotEmpty() }
                pendingShopNonce += 1
                intent.data = null
                clearPushExtras(intent, extras)
                return
            }
            if (kind == "product" && id != null) {
                pendingProductId = id
                pendingProductShopId = data.getQueryParameter("shopId")?.trim()?.takeIf { it.isNotEmpty() }
                pendingProductNonce += 1
                intent.data = null
                clearPushExtras(intent, extras)
                return
            }
        }

        val pushType = extra("type") ?: data?.getQueryParameter("type")
        val productId = extra(OrderStatusNotificationHelper.EXTRA_PRODUCT_ID) ?: extra("product_id")
        val orderId = extra(OrderStatusNotificationHelper.EXTRA_ORDER_ID) ?: extra("order_id")
        val openScreen = extra("open_screen")

        if (pushType == "new_restaurant" || (openScreen == "shop_detail" && productId == null)) {
            val shopId = extra(OrderStatusNotificationHelper.EXTRA_SHOP_ID) ?: extra("shop_id")
            if (shopId != null) {
                pendingShopId = shopId
                pendingShopName = extra(OrderStatusNotificationHelper.EXTRA_SHOP_NAME) ?: extra("shop_name")
                pendingShopNonce += 1
                clearPushExtras(intent, extras)
            }
            return
        }

        if (
            productId != null &&
            (pushType == "product_promotion" || pushType == "new_product" || orderId == null)
        ) {
            pendingProductId = productId
            pendingProductShopId = extra(OrderStatusNotificationHelper.EXTRA_SHOP_ID) ?: extra("shop_id")
            pendingProductNonce += 1
            clearPushExtras(intent, extras)
            return
        }

        if (orderId != null) {
            OrderStatusNotificationHelper.clearOrderNotifications(this, orderId)
            pendingOrderTrackingId = orderId
            intent.removeExtra(OrderStatusNotificationHelper.EXTRA_ORDER_ID)
            extras?.remove("order_id")
        }
    }

    private fun clearPushExtras(intent: Intent, extras: android.os.Bundle?) {
        listOf(
            OrderStatusNotificationHelper.EXTRA_SHOP_ID,
            OrderStatusNotificationHelper.EXTRA_SHOP_NAME,
            OrderStatusNotificationHelper.EXTRA_PRODUCT_ID,
            OrderStatusNotificationHelper.EXTRA_PUSH_TYPE,
            "shop_id",
            "shop_name",
            "product_id",
            "type",
            "open_screen",
        ).forEach { key ->
            intent.removeExtra(key)
            extras?.remove(key)
        }
    }

    private fun requestNotifPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            syncRealtime()
            return
        }
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED -> syncRealtime()
            else -> notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun syncRealtime() {
        lifecycleScope.launch {
            consumerRealtimeCoordinator.onSessionReady()
        }
    }
}

@Composable
fun DobbyApp(
    pendingOrderTrackingId: String? = null,
    onPendingOrderTrackingNavigated: () -> Unit = {},
    pendingProductId: String? = null,
    pendingProductShopId: String? = null,
    pendingProductNonce: Int = 0,
    onPendingProductNavigated: () -> Unit = {},
    pendingShopId: String? = null,
    pendingShopName: String? = null,
    pendingShopNonce: Int = 0,
    onPendingShopNavigated: () -> Unit = {},
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize(),
    ) {
        DobbyNavigation(
            pendingOrderTrackingId = pendingOrderTrackingId,
            onPendingOrderTrackingNavigated = onPendingOrderTrackingNavigated,
            pendingProductId = pendingProductId,
            pendingProductShopId = pendingProductShopId,
            pendingProductNonce = pendingProductNonce,
            onPendingProductNavigated = onPendingProductNavigated,
            pendingShopId = pendingShopId,
            pendingShopName = pendingShopName,
            pendingShopNonce = pendingShopNonce,
            onPendingShopNavigated = onPendingShopNavigated,
        )
    }
}