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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.ares.ewe.R
import com.ares.ewe.core.theme.DobbyTheme
import com.ares.ewe.presentation.ui.navigation.DobbyNavigation
import com.ares.ewe.push.ConsumerRealtimeCoordinator
import com.ares.ewe.push.OrderStatusNotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var consumerRealtimeCoordinator: ConsumerRealtimeCoordinator

    private var pendingOrderTrackingId by mutableStateOf<String?>(null)
    private var pendingProductId by mutableStateOf<String?>(null)
    private var pendingProductShopId by mutableStateOf<String?>(null)

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
                    onPendingProductNavigated = {
                        pendingProductId = null
                        pendingProductShopId = null
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        OrderStatusNotificationHelper.clearAllOrderNotifications(this)
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

        val pushType = extra("type")
        val productId = extra(OrderStatusNotificationHelper.EXTRA_PRODUCT_ID) ?: extra("product_id")
        val orderId = extra(OrderStatusNotificationHelper.EXTRA_ORDER_ID) ?: extra("order_id")

        if (
            productId != null &&
            (pushType == "product_promotion" || orderId == null)
        ) {
            pendingProductId = productId
            pendingProductShopId = extra(OrderStatusNotificationHelper.EXTRA_SHOP_ID) ?: extra("shop_id")
            intent.removeExtra(OrderStatusNotificationHelper.EXTRA_PRODUCT_ID)
            intent.removeExtra(OrderStatusNotificationHelper.EXTRA_SHOP_ID)
            extras?.remove("product_id")
            extras?.remove("shop_id")
            extras?.remove("type")
            return
        }

        if (orderId != null) {
            OrderStatusNotificationHelper.clearOrderNotifications(this, orderId)
            pendingOrderTrackingId = orderId
            intent.removeExtra(OrderStatusNotificationHelper.EXTRA_ORDER_ID)
            extras?.remove("order_id")
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
    onPendingProductNavigated: () -> Unit = {},
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
            onPendingProductNavigated = onPendingProductNavigated,
        )
    }
}