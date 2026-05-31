package com.ares.ewe.presentation.ui.main.home

import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.TextButton
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.ares.ewe.core.location.DeliveryServiceArea
import com.ares.ewe.core.theme.DobbyColors
import com.ares.ewe.presentation.viewmodel.main.home.MapLocationViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState

private const val DEFAULT_ZOOM = 15f
private val DEFAULT_POSITION = LatLng(20.6507582, -103.7029606) // Plaza Tala fallback
private val FloatingAddressCardColor = DobbyColors.Primary
private val ConfirmButtonColor = Color(0xFF22C55E)
private val PinTipToCenterOffset = (-24).dp
private val AddressCardOffsetFromPin = (-67).dp

private val ADDRESS_LABEL_OPTIONS = listOf(
    "Casa",
    "Apartamento",
    "Trabajo",
    "Novia",
    "Fiesta"
)

private fun String.toAddressCardPreview(): String {
    val trimmed = trim()
    if (trimmed.isBlank()) return "Dirección no disponible"
    val segments = trimmed.split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
    if (segments.isEmpty()) return "Dirección no disponible"
    val streetAndNumber = segments[0]
    val neighborhood = segments.getOrNull(1).orEmpty()
    return when {
        neighborhood.isBlank() -> streetAndNumber
        else -> "$streetAndNumber, $neighborhood"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapLocationScreen(
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit = {},
    viewModel: MapLocationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showFarAddressWarningBeforeSheet by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onPermissionResult(fineGranted || coarseGranted)
    }

    LaunchedEffect(Unit) {
        if (uiState.isChosenAddress) return@LaunchedEffect
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            viewModel.onPermissionResult(true)
        } else {
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            uiState.currentLocation ?: DEFAULT_POSITION,
            DEFAULT_ZOOM
        )
    }

    LaunchedEffect(uiState.userStartLocation) {
        val latLng = uiState.userStartLocation ?: return@LaunchedEffect
        cameraPositionState.animate(
            CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM),
            durationMs = 800
        )
    }

    // When the user moves the map, update the pin and address after they stop (debounced)
    LaunchedEffect(cameraPositionState) {
        snapshotFlow { cameraPositionState.position.target }
            .distinctUntilChanged { a, b ->
                a.latitude == b.latitude && a.longitude == b.longitude
            }
            .debounce(600)
            .collect { center ->
                viewModel.onMapCenterChanged(center)
            }
    }

    LaunchedEffect(uiState.addressSaved) {
        if (uiState.addressSaved) {
            onSaveSuccess()
            viewModel.clearAddressSaved()
        }
    }

    // Address label + description bottom sheet before saving
    if (uiState.showDescriptionDialog) {
        var descriptionText by remember { mutableStateOf("") }
        var selectedLabel by remember { mutableStateOf(ADDRESS_LABEL_OPTIONS.first()) }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        LaunchedEffect(Unit) { sheetState.expand() }
        ModalBottomSheet(
            onDismissRequest = viewModel::onDismissDescriptionDialog,
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Guardar dirección",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // Description section
                Text(
                    text = "Descripción",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = { descriptionText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    placeholder = { Text("ej. Casa verde, piso 2") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                // Label section (5 options in 2 columns)
                Text(
                    text = "Etiqueta",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ADDRESS_LABEL_OPTIONS.take(3).forEach { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedLabel = option }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedLabel == option,
                                    onClick = { selectedLabel = option }
                                )
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ADDRESS_LABEL_OPTIONS.drop(3).forEach { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedLabel = option }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedLabel == option,
                                    onClick = { selectedLabel = option }
                                )
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = viewModel::onDismissDescriptionDialog) {
                        Text("Cancelar", color = MaterialTheme.colorScheme.onSurface)
                    }
                    TextButton(
                        onClick = {
                            val center = cameraPositionState.position.target
                            viewModel.saveAddressWithDescription(
                                label = selectedLabel,
                                description = descriptionText.ifBlank { null },
                                latLng = center,
                                addressText = uiState.editableAddress
                            )
                        }
                    ) {
                        Text("Guardar", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

    }

    if (showFarAddressWarningBeforeSheet) {
        AlertDialog(
            onDismissRequest = { showFarAddressWarningBeforeSheet = false },
            title = { Text("Dirección lejana") },
            text = {
                Text("El pin está a más de 200 metros de tu ubicación actual. ¿Deseas continuar?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFarAddressWarningBeforeSheet = false
                        viewModel.onSaveAddressClick(cameraPositionState.position.target)
                    }
                ) {
                    Text("Sí, continuar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFarAddressWarningBeforeSheet = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.isChosenAddress) "Dirección elegida" else "Mi ubicación"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Full-size map
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = com.google.maps.android.compose.MapProperties(
                    isMyLocationEnabled = uiState.permissionGranted
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    zoomGesturesEnabled = true,
                    scrollGesturesEnabled = true
                )
            )

            // Center pin overlay (only one pin)
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = PinTipToCenterOffset)
                    .size(48.dp),
                tint = FloatingAddressCardColor
            )

            if (uiState.isLoading && uiState.currentLocation == null) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }

            // Address field over the map (top)
            if (uiState.currentLocation != null) {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = AddressCardOffsetFromPin)
                        .padding(horizontal = 44.dp)
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = FloatingAddressCardColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Ajusta la ubicación de entrega",
                            color = Color.White.copy(alpha = 0.95f),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Text(
                            text = uiState.editableAddress.toAddressCardPreview(),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ),
                            maxLines = 2
                        )
                    }
                }
            }

            // Save address button over the map (bottom)
            val mapCenter = cameraPositionState.position.target
            val hasPolygon = viewModel.hasValidServiceAreaPolygon()
            val configBlocking = viewModel.isServiceAreaConfigBlocking()
            val insideArea = viewModel.isInsideServiceArea(mapCenter)
            val canConfirmByArea = !configBlocking && (!hasPolygon || insideArea)
            val confirmEnabled = !uiState.isReverseGeocoding && canConfirmByArea
            val confirmLabel = when {
                uiState.isReverseGeocoding -> "Guardando…"
                configBlocking -> DeliveryServiceArea.CONFIG_FIX_LABEL
                hasPolygon && !insideArea -> DeliveryServiceArea.OUTSIDE_LIMITS_LABEL
                else -> "Confirmar ubicación"
            }
            Button(
                onClick = {
                    val center = cameraPositionState.position.target
                    val distanceMeters = uiState.userStartLocation?.let { start ->
                        val results = FloatArray(1)
                        Location.distanceBetween(
                            start.latitude,
                            start.longitude,
                            center.latitude,
                            center.longitude,
                            results
                        )
                        results[0]
                    }
                    if (distanceMeters != null && distanceMeters > 200f) {
                        showFarAddressWarningBeforeSheet = true
                    } else {
                        viewModel.onSaveAddressClick(center)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
                    .height(64.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ConfirmButtonColor,
                    contentColor = Color.White,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                enabled = confirmEnabled
            ) {
                Text(
                    confirmLabel,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = MaterialTheme.typography.titleMedium.fontSize * 1.1f
                    )
                )
            }

            uiState.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 72.dp)
                )
            }
        }
    }
}
