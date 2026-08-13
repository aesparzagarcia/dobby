package com.ares.ewe.presentation.ui.main.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ares.ewe.core.theme.DobbyColors
import com.ares.ewe.core.theme.DobbyPureScale
import com.ares.ewe.domain.model.ServiceDetail
import com.ares.ewe.presentation.components.LoadingAsyncImage
import com.ares.ewe.presentation.viewmodel.main.home.ServiceDetailUiState
import com.ares.ewe.presentation.viewmodel.main.home.ServiceDetailViewModel

private val ServiceScreenBg = Color(0xFFF7F8F7)
private val ServiceCardBorder = Color(0xFFE6E8E6)
private val ServiceMuted = Color(0xFF6B7280)
private val ServiceSafeGreen = Color(0xFF1B7A3D)
private val ServiceSafeGreenBg = Color(0xFFE8F6EE)
private val ServiceFieldBorder = Color(0xFFD7DBD7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceDetailScreen(
    onBack: () -> Unit,
    onCartClick: () -> Unit = {},
    viewModel: ServiceDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val cartItemCount by viewModel.cartItemCount.collectAsState(0)

    Scaffold(
        containerColor = ServiceScreenBg,
        topBar = {
            TopAppBar(
                title = { Text(uiState.service?.name ?: "Servicio") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    CartIconBadge(itemCount = cartItemCount, onClick = onCartClick)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ServiceScreenBg,
                    titleContentColor = DobbyColors.TextPrimary,
                ),
            )
        },
        bottomBar = {
            if (uiState.service != null && !uiState.isLoading && uiState.errorMessage == null) {
                ServicePayBottomBar(
                    enabled = uiState.canPay,
                    onPay = {
                        viewModel.payService(onAdded = onCartClick)
                    },
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = DobbyPureScale.Onyx)
                    }
                }
                uiState.errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.loadService() },
                                colors = ButtonDefaults.buttonColors(containerColor = DobbyPureScale.Onyx),
                            ) {
                                Text("Reintentar")
                            }
                        }
                    }
                }
                uiState.service != null -> {
                    val context = LocalContext.current
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (!uiState.payError.isNullOrBlank()) {
                            Text(
                                text = uiState.payError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                            )
                        }
                        ServiceDetailContent(
                            service = uiState.service!!,
                            uiState = uiState,
                            onServiceNumberChange = viewModel::onServiceNumberChange,
                            onAmountChange = viewModel::onAmountChange,
                            onScanBarcode = {
                                launchServiceBarcodeScanner(context) { digits ->
                                    viewModel.onServiceNumberChange(digits)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceDetailContent(
    service: ServiceDetail,
    uiState: ServiceDetailUiState,
    onServiceNumberChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onScanBarcode: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ServiceHeaderCard(service = service)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = DobbyPureScale.Onyx,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = String.format("%.1f", service.rate),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = DobbyColors.TextPrimary,
            )
        }

        Text(
            text = "Información del servicio",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = DobbyColors.TextPrimary,
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ServiceOutlinedField(
                value = uiState.serviceNumber,
                onValueChange = onServiceNumberChange,
                placeholder = "Número de servicio",
                trailingIcon = Icons.Default.QrCodeScanner,
                onTrailingIconClick = onScanBarcode,
                keyboardType = KeyboardType.Number,
            )
            Text(
                text = "Puedes encontrarlo en tu recibo de ${service.name}.",
                style = MaterialTheme.typography.bodySmall,
                color = ServiceMuted,
            )
        }

        ServiceAmountField(
            value = uiState.amountToPay,
            onValueChange = onAmountChange,
        )
    }
}

@Composable
private fun ServiceAmountField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Importe a pagar",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = DobbyColors.TextPrimary,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .border(1.dp, ServiceFieldBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = DobbyColors.TextPrimary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = "0.00",
                        style = MaterialTheme.typography.bodyLarge,
                        color = ServiceMuted,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = DobbyColors.TextPrimary,
                        fontSize = 16.sp,
                    ),
                    cursorBrush = SolidColor(DobbyPureScale.Onyx),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Text(
            text = "Ingresa el importe exacto a pagar",
            style = MaterialTheme.typography.bodySmall,
            color = ServiceMuted,
        )
    }
}

@Composable
private fun ServiceHeaderCard(service: ServiceDetail) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, ServiceCardBorder, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFF2F4F2)),
            contentAlignment = Alignment.Center,
        ) {
            LoadingAsyncImage(
                model = service.imageUrl,
                contentDescription = service.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = {
                    Text(
                        text = service.name.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = DobbyPureScale.Onyx,
                    )
                },
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = service.name,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = DobbyColors.TextPrimary,
            )
            val subtitle = service.description?.takeIf { it.isNotBlank() }
                ?: service.category?.takeIf { it.isNotBlank() }
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ServiceMuted,
                    maxLines = 2,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(ServiceSafeGreenBg)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = null,
                    tint = ServiceSafeGreen,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = "Servicio oficial",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = ServiceSafeGreen,
                )
            }
        }
    }
}

@Composable
private fun ServiceOutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, ServiceFieldBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = ServiceMuted,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = ServiceMuted,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = DobbyColors.TextPrimary,
                    fontSize = 16.sp,
                ),
                cursorBrush = SolidColor(DobbyPureScale.Onyx),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (trailingIcon != null) {
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                imageVector = trailingIcon,
                contentDescription = "Escanear código de barras",
                tint = ServiceMuted,
                modifier = Modifier
                    .size(22.dp)
                    .then(
                        if (onTrailingIconClick != null) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onTrailingIconClick,
                            )
                        } else {
                            Modifier
                        }
                    ),
            )
        }
    }
}

@Composable
private fun ServicePayBottomBar(
    enabled: Boolean,
    onPay: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ServiceScreenBg)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Button(
            onClick = onPay,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DobbyPureScale.Onyx,
                contentColor = Color.White,
                disabledContainerColor = DobbyPureScale.Mist,
                disabledContentColor = DobbyPureScale.Ash,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        ) {
            Text(
                text = "Pagar servicio",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
}
