package com.ares.ewe.presentation.ui.main.home

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ares.ewe.BuildConfig
import com.ares.ewe.presentation.viewmodel.main.home.OrderTrackingViewModel
import com.ares.ewe.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

private const val DEFAULT_ZOOM = 15f
private const val MARKER_ICON_SIZE_DP = 48
private val FALLBACK_LATLNG = LatLng(20.6507582, -103.7029606)
private val RoutePolylineColor = Color(0xFF1976D2)

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderTrackingScreen(
    onBack: () -> Unit,
    viewModel: OrderTrackingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val routePoints = uiState.routePoints
    val usingStraightLineRoute = uiState.usingStraightLineRoute
    var sheetVisible by remember { mutableStateOf(true) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )
    val context = LocalContext.current
    val houseIcon = remember(context) { bitmapDescriptorFromRes(context, R.drawable.ic_house) }
    val deliveryIcon = remember(context) { bitmapDescriptorFromRes(context, R.drawable.ic_delivery) }

    val deliveryLatLng = uiState.tracking?.let { t ->
        if (t.lat != null && t.lng != null) LatLng(t.lat, t.lng) else null
    }
    val deliveryManLatLng = uiState.tracking?.deliveryMan?.let { dm ->
        if (dm.lat != null && dm.lng != null) LatLng(dm.lat, dm.lng) else null
    }

    val deliveryManMarkerState = remember { MarkerState(FALLBACK_LATLNG) }
    LaunchedEffect(deliveryManLatLng?.latitude, deliveryManLatLng?.longitude) {
        deliveryManLatLng?.let { deliveryManMarkerState.position = it }
    }

    var hasFittedBounds by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            deliveryLatLng ?: FALLBACK_LATLNG,
            DEFAULT_ZOOM
        )
    }

    LaunchedEffect(deliveryLatLng, deliveryManLatLng) {
        when {
            deliveryLatLng != null && deliveryManLatLng != null && !hasFittedBounds -> {
                val builder = com.google.android.gms.maps.model.LatLngBounds.builder()
                builder.include(deliveryLatLng)
                builder.include(deliveryManLatLng)
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngBounds(builder.build(), 120),
                    durationMs = 800
                )
                hasFittedBounds = true
            }
            deliveryLatLng != null && deliveryManLatLng == null -> cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(deliveryLatLng, DEFAULT_ZOOM),
                durationMs = 800
            )
            deliveryManLatLng != null && deliveryLatLng == null -> cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(deliveryManLatLng, DEFAULT_ZOOM),
                durationMs = 800
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seguimiento del pedido") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Cargando...", style = MaterialTheme.typography.bodyLarge)
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
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(mapType = MapType.NORMAL),
                        uiSettings = MapUiSettings(zoomControlsEnabled = true),
                    ) {
                        if (routePoints.size >= 2) {
                            Polyline(
                                points = routePoints,
                                color = RoutePolylineColor,
                                width = 10f
                            )
                        }
                        deliveryLatLng?.let { latLng ->
                            Marker(
                                state = MarkerState(position = latLng),
                                title = "Tu dirección de entrega",
                                snippet = uiState.tracking?.deliveryAddress,
                                icon = houseIcon ?: BitmapDescriptorFactory.fromResource(R.drawable.ic_house)
                            )
                        }
                        deliveryManLatLng?.let {
                            Marker(
                                state = deliveryManMarkerState,
                                title = "Repartidor",
                                snippet = uiState.tracking?.deliveryMan?.name,
                                icon = deliveryIcon ?: BitmapDescriptorFactory.fromResource(R.drawable.ic_delivery)
                            )
                        }
                    }

                    if (usingStraightLineRoute && deliveryLatLng != null && deliveryManLatLng != null) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.92f),
                            tonalElevation = 2.dp
                        ) {
                            Text(
                                text = "La ruta por calles no está disponible (solo línea recta). " +
                                    "Habilita Directions API y facturación en Google Cloud, y en local.properties " +
                                    "define DIRECTIONS_API_KEY con una clave apta para el servicio web " +
                                    "(no uses solo restricción «aplicaciones Android»).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // Bottom sheet: when dismissed, remove it so the map is fully touchable; reopen with button
                    if (sheetVisible) {
                        LaunchedEffect(sheetVisible) {
                            if (sheetVisible) sheetState.expand()
                        }
                        ModalBottomSheet(
                            onDismissRequest = { sheetVisible = false },
                            sheetState = sheetState
                        ) {
                            OrderTrackingBottomSheetContent(
                                tracking = uiState.tracking!!,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .padding(bottom = 32.dp)
                            )
                        }
                    } else {
                        // Sheet closed: map is fully interactive; show button to reopen
                        FilledTonalButton(
                            onClick = { sheetVisible = true },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowUp,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Ver detalles del pedido")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderTrackingBottomSheetContent(
    tracking: com.ares.ewe.domain.model.OrderTracking,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier.verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Tu pedido",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )

        // Status + estimated preparation (when provided by API)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    RoundedCornerShape(8.dp)
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.ShoppingBag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = statusLabel(tracking.status),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            tracking.estimatedPreparationMinutes?.let { minutes ->
                Text(
                    text = "Prep. estimada: ${minutes} min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        tracking.shopName?.let { name ->
            Text(
                text = "Tienda: $name",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (tracking.deliveryAddress != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = tracking.deliveryAddress,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Text(
            text = "Productos",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        tracking.items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${item.productName} x${item.quantity}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "$${String.format("%.2f", item.price * item.quantity)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Total",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "$${String.format("%.2f", tracking.total)}",
                style = MaterialTheme.typography.titleMedium
            )
        }

        // Delivery man section
        tracking.deliveryMan?.let { dm ->
            val context = LocalContext.current
            Text(
                text = "Repartidor",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
                    .then(
                        if (!dm.celphone.isNullOrBlank()) {
                            Modifier.clickable {
                                context.startActivity(
                                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:${dm.celphone}"))
                                )
                            }
                        } else Modifier
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!dm.profilePhotoUrl.isNullOrBlank()) {
                    val imageUrl = dm.profilePhotoUrl!!.let { url ->
                        if (url.startsWith("http")) url
                        else BuildConfig.BASE_URL.substringBefore("api/").dropLast(1) + url
                    }
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = dm.name,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Text(
                    text = dm.name,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                )
                if (!dm.celphone.isNullOrBlank()) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = "Llamar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } ?: run {
            Text(
                text = "Aún no se ha asignado un repartidor.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun statusLabel(status: String): String = when (status) {
    "PENDING" -> "Pendiente"
    "CONFIRMED" -> "Confirmado"
    "PREPARING" -> "En preparación"
    "READY_FOR_PICKUP" -> "Listo para recoger"
    "ASSIGNED" -> "Asignado a repartidor"
    "ON_DELIVERY" -> "En camino"
    "DELIVERED" -> "Entregado"
    "CANCELLED" -> "Cancelado"
    else -> status
}
