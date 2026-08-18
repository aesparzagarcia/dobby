package com.ares.ewe.presentation.ui.main.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ares.ewe.presentation.ui.components.FloatingScreenHeader
import com.ares.ewe.presentation.viewmodel.main.home.OrderTrackingViewModel
import com.ares.ewe.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlin.math.abs
import kotlin.math.max

/**
 * Zoom level when only one marker is shown (Google Maps: ~1 world … ~21 building).
 * Not used when both courier and destination exist — see [MAP_BOUNDS_EXPANSION_FACTOR].
 */
private const val DEFAULT_ZOOM = 14.25f
/** Slightly more zoomed out for Recogido / Lavando / Secado y Aspirado (single marker). */
private const val WASH_PHASE_ZOOM = 13.2f
/** Edge padding (px) when fitting courier + destination; larger = more zoom out. */
private const val MAP_BOUNDS_PADDING_PX = 150
private const val WASH_PHASE_BOUNDS_PADDING_PX = 220
/**
 * Expands the fitted bounds before applying the camera (1.0 = tight fit).
 * Increase (e.g. 1.5) to zoom out more when repartidor + destino are visible.
 */
private const val MAP_BOUNDS_EXPANSION_FACTOR = 1.35f
/** Extra zoom-out while car is at the shop (Recogido / Lavando / Secado y Aspirado). */
private const val WASH_PHASE_BOUNDS_EXPANSION_FACTOR = 1.85f
/** Minimum lat/lng span so [CameraUpdateFactory.newLatLngBounds] does not zoom to world view. */
private const val MIN_BOUNDS_SPAN_DEGREES = 0.004
private const val MARKER_ICON_SIZE_DP = 48
private val FALLBACK_LATLNG = LatLng(20.6507582, -103.7029606)
private val RoutePolylineColor = Color(0xFF0D0D0D)

private fun isWashPhaseStatus(status: String?): Boolean =
    when (status?.uppercase()) {
        "PICKED_UP", "PREPARING", "READY_FOR_PICKUP" -> true
        else -> false
    }
private fun isValidMapCoordinate(lat: Double, lng: Double): Boolean {
    if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return false
    if (abs(lat) < 1e-4 && abs(lng) < 1e-4) return false
    return true
}

private fun LatLng.takeIfValid(): LatLng? =
    if (isValidMapCoordinate(latitude, longitude)) this else null

/** Ensures non-zero span before [CameraUpdateFactory.newLatLngBounds] (avoids world-level zoom). */
private fun LatLngBounds.withMinimumSpan(minDelta: Double = MIN_BOUNDS_SPAN_DEGREES): LatLngBounds {
    val latSpan = northeast.latitude - southwest.latitude
    val lngSpan = northeast.longitude - southwest.longitude
    if (latSpan >= minDelta && lngSpan >= minDelta) return this
    val center = center
    val halfLat = max(latSpan, minDelta) / 2.0
    val halfLng = max(lngSpan, minDelta) / 2.0
    return LatLngBounds(
        LatLng(center.latitude - halfLat, center.longitude - halfLng),
        LatLng(center.latitude + halfLat, center.longitude + halfLng),
    )
}

/** Widens [bounds] so [CameraUpdateFactory.newLatLngBounds] shows more map around the markers. */
private fun LatLngBounds.expandForZoomOut(factor: Float): LatLngBounds {
    if (factor <= 1f) return this
    val extra = (factor - 1f) / 2f
    val latPad = (northeast.latitude - southwest.latitude) * extra
    val lngPad = (northeast.longitude - southwest.longitude) * extra
    return LatLngBounds(
        LatLng(southwest.latitude - latPad, southwest.longitude - lngPad),
        LatLng(northeast.latitude + latPad, northeast.longitude + lngPad),
    )
}

private fun bitmapDescriptorFromRes(context: Context, resId: Int, sizeDp: Int = MARKER_ICON_SIZE_DP): BitmapDescriptor? {
    val drawable = ContextCompat.getDrawable(context, resId) ?: return null
    val density = context.resources.displayMetrics.density
    val sizePx = (sizeDp * density).toInt()
    drawable.setBounds(0, 0, sizePx, sizePx)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

/** Max height for in-progress order detail panel (map stays visible above). */
private const val ORDER_TRACKING_SHEET_MAX_HEIGHT_FRACTION = 0.78f
/** Delivered sheet: wrap content, cap height so the map stays visible without empty white space. */
private const val ORDER_TRACKING_DELIVERED_SHEET_MAX_HEIGHT_FRACTION = 0.68f

@Composable
fun OrderTrackingScreen(
    onBack: () -> Unit,
    onFinish: () -> Unit,
    viewModel: OrderTrackingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val routePoints = uiState.routePoints
    val usingStraightLineRoute = uiState.usingStraightLineRoute
    var sheetCollapsed by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val deliveryIcon = remember(context) { bitmapDescriptorFromRes(context, R.drawable.ic_delivery) }

    val tracking = uiState.tracking
    val isCarWash = tracking?.isCarWash == true
    val shopMarkerIcon = remember(context, isCarWash) {
        val resId = if (isCarWash) R.drawable.ic_car_wash else R.drawable.ic_shop
        bitmapDescriptorFromRes(context, resId)
            ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
    }
    val houseMarkerIcon = remember(context) {
        bitmapDescriptorFromRes(context, R.drawable.ic_house)
            ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
    }
    val showsRestaurantAndCustomer = tracking?.showsRestaurantAndCustomerOnMap == true
    val shopLatLng = tracking?.shopLatLngPair()?.let { (lat, lng) ->
        LatLng(lat, lng).takeIfValid()
    }
    val customerLatLng = tracking?.customerLatLngPair()?.let { (lat, lng) ->
        LatLng(lat, lng).takeIfValid()
    }
    // Carwash: siempre mostrar el local (origen del vehículo) cuando hay coordenadas.
    // En ruta, el marcador del vehículo (deliveryMan) es la posición en vivo / del local.
    val deliveryManLatLng = tracking?.deliveryMan?.let { dm ->
        if (dm.lat != null && dm.lng != null) LatLng(dm.lat, dm.lng).takeIfValid() else null
    } ?: if (isCarWash && (
            tracking?.isOnDelivery == true ||
                tracking?.isAssignedToCourier == true ||
                tracking?.isOutForPickup == true ||
                tracking?.isPickedUp == true
            )
    ) {
        if (tracking?.isPickedUp == true) customerLatLng else shopLatLng
    } else {
        null
    }
    // Carwash: keep the shop pin visible; the vehicle pin is separate ("Tu vehículo").
    // Only hide the shop pin when it would stack exactly on the live vehicle.
    val shopLatLngForMap = when {
        isCarWash && deliveryManLatLng != null -> {
            val shop = shopLatLng
            val vehicle = deliveryManLatLng
            if (
                shop != null &&
                (
                    abs(shop.latitude - vehicle.latitude) > 1e-4 ||
                    abs(shop.longitude - vehicle.longitude) > 1e-4
                )
            ) {
                shop
            } else if (
                shop != null &&
                tracking?.isOnDelivery != true &&
                tracking?.isAssignedToCourier != true &&
                tracking?.isOutForPickup != true &&
                tracking?.isPickedUp != true
            ) {
                // Before En camino / Recogido: shop pin alone is the origin (no separate vehicle yet).
                shop
            } else {
                // Same spot as vehicle while in route — show only "Tu vehículo".
                null
            }
        }
        isCarWash -> shopLatLng
        showsRestaurantAndCustomer -> shopLatLng
        else -> null
    }
    val customerLatLngForMap = when {
        // Durante recolección no mostramos la casa; sí desde Recogido en adelante.
        isCarWash && tracking?.isOutForPickup == true -> null
        isCarWash && (
            showsRestaurantAndCustomer ||
                tracking?.isOnDelivery == true ||
                tracking?.isAssignedToCourier == true ||
                tracking?.isPickedUp == true
            ) ->
            customerLatLng
        showsRestaurantAndCustomer -> customerLatLng
        else -> null
    }
    val destinationLatLng = if (tracking != null && !showsRestaurantAndCustomer && !isCarWash) {
        tracking.routeDestinationLatLng()?.let { (lat, lng) ->
            LatLng(lat, lng).takeIfValid()
        }
    } else {
        null
    }
    val destinationMarkerIcon = if (tracking?.isAssignedToCourier == true) shopMarkerIcon else houseMarkerIcon
    val destinationMarkerTitle = if (tracking?.isAssignedToCourier == true) {
        if (isCarWash) "Autolavado" else "Restaurante"
    } else {
        "Tu dirección de entrega"
    }
    val destinationMarkerSnippet = if (tracking?.isAssignedToCourier == true) {
        tracking.shopName
    } else {
        tracking?.deliveryAddress
    }
    val carMarkerIcon = remember(context) {
        bitmapDescriptorFromRes(context, R.drawable.ic_car)
            ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
    }
    val vehicleMarkerIcon = if (isCarWash) carMarkerIcon else deliveryIcon
    val vehicleMarkerTitle = if (isCarWash) "Tu vehículo" else "Repartidor"

    // Sync position during composition — LaunchedEffect alone left the pin at FALLBACK
    // (off-camera) until the next frame, so the vehicle never appeared near the route.
    val deliveryManMarkerState = remember { MarkerState(deliveryManLatLng ?: FALLBACK_LATLNG) }
    deliveryManLatLng?.let { deliveryManMarkerState.position = it }

    var mapLoaded by remember { mutableStateOf(false) }
    var lastCameraFitKey by remember { mutableStateOf<String?>(null) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(FALLBACK_LATLNG, DEFAULT_ZOOM)
    }

    val mapFitPoints = remember(tracking?.id, tracking?.status, shopLatLng, customerLatLng, destinationLatLng, deliveryManLatLng) {
        tracking?.mapCameraFitPoints()
            ?.mapNotNull { (lat, lng) -> LatLng(lat, lng).takeIfValid() }
            .orEmpty()
    }

    val cameraFitKey = remember(mapFitPoints) {
        mapFitPoints.joinToString(";") { "${it.latitude},${it.longitude}" }
    }

    LaunchedEffect(tracking?.status) {
        lastCameraFitKey = null
    }

    LaunchedEffect(cameraFitKey, mapLoaded, uiState.isLoading, tracking?.id) {
        if (uiState.isLoading || tracking == null || !mapLoaded) return@LaunchedEffect

        if (mapFitPoints.isEmpty()) return@LaunchedEffect

        val previousCount = lastCameraFitKey?.split(";")?.count { it.isNotBlank() } ?: 0
        val shouldFit = lastCameraFitKey == null || mapFitPoints.size > previousCount
        if (!shouldFit) return@LaunchedEffect

        val washPhase = isWashPhaseStatus(tracking.status)
        val expansion = if (washPhase) {
            WASH_PHASE_BOUNDS_EXPANSION_FACTOR
        } else {
            MAP_BOUNDS_EXPANSION_FACTOR
        }
        val boundsPadding = if (washPhase) WASH_PHASE_BOUNDS_PADDING_PX else MAP_BOUNDS_PADDING_PX
        val singleZoom = if (washPhase) WASH_PHASE_ZOOM else DEFAULT_ZOOM

        when (mapFitPoints.size) {
            1 -> {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(mapFitPoints.first(), singleZoom),
                    durationMs = 800,
                )
            }
            else -> {
                try {
                    val builder = LatLngBounds.builder()
                    mapFitPoints.forEach { builder.include(it) }
                    val bounds = builder.build()
                        .withMinimumSpan()
                        .expandForZoomOut(expansion)
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngBounds(bounds, boundsPadding),
                        durationMs = 800,
                    )
                } catch (_: Exception) {
                    val center = LatLng(
                        mapFitPoints.map { it.latitude }.average(),
                        mapFitPoints.map { it.longitude }.average(),
                    )
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(center, singleZoom),
                        durationMs = 800,
                    )
                }
            }
        }
        lastCameraFitKey = cameraFitKey
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Cargando…", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                uiState.errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.errorMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                uiState.tracking != null -> {
                    val tracking = uiState.tracking!!
                    if (tracking.isDelivered) {
                        DeliveredOrderTrackingLayout(
                            tracking = tracking,
                            routePoints = routePoints,
                            usingStraightLineRoute = usingStraightLineRoute,
                            shopLatLng = shopLatLngForMap,
                            customerLatLng = customerLatLngForMap,
                            shopMarkerIcon = shopMarkerIcon,
                            houseMarkerIcon = houseMarkerIcon,
                            destinationLatLng = destinationLatLng,
                            destinationMarkerTitle = destinationMarkerTitle,
                            destinationMarkerSnippet = destinationMarkerSnippet,
                            deliveryManLatLng = deliveryManLatLng,
                            deliveryManMarkerState = deliveryManMarkerState,
                            destinationMarkerIcon = destinationMarkerIcon,
                            deliveryIcon = vehicleMarkerIcon,
                            cameraPositionState = cameraPositionState,
                            onMapLoaded = { mapLoaded = true },
                            uiState = uiState,
                            viewModel = viewModel,
                            onFinish = onFinish,
                        )
                    } else {
                    InProgressOrderTrackingLayout(
                        tracking = tracking,
                        routePoints = routePoints,
                        usingStraightLineRoute = usingStraightLineRoute,
                        shopLatLng = shopLatLngForMap,
                        customerLatLng = customerLatLngForMap,
                        shopMarkerIcon = shopMarkerIcon,
                        houseMarkerIcon = houseMarkerIcon,
                        destinationLatLng = destinationLatLng,
                        destinationMarkerTitle = destinationMarkerTitle,
                        destinationMarkerSnippet = destinationMarkerSnippet,
                        deliveryManLatLng = deliveryManLatLng,
                        deliveryManMarkerState = deliveryManMarkerState,
                        destinationMarkerIcon = destinationMarkerIcon,
                        deliveryIcon = vehicleMarkerIcon,
                        vehicleMarkerTitle = vehicleMarkerTitle,
                        cameraPositionState = cameraPositionState,
                        onMapLoaded = { mapLoaded = true },
                        sheetCollapsed = sheetCollapsed,
                        onSheetCollapsedChange = { sheetCollapsed = it },
                        uiState = uiState,
                        viewModel = viewModel,
                        onFinish = onFinish,
                    )
                    }
                }
            }
        }

        FloatingScreenHeader(
            title = if (uiState.tracking?.isCarWash == true) {
                "Seguimiento del servicio"
            } else {
                "Seguimiento del pedido"
            },
            onBack = onBack,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun DeliveredOrderTrackingLayout(
    tracking: com.ares.ewe.domain.model.OrderTracking,
    routePoints: List<LatLng>,
    usingStraightLineRoute: Boolean,
    shopLatLng: LatLng?,
    customerLatLng: LatLng?,
    shopMarkerIcon: BitmapDescriptor?,
    houseMarkerIcon: BitmapDescriptor?,
    destinationLatLng: LatLng?,
    destinationMarkerTitle: String,
    destinationMarkerSnippet: String?,
    deliveryManLatLng: LatLng?,
    deliveryManMarkerState: MarkerState,
    destinationMarkerIcon: BitmapDescriptor?,
    deliveryIcon: BitmapDescriptor?,
    cameraPositionState: com.google.maps.android.compose.CameraPositionState,
    onMapLoaded: () -> Unit,
    uiState: com.ares.ewe.presentation.viewmodel.main.home.OrderTrackingUiState,
    viewModel: OrderTrackingViewModel,
    onFinish: () -> Unit,
) {
    val maxSheetHeight = (LocalConfiguration.current.screenHeightDp *
        ORDER_TRACKING_DELIVERED_SHEET_MAX_HEIGHT_FRACTION).dp

    Box(Modifier.fillMaxSize()) {
        OrderTrackingMapContent(
            modifier = Modifier.fillMaxSize(),
            routePoints = routePoints,
            shopLatLng = shopLatLng,
            customerLatLng = customerLatLng,
            shopMarkerIcon = shopMarkerIcon,
            houseMarkerIcon = houseMarkerIcon,
            shopMarkerTitle = if (tracking.isCarWash) "Autolavado" else "Restaurante",
            shopMarkerSnippet = tracking.shopName,
            customerMarkerSnippet = tracking.deliveryAddress,
            destinationLatLng = destinationLatLng,
            destinationMarkerTitle = destinationMarkerTitle,
            destinationMarkerSnippet = destinationMarkerSnippet,
            destinationMarkerIcon = destinationMarkerIcon,
            deliveryManLatLng = deliveryManLatLng,
            deliveryManMarkerState = deliveryManMarkerState,
            deliveryIcon = deliveryIcon,
            deliveryManName = tracking.deliveryMan?.name,
            cameraPositionState = cameraPositionState,
            onMapLoaded = onMapLoaded,
        )

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .wrapContentHeight()
                .heightIn(max = maxSheetHeight),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            shadowElevation = 12.dp,
        ) {
            OrderTrackingBottomSheetContent(
                tracking = tracking,
                rateSubmitting = uiState.rateSubmitting,
                rateError = uiState.rateError,
                onSubmitDeliveryRating = { viewModel.submitDeliveryRating(it) },
                onSubmitShopRating = { viewModel.submitShopRating(it) },
                onSubmitProductRating = { productId, stars -> viewModel.submitProductRating(productId, stars) },
                onClearRateError = { viewModel.clearRateError() },
                onFinish = onFinish,
                fullScreen = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxSheetHeight)
                    .padding(horizontal = 20.dp)
                    .padding(top = 22.dp, bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun InProgressOrderTrackingLayout(
    tracking: com.ares.ewe.domain.model.OrderTracking,
    routePoints: List<LatLng>,
    usingStraightLineRoute: Boolean,
    shopLatLng: LatLng?,
    customerLatLng: LatLng?,
    shopMarkerIcon: BitmapDescriptor?,
    houseMarkerIcon: BitmapDescriptor?,
    destinationLatLng: LatLng?,
    destinationMarkerTitle: String,
    destinationMarkerSnippet: String?,
    deliveryManLatLng: LatLng?,
    deliveryManMarkerState: MarkerState,
    destinationMarkerIcon: com.google.android.gms.maps.model.BitmapDescriptor?,
    deliveryIcon: com.google.android.gms.maps.model.BitmapDescriptor?,
    vehicleMarkerTitle: String = "Repartidor",
    cameraPositionState: com.google.maps.android.compose.CameraPositionState,
    onMapLoaded: () -> Unit,
    sheetCollapsed: Boolean,
    onSheetCollapsedChange: (Boolean) -> Unit,
    uiState: com.ares.ewe.presentation.viewmodel.main.home.OrderTrackingUiState,
    viewModel: OrderTrackingViewModel,
    onFinish: () -> Unit,
) {
    var dragAccum by remember { mutableFloatStateOf(0f) }
    val maxSheetHeight = (LocalConfiguration.current.screenHeightDp *
        ORDER_TRACKING_SHEET_MAX_HEIGHT_FRACTION).dp

    Box(Modifier.fillMaxSize()) {
        OrderTrackingMapContent(
            modifier = Modifier.fillMaxSize(),
            routePoints = routePoints,
            shopLatLng = shopLatLng,
            customerLatLng = customerLatLng,
            shopMarkerIcon = shopMarkerIcon,
            houseMarkerIcon = houseMarkerIcon,
            shopMarkerTitle = if (tracking.isCarWash) "Autolavado" else "Restaurante",
            shopMarkerSnippet = tracking.shopName,
            customerMarkerSnippet = tracking.deliveryAddress,
            destinationLatLng = destinationLatLng,
            destinationMarkerTitle = destinationMarkerTitle,
            destinationMarkerSnippet = destinationMarkerSnippet,
            deliveryManLatLng = deliveryManLatLng,
            deliveryManMarkerState = deliveryManMarkerState,
            destinationMarkerIcon = destinationMarkerIcon,
            deliveryIcon = deliveryIcon,
            deliveryManName = vehicleMarkerTitle,
            cameraPositionState = cameraPositionState,
            onMapLoaded = onMapLoaded,
        )

        // El mapa debe recibir gestos arriba del sheet (como DobbyShop).
        // Colapsar solo con la cejilla / "Ocultar detalles".

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .animateContentSize()
                .then(
                    if (sheetCollapsed) Modifier else Modifier
                        .wrapContentHeight()
                        .heightIn(max = maxSheetHeight),
                ),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            shadowElevation = 12.dp,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(sheetCollapsed) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    when {
                                        dragAccum > 48f -> onSheetCollapsedChange(true)
                                        dragAccum < -48f -> onSheetCollapsedChange(false)
                                    }
                                    dragAccum = 0f
                                },
                                onVerticalDrag = { _, dragAmount ->
                                    dragAccum += dragAmount
                                },
                            )
                        }
                        .clickable { onSheetCollapsedChange(!sheetCollapsed) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = if (sheetCollapsed) "Mostrar detalles" else "Ocultar detalles",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Icon(
                            imageVector = if (sheetCollapsed) {
                                Icons.Default.KeyboardArrowUp
                            } else {
                                Icons.Default.KeyboardArrowDown
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                if (!sheetCollapsed) {
                    OrderTrackingBottomSheetContent(
                        tracking = tracking,
                        rateSubmitting = uiState.rateSubmitting,
                        rateError = uiState.rateError,
                        onSubmitDeliveryRating = { viewModel.submitDeliveryRating(it) },
                        onSubmitShopRating = { viewModel.submitShopRating(it) },
                        onSubmitProductRating = { productId, stars ->
                            viewModel.submitProductRating(productId, stars)
                        },
                        onClearRateError = { viewModel.clearRateError() },
                        onFinish = onFinish,
                        fullScreen = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxSheetHeight - 48.dp)
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderTrackingMapContent(
    modifier: Modifier,
    routePoints: List<LatLng>,
    shopLatLng: LatLng? = null,
    customerLatLng: LatLng? = null,
    shopMarkerIcon: BitmapDescriptor? = null,
    houseMarkerIcon: BitmapDescriptor? = null,
    shopMarkerTitle: String = "Restaurante",
    shopMarkerSnippet: String? = null,
    customerMarkerSnippet: String? = null,
    destinationLatLng: LatLng? = null,
    destinationMarkerTitle: String = "",
    destinationMarkerSnippet: String? = null,
    deliveryManLatLng: LatLng?,
    deliveryManMarkerState: MarkerState,
    destinationMarkerIcon: BitmapDescriptor?,
    deliveryIcon: BitmapDescriptor?,
    deliveryManName: String?,
    cameraPositionState: com.google.maps.android.compose.CameraPositionState,
    onMapLoaded: () -> Unit = {},
) {
    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(mapType = MapType.NORMAL),
        uiSettings = MapUiSettings(zoomControlsEnabled = false),
        onMapLoaded = onMapLoaded,
    ) {
        shopLatLng?.let { latLng ->
            Marker(
                state = MarkerState(position = latLng),
                title = shopMarkerTitle,
                snippet = shopMarkerSnippet,
                icon = shopMarkerIcon,
            )
        }
        customerLatLng?.let { latLng ->
            Marker(
                state = MarkerState(position = latLng),
                title = "Tu dirección de entrega",
                snippet = customerMarkerSnippet,
                icon = houseMarkerIcon,
            )
        }
        destinationLatLng?.let { latLng ->
            Marker(
                state = MarkerState(position = latLng),
                title = destinationMarkerTitle,
                snippet = destinationMarkerSnippet,
                icon = destinationMarkerIcon,
            )
        }
        deliveryManLatLng?.let {
            Marker(
                state = deliveryManMarkerState,
                title = deliveryManName ?: "Repartidor",
                snippet = deliveryManName,
                icon = deliveryIcon ?: BitmapDescriptorFactory.fromResource(R.drawable.ic_delivery),
                zIndex = 2f,
            )
        }
    }
}

