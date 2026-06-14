package com.ares.ewe.presentation.ui.main.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
/** Edge padding (px) when fitting courier + destination; larger = more zoom out. */
private const val MAP_BOUNDS_PADDING_PX = 150
/**
 * Expands the fitted bounds before applying the camera (1.0 = tight fit).
 * Increase (e.g. 1.5) to zoom out more when repartidor + destino are visible.
 */
private const val MAP_BOUNDS_EXPANSION_FACTOR = 1.35f
/** Minimum lat/lng span so [CameraUpdateFactory.newLatLngBounds] does not zoom to world view. */
private const val MIN_BOUNDS_SPAN_DEGREES = 0.004
private const val MARKER_ICON_SIZE_DP = 48
private val FALLBACK_LATLNG = LatLng(20.6507582, -103.7029606)
private val RoutePolylineColor = Color(0xFF1976D2)
/** Brand green aligned with PhoneScreen (~#2ECC71). */
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
private const val ORDER_TRACKING_SHEET_MAX_HEIGHT_FRACTION = 0.88f

@Composable
fun OrderTrackingScreen(
    onBack: () -> Unit,
    onFinish: () -> Unit,
    viewModel: OrderTrackingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val routePoints = uiState.routePoints
    val usingStraightLineRoute = uiState.usingStraightLineRoute
    var sheetVisible by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val deliveryIcon = remember(context) { bitmapDescriptorFromRes(context, R.drawable.ic_delivery) }

    val tracking = uiState.tracking
    val shopMarkerIcon = remember(context) {
        bitmapDescriptorFromRes(context, R.drawable.ic_shop)
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
    val shopLatLngForMap = if (showsRestaurantAndCustomer) shopLatLng else null
    val customerLatLngForMap = if (showsRestaurantAndCustomer) customerLatLng else null
    val destinationLatLng = if (tracking != null && !showsRestaurantAndCustomer) {
        tracking.routeDestinationLatLng()?.let { (lat, lng) ->
            LatLng(lat, lng).takeIfValid()
        }
    } else {
        null
    }
    val destinationMarkerIcon = if (tracking?.isAssignedToCourier == true) shopMarkerIcon else houseMarkerIcon
    val destinationMarkerTitle = if (tracking?.isAssignedToCourier == true) {
        "Restaurante"
    } else {
        "Tu dirección de entrega"
    }
    val destinationMarkerSnippet = if (tracking?.isAssignedToCourier == true) {
        tracking.shopName
    } else {
        tracking?.deliveryAddress
    }
    val deliveryManLatLng = tracking?.deliveryMan?.let { dm ->
        if (dm.lat != null && dm.lng != null) LatLng(dm.lat, dm.lng).takeIfValid() else null
    }

    val deliveryManMarkerState = remember { MarkerState(FALLBACK_LATLNG) }
    LaunchedEffect(deliveryManLatLng?.latitude, deliveryManLatLng?.longitude) {
        deliveryManLatLng?.let { deliveryManMarkerState.position = it }
    }

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

        when (mapFitPoints.size) {
            1 -> {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(mapFitPoints.first(), DEFAULT_ZOOM),
                    durationMs = 800,
                )
            }
            else -> {
                try {
                    val builder = LatLngBounds.builder()
                    mapFitPoints.forEach { builder.include(it) }
                    val bounds = builder.build()
                        .withMinimumSpan()
                        .expandForZoomOut(MAP_BOUNDS_EXPANSION_FACTOR)
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngBounds(bounds, MAP_BOUNDS_PADDING_PX),
                        durationMs = 800,
                    )
                } catch (_: Exception) {
                    val center = LatLng(
                        mapFitPoints.map { it.latitude }.average(),
                        mapFitPoints.map { it.longitude }.average(),
                    )
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(center, DEFAULT_ZOOM),
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
                            deliveryIcon = deliveryIcon,
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
                        deliveryIcon = deliveryIcon,
                        cameraPositionState = cameraPositionState,
                        onMapLoaded = { mapLoaded = true },
                        sheetVisible = sheetVisible,
                        onSheetVisibleChange = { sheetVisible = it },
                        uiState = uiState,
                        viewModel = viewModel,
                        onFinish = onFinish,
                    )
                    }
                }
            }
        }

        FloatingScreenHeader(
            title = "Seguimiento del pedido",
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
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.height(112.dp).fillMaxWidth()) {
            OrderTrackingMapContent(
                modifier = Modifier.fillMaxSize(),
                routePoints = routePoints,
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
        }
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
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
                fullScreen = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
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
    cameraPositionState: com.google.maps.android.compose.CameraPositionState,
    onMapLoaded: () -> Unit,
    sheetVisible: Boolean,
    onSheetVisibleChange: (Boolean) -> Unit,
    uiState: com.ares.ewe.presentation.viewmodel.main.home.OrderTrackingUiState,
    viewModel: OrderTrackingViewModel,
    onFinish: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        OrderTrackingMapContent(
            modifier = Modifier.fillMaxSize(),
            routePoints = routePoints,
            shopLatLng = shopLatLng,
            customerLatLng = customerLatLng,
            shopMarkerIcon = shopMarkerIcon,
            houseMarkerIcon = houseMarkerIcon,
            shopMarkerSnippet = tracking.shopName,
            customerMarkerSnippet = tracking.deliveryAddress,
            destinationLatLng = destinationLatLng,
            destinationMarkerTitle = destinationMarkerTitle,
            destinationMarkerSnippet = destinationMarkerSnippet,
            deliveryManLatLng = deliveryManLatLng,
            deliveryManMarkerState = deliveryManMarkerState,
            destinationMarkerIcon = destinationMarkerIcon,
            deliveryIcon = deliveryIcon,
            deliveryManName = tracking.deliveryMan?.name,
            cameraPositionState = cameraPositionState,
            onMapLoaded = onMapLoaded,
        )

        if (usingStraightLineRoute && destinationLatLng != null && deliveryManLatLng != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.92f),
                tonalElevation = 2.dp,
            ) {
                Text(
                    text = "La ruta por calles no está disponible (solo línea recta). " +
                        "Habilita Directions API y facturación en Google Cloud, y en local.properties " +
                        "define DIRECTIONS_API_KEY con una clave apta para el servicio web " +
                        "(no uses solo restricción «aplicaciones Android»).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        if (sheetVisible) {
            val maxSheetHeight = (LocalConfiguration.current.screenHeightDp *
                ORDER_TRACKING_SHEET_MAX_HEIGHT_FRACTION).dp
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = { onSheetVisibleChange(false) },
                    ),
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .heightIn(max = maxSheetHeight),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                shadowElevation = 12.dp,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 40.dp, height = 5.dp)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                        )
                    }
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
                            .heightIn(max = maxSheetHeight - 28.dp)
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 12.dp),
                    )
                }
            }
        } else {
            Button(
                onClick = { onSheetVisibleChange(true) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White,
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 15.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp),
                    )
                    Text(
                        text = "Ver detalles del pedido",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            fontSize = 18.sp
                        ),
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
        if (routePoints.size >= 2) {
            Polyline(points = routePoints, color = RoutePolylineColor, width = 10f)
        }
        shopLatLng?.let { latLng ->
            Marker(
                state = MarkerState(position = latLng),
                title = "Restaurante",
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
                title = "Repartidor",
                snippet = deliveryManName,
                icon = deliveryIcon ?: BitmapDescriptorFactory.fromResource(R.drawable.ic_delivery),
            )
        }
    }
}

