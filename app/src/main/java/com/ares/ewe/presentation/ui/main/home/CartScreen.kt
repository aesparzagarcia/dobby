package com.ares.ewe.presentation.ui.main.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import java.util.Locale
import androidx.hilt.navigation.compose.hiltViewModel
import com.ares.ewe.presentation.components.LoadingAsyncImage
import com.ares.ewe.core.pricing.OrderPricing
import com.ares.ewe.domain.model.CartItem
import com.ares.ewe.presentation.viewmodel.main.home.CartViewModel

private val CartScreenBackground = Color(0xFFF7F5FA)
private val CartListBackground = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onBack: () -> Unit,
    onCheckoutComplete: suspend () -> Unit = {},
    onRequireLogin: () -> Unit = {},
    viewModel: CartViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.orderPlaced) {
        if (uiState.orderPlaced) {
            viewModel.clearOrderPlaced()
            onCheckoutComplete()
        }
    }

    if (uiState.isPlacingOrder) {
        BackHandler { /* bloquear atrás mientras se crea el pedido */ }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = CartScreenBackground,
        topBar = {
            TopAppBar(
                title = { Text("Carrito") },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !uiState.isPlacingOrder) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tu carrito está vacío",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(CartListBackground),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    itemsIndexed(uiState.items, key = { _, item -> item.productId }) { index, item ->
                        CartItemRow(
                            item = item,
                            onRemove = { viewModel.removeItem(item.productId) },
                        )
                        if (index < uiState.items.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 84.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                        }
                    }
                }
                CartDetailsSection(
                    addressLabel = uiState.addressLabel,
                    addressText = uiState.addressText,
                    addressDetails = uiState.addressDetails,
                    estimatedDeliveryTime = uiState.estimatedDeliveryTime,
                    paymentMethod = uiState.paymentMethod,
                )
                if (uiState.placeOrderError != null) {
                    Text(
                        text = uiState.placeOrderError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CartListBackground)
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp, bottom = 16.dp)
                ) {
                    CartPricingFooter(
                        pricing = uiState.pricing,
                        grandTotal = uiState.grandTotal,
                        hasValidDeliveryAddress = uiState.hasValidDeliveryAddress,
                        isServicePayment = uiState.items.isNotEmpty() && uiState.items.all { it.isServicePayment },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (uiState.isLoggedIn) {
                        androidx.compose.material3.Button(
                            onClick = {
                                viewModel.placeOrder(
                                    uiState.addressId,
                                    uiState.items,
                                    uiState.pricing?.delivery?.finalDeliveryFee ?: 0.0
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = uiState.hasValidDeliveryAddress &&
                                uiState.items.isNotEmpty() &&
                                !uiState.isPlacingOrder,
                        ) {
                            Text("Pagar $${String.format(Locale.US, "%.2f", uiState.grandTotal)}")
                        }
                    } else {
                        androidx.compose.material3.Button(
                            onClick = onRequireLogin,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Iniciar sesión para pedir")
                        }
                    }
                }
            }
        }
    }

        if (uiState.isPlacingOrder) {
            PlaceOrderLoadingOverlay()
        }
    }
}

@Composable
private fun CartPricingFooter(
    pricing: OrderPricing?,
    grandTotal: Double,
    hasValidDeliveryAddress: Boolean,
    isServicePayment: Boolean = false,
) {
    if (pricing == null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Subtotal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = money(grandTotal),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (hasValidDeliveryAddress) {
                if (isServicePayment) {
                    "No se pudo calcular el envío. El servicio no tiene ubicación válida configurada."
                } else {
                    "No se pudo calcular el envío. La tienda no tiene ubicación válida configurada."
                }
            } else {
                "El costo de envío se calculará al tener una dirección de entrega válida."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
    } else {
        PricingLine(
            label = if (isServicePayment) "Subtotal servicios" else "Subtotal productos",
            amount = pricing.productsSubtotal,
        )
        Spacer(modifier = Modifier.height(4.dp))
        PricingLine(label = "Tarifa de servicio", amount = pricing.serviceFee)
        Spacer(modifier = Modifier.height(4.dp))
        PricingLine(label = "Envío", amount = pricing.delivery.finalDeliveryFee)
        if (pricing.delivery.dynamicMultiplier > 1.0) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Incluye tarifa dinámica (×${String.format(Locale.US, "%.2f", pricing.delivery.dynamicMultiplier)})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(8.dp))
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Total",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = money(grandTotal),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun PricingLine(
    label: String,
    amount: Double,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = money(amount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun money(value: Double): String =
    "$${String.format(Locale.US, "%.2f", value)}"

@Composable
private fun CartDetailsSection(
    addressLabel: String,
    addressText: String,
    addressDetails: String?,
    estimatedDeliveryTime: String,
    paymentMethod: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CartListBackground)
    ) {
        DetailBlock(
            title = addressLabel.ifBlank { "Casa" },
            icon = Icons.Default.LocationOn,
            content = addressText.ifBlank { "Añade una dirección de entrega" }
        )
        HorizontalDivider(
            modifier = Modifier.padding(start = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
        if (!addressDetails.isNullOrBlank()) {
            DetailBlock(
                title = "Detalles",
                icon = Icons.Default.Info,
                content = addressDetails
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )
        }
        DetailBlock(
            title = "Entrega estimada",
            icon = Icons.Default.Schedule,
            content = estimatedDeliveryTime
        )
        HorizontalDivider(
            modifier = Modifier.padding(start = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
        DetailBlock(
            title = "Método de pago",
            icon = Icons.Default.Payment,
            content = paymentMethod
        )
    }
}

@Composable
private fun DetailBlock(
    title: String,
    icon: ImageVector,
    content: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 7.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        ) {
            LoadingAsyncImage(
                model = item.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.isServicePayment) {
                val number = item.serviceNumber?.takeIf { it.isNotBlank() }
                    ?: item.description?.removePrefix("Nº ")?.trim().orEmpty()
                if (number.isNotEmpty()) {
                    Text(
                        text = "Nº $number",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Text(
                    text = "$${String.format(Locale.US, "%.2f", item.lineTotal)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            } else {
                Text(
                    text = "${item.quantity} × $${String.format(Locale.US, "%.2f", item.chargedUnitPrice)} = $${String.format(Locale.US, "%.2f", item.lineTotal)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (item.hasDiscount) {
                val d = item.discount.coerceIn(0, 100)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFFE34D))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "-$d%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", item.originalUnitPrice)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = TextDecoration.LineThrough,
                    )
                }
            }
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Eliminar",
                tint = Color(0xFF595959),
            )
        }
    }
}
