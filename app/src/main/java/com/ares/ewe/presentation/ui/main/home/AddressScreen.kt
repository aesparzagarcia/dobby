package com.ares.ewe.presentation.ui.main.home

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.ares.ewe.core.theme.DobbyColors
import com.ares.ewe.core.theme.DobbyPureScale
import com.ares.ewe.domain.model.UserAddress
import com.ares.ewe.domain.model.toAddressWithColonyOnly
import com.ares.ewe.presentation.viewmodel.main.home.AddressViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

private val AddressScreenBg = Color(0xFFF7F8FA)
private val AddressCardBorder = Color(0xFFE8EAEF)
private val AddressMuted = Color(0xFF8A8F98)
private val AddressLocationBlue = Color(0xFF2F6BFF)
private val AddressPrincipalBg = Color(0xFFE8F0FF)
private val DefaultMapPosition = LatLng(20.6507582, -103.7029606)

@Composable
fun AddressScreen(
    onBack: () -> Unit,
    onCurrentLocationClick: () -> Unit = {},
    onNavigateToMapWithLocation: (lat: Double, lng: Double, address: String) -> Unit = { _, _, _ -> },
    onRequireLogin: () -> Unit = {},
    viewModel: AddressViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchFocusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) {
            viewModel.refreshDeviceLocation()
        }
    }

    LaunchedEffect(uiState.isLoggedIn) {
        if (!uiState.isLoggedIn) return@LaunchedEffect
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            viewModel.refreshDeviceLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(uiState.navigateToMapWithLocation) {
        uiState.navigateToMapWithLocation?.let { navData ->
            onNavigateToMapWithLocation(navData.latLng.latitude, navData.latLng.longitude, navData.addressLabel)
            viewModel.onNavigatedToMap()
        }
    }

    LaunchedEffect(uiState.navigateBackToHome) {
        if (uiState.navigateBackToHome) {
            viewModel.onNavigatedBackToHome()
            onBack()
        }
    }

    Scaffold(
        containerColor = AddressScreenBg,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AddressScreenBg)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp)
                    .padding(top = 4.dp, bottom = 8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = DobbyColors.TextPrimary
                    )
                }
                Text(
                    text = "Mis direcciones",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                    ),
                    color = DobbyColors.TextPrimary,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Text(
                    text = "Selecciona o agrega la dirección donde quieres que te lleguen tus pedidos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AddressMuted,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!uiState.isLoggedIn) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "Inicia sesión para guardar y administrar tu dirección de entrega.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AddressMuted,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onRequireLogin,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DobbyColors.Primary,
                        ),
                    ) {
                        Text(
                            text = "Inicia sesión para continuar",
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        AddressSearchBar(
                            query = uiState.searchQuery,
                            isLoading = uiState.isLoading,
                            focusRequester = searchFocusRequester,
                            onQueryChange = viewModel::onSearchQueryChange,
                        )
                    }

                    if (uiState.errorMessage != null) {
                        item {
                            Text(
                                text = uiState.errorMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }

                    if (uiState.searchResults.isNotEmpty()) {
                        items(uiState.searchResults, key = { "search-${it.id}" }) { result ->
                            AddressResultItem(
                                title = result.title,
                                subtitle = result.subtitle,
                                onClick = {
                                    val label = if (result.subtitle != null) {
                                        "${result.title}, ${result.subtitle}"
                                    } else {
                                        result.title
                                    }
                                    viewModel.onAddressClick(result.id, label)
                                }
                            )
                        }
                    } else {
                        item {
                            CurrentLocationCard(
                                location = uiState.deviceLocation,
                                addressText = uiState.deviceLocationAddress,
                                isLoading = uiState.isLoadingDeviceLocation,
                                onClick = onCurrentLocationClick,
                            )
                        }

                        item {
                            SavedAddressesHeader(
                                onAddNew = {
                                    viewModel.onSearchQueryChange("")
                                    searchFocusRequester.requestFocus()
                                }
                            )
                        }

                        if (uiState.isLoadingAddresses && uiState.myAddresses.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        strokeWidth = 2.dp,
                                        color = AddressLocationBlue,
                                    )
                                }
                            }
                        } else if (uiState.myAddresses.isEmpty()) {
                            item {
                                Text(
                                    text = "No hay direcciones guardadas",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AddressMuted,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        } else {
                            items(uiState.myAddresses, key = { it.id }) { address ->
                                SavedAddressCard(
                                    address = address,
                                    onSelect = { viewModel.onMyAddressSelected(address) },
                                    onSetDefault = { viewModel.onSetAsDefault(address) },
                                    onDelete = { viewModel.onDeleteAddress(address) },
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.isLoadingPlaceDetails) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 3.dp,
                            color = AddressLocationBlue,
                        )
                        Text(
                            text = "Cargando ubicación…",
                            style = MaterialTheme.typography.bodyLarge,
                            color = DobbyColors.TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddressSearchBar(
    query: String,
    isLoading: Boolean,
    focusRequester: FocusRequester,
    onQueryChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, AddressCardBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = AddressMuted,
            modifier = Modifier.size(22.dp)
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(
                color = DobbyColors.TextPrimary,
                fontSize = 15.sp,
            ),
            cursorBrush = SolidColor(AddressLocationBlue),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        text = "Buscar dirección, colonia o ciudad",
                        color = AddressMuted,
                        fontSize = 15.sp,
                    )
                }
                inner()
            }
        )
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = AddressLocationBlue,
            )
        } else {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = DobbyColors.TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun CurrentLocationCard(
    location: LatLng?,
    addressText: String?,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    val mapTarget = location ?: DefaultMapPosition
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(mapTarget, 15f)
    }

    LaunchedEffect(location) {
        location?.let {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(it, 15f)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, AddressCardBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    scrollGesturesEnabled = false,
                    zoomGesturesEnabled = false,
                    tiltGesturesEnabled = false,
                    rotationGesturesEnabled = false,
                    mapToolbarEnabled = false,
                    myLocationButtonEnabled = false,
                ),
            ) {
                if (location != null) {
                    Marker(state = MarkerState(position = location))
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = null,
                    tint = DobbyColors.TextPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Ubicación actual",
                    color = DobbyColors.TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DobbyColors.Primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NearMe,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Usar mi ubicación actual",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DobbyColors.TextPrimary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when {
                        isLoading -> "Obteniendo ubicación…"
                        !addressText.isNullOrBlank() -> addressText
                        else -> "Toca para confirmar en el mapa"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = AddressMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = AddressMuted,
            )
        }
    }
}

@Composable
private fun SavedAddressesHeader(onAddNew: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Mis direcciones guardadas",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = DobbyColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        TextButton(
            onClick = onAddNew,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
        ) {
            Text(
                text = "+ Agregar nueva",
                color = AddressLocationBlue,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun SavedAddressCard(
    address: UserAddress,
    onSelect: () -> Unit,
    onSetDefault: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val style = addressLabelStyle(address.label)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, AddressCardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onSelect),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(style.background),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = style.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = address.label.ifBlank { "Dirección" },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = DobbyColors.TextPrimary,
                    )
                    if (address.isDefault) {
                        Text(
                            text = "Principal",
                            color = AddressLocationBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(AddressPrincipalBg)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = address.address.toAddressWithColonyOnly(),
                    style = MaterialTheme.typography.bodySmall,
                    color = AddressMuted,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Opciones",
                    tint = AddressMuted,
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                if (!address.isDefault) {
                    DropdownMenuItem(
                        text = { Text("Establecer como principal") },
                        onClick = {
                            menuExpanded = false
                            onSetDefault()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Eliminar") },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    }
                )
            }
        }
    }
}

private data class AddressLabelStyle(
    val icon: ImageVector,
    val background: Color,
)

private fun addressLabelStyle(label: String): AddressLabelStyle {
    return when (label.trim().lowercase()) {
        "casa", "home" -> AddressLabelStyle(Icons.Default.Home, AddressLocationBlue)
        "trabajo", "work" -> AddressLabelStyle(Icons.Default.Work, Color(0xFF34C759))
        "gimnasio", "gym" -> AddressLabelStyle(Icons.Default.FitnessCenter, Color(0xFFAF52DE))
        "novia" -> AddressLabelStyle(Icons.Default.Favorite, Color(0xFFFF2D55))
        "apartamento" -> AddressLabelStyle(Icons.Default.Apartment, Color(0xFF5856D6))
        "fiesta" -> AddressLabelStyle(Icons.Default.Celebration, Color(0xFFFF9500))
        else -> AddressLabelStyle(Icons.Default.Place, DobbyPureScale.Graphite)
    }
}

@Composable
private fun AddressResultItem(
    title: String,
    subtitle: String?,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, AddressCardBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = DobbyColors.TextPrimary
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = AddressMuted
            )
        }
    }
}
