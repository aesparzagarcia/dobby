package com.ares.ewe.presentation.ui.main.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ares.ewe.presentation.components.LoadingAsyncImage
import com.ares.ewe.BuildConfig
import com.ares.ewe.core.theme.DobbyColors
import com.ares.ewe.domain.model.OrderTracking
import com.ares.ewe.domain.model.OrderTrackingDeliveryMan
import com.ares.ewe.domain.model.OrderTrackingItem

private object OrderTrackingSheetPalette {
    val Primary = DobbyColors.Primary
    val TitleDark = DobbyColors.Dark
    val StatusBackground = DobbyColors.Light
    val StatusSubtitle = Color(0x560B1185)
    val Muted = Color(0xFF8E8E93)
    val TotalBarBackground = DobbyColors.Light
    val IconTileBackground = Color(0xFFF3F4F6)
    val ProductThumbBorder = Color(0xFFE5E7EB)
    val CheckBadgeBackground = Primary.copy(alpha = 0.18f)
}

/** Slightly compact sheet metrics (≈2% smaller than base). */
private object OrderTrackingSheetDim {
    private const val F = 0.98f

    private fun d(v: Number): Dp = (v.toFloat() * F).dp
    private fun s(v: Number): TextUnit = (v.toFloat() * F).sp

    val sectionGap = d(10)
    /** Tighter gaps: shop→productos, productos→precios, total→repartidor. */
    val tightBlockGap = d(5)
    val productsInnerGap = d(3)
    val rowGap = d(6)
    /** Tight gap between stacked title + subtitle (status, shop, courier). */
    val textStackGap = d(0)
    /** Subtotal / envío rows. */
    val pricingRowGap = d(3)
    val gapXs = d(2)
    val gapSm = d(3)
    val gapMd = d(5)
    val gapLg = d(6)
    val padH = d(10)
    val padH14 = d(11)
    val padV12 = d(9)
    val padV14 = d(10)
    val padStart10 = d(8)
    val radiusSm = d(8)
    val radiusMd = d(10)
    val radiusLg = d(12)
    val iconSm = d(16)
    val iconMd = d(18)
    val iconLg = d(20)
    val tile = d(36)
    val thumb = d(42)
    val avatar = d(40)
    val checkBadge = d(28)
    val border = d(1)

    val title = s(17)
    val sectionLabel = s(10)
    val sectionLabelTracking = s(0.6)
    val statusTitle = s(14)
    val statusSubtitle = s(12)
    val statusTitleLine = s(17)
    val statusSubtitleLine = s(14)
    val body = s(13)
    val bodySm = s(11)
    val bodyLine = s(15)
    val bodySmLine = s(13)
    val caption = s(11)
    val price = s(12)
    val priceLine = s(14)
    val total = s(14)
    val deliveryCode = 36.sp
}

@Composable
fun OrderTrackingBottomSheetContent(
    tracking: OrderTracking,
    rateSubmitting: Boolean,
    rateError: String?,
    onSubmitDeliveryRating: (Int) -> Unit,
    onSubmitShopRating: (Int) -> Unit,
    onSubmitProductRating: (String, Int) -> Unit,
    onClearRateError: () -> Unit,
    onFinish: () -> Unit,
    fullScreen: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier.verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(OrderTrackingSheetDim.sectionGap),
    ) {
        Text(
            text = "Tu pedido",
            fontSize = OrderTrackingSheetDim.title,
            fontWeight = FontWeight.Bold,
            color = OrderTrackingSheetPalette.TitleDark,
        )

        OrderTrackingStatusCard(tracking = tracking)

        if (tracking.courierArrivedAtCustomer && tracking.isOnDelivery) {
            val code = tracking.deliveryCode?.trim().orEmpty()
            if (code.isNotEmpty()) {
                OrderTrackingDeliveryCodeCard(code = code)
            } else {
                OrderTrackingDeliveryCodePendingCard()
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(OrderTrackingSheetDim.tightBlockGap)) {
            tracking.shopName?.let { shopName ->
                OrderTrackingShopRow(
                    shopName = shopName,
                    shopAddress = tracking.shopAddress ?: tracking.deliveryAddress,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(OrderTrackingSheetDim.productsInnerGap)) {
                Text(
                    text = "PRODUCTOS",
                    fontSize = OrderTrackingSheetDim.sectionLabel,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = OrderTrackingSheetDim.sectionLabelTracking,
                    color = OrderTrackingSheetPalette.Muted,
                )
                tracking.items.forEach { item ->
                    OrderTrackingProductRow(item = item)
                }
            }

            val productsSubtotal = tracking.productsSubtotal.takeIf { it > 0 }
                ?: tracking.items.sumOf { it.price * it.quantity }
            OrderTrackingPricingSection(
                productsSubtotal = productsSubtotal,
                serviceFee = tracking.serviceFee,
                deliveryFee = tracking.deliveryFee,
                total = tracking.total,
            )

            OrderTrackingCourierFooter(tracking = tracking)
        }

        if (tracking.isDelivered) {
            OrderTrackingDeliveredRatings(
                tracking = tracking,
                rateSubmitting = rateSubmitting,
                rateError = rateError,
                onSubmitDeliveryRating = onSubmitDeliveryRating,
                onSubmitShopRating = onSubmitShopRating,
                onSubmitProductRating = onSubmitProductRating,
                onClearRateError = onClearRateError,
            )
        } else if (tracking.canRateDelivery) {
            Text(
                text = "¿Cómo fue el reparto?",
                style = MaterialTheme.typography.titleMedium,
                color = OrderTrackingSheetPalette.Primary,
            )
            StarRatingBlock(
                title = "Repartidor",
                subtitle = tracking.deliveryMan?.name ?: "",
                existingRating = null,
                canRate = true,
                rateSubmitting = rateSubmitting,
                onSelect = onSubmitDeliveryRating,
            )
        }

        if (shouldShowFinishButton(tracking)) {
            Button(
                onClick = onFinish,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = OrderTrackingSheetDim.gapSm),
            ) {
                Text("Finalizar")
            }
        }
    }
}

@Composable
private fun OrderTrackingStatusCard(tracking: OrderTracking) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(OrderTrackingSheetPalette.StatusBackground, RoundedCornerShape(OrderTrackingSheetDim.radiusLg))
            .padding(horizontal = OrderTrackingSheetDim.padH14, vertical = OrderTrackingSheetDim.padV14),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(OrderTrackingSheetDim.tile)
                .clip(RoundedCornerShape(OrderTrackingSheetDim.radiusMd))
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = statusCardIcon(tracking.status),
                contentDescription = null,
                tint = OrderTrackingSheetPalette.Primary,
                modifier = Modifier.size(OrderTrackingSheetDim.iconMd),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = OrderTrackingSheetDim.padH),
            verticalArrangement = Arrangement.spacedBy(OrderTrackingSheetDim.textStackGap),
        ) {
            Text(
                text = trackingStatusTitle(tracking),
                fontSize = OrderTrackingSheetDim.statusTitle,
                lineHeight = OrderTrackingSheetDim.statusTitleLine,
                fontWeight = FontWeight.Bold,
                color = OrderTrackingSheetPalette.TitleDark,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = trackingStatusSubtitle(tracking),
                fontSize = OrderTrackingSheetDim.statusSubtitle,
                lineHeight = OrderTrackingSheetDim.statusSubtitleLine,
                color = OrderTrackingSheetPalette.StatusSubtitle,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showsStatusCheckBadge(tracking.status)) {
            Box(
                modifier = Modifier
                    .size(OrderTrackingSheetDim.checkBadge)
                    .clip(CircleShape)
                    .background(OrderTrackingSheetPalette.CheckBadgeBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = OrderTrackingSheetPalette.Primary,
                    modifier = Modifier.size(OrderTrackingSheetDim.iconSm),
                )
            }
        }
    }
}

@Composable
private fun OrderTrackingShopRow(shopName: String, shopAddress: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(OrderTrackingSheetDim.tile)
                .clip(RoundedCornerShape(OrderTrackingSheetDim.radiusMd))
                .background(OrderTrackingSheetPalette.IconTileBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Store,
                contentDescription = null,
                tint = OrderTrackingSheetPalette.Primary,
                modifier = Modifier.size(OrderTrackingSheetDim.iconMd),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = OrderTrackingSheetDim.padH),
            verticalArrangement = Arrangement.spacedBy(OrderTrackingSheetDim.textStackGap),
        ) {
            Text(
                text = "Tienda: $shopName",
                fontSize = OrderTrackingSheetDim.body,
                lineHeight = OrderTrackingSheetDim.bodyLine,
                fontWeight = FontWeight.SemiBold,
                color = OrderTrackingSheetPalette.TitleDark,
            )
            shopAddress?.let { address ->
                Text(
                    text = address,
                    fontSize = OrderTrackingSheetDim.bodySm,
                    lineHeight = OrderTrackingSheetDim.bodySmLine,
                    color = OrderTrackingSheetPalette.Muted,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun OrderTrackingProductRow(item: OrderTrackingItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(OrderTrackingSheetDim.thumb)
                .clip(RoundedCornerShape(OrderTrackingSheetDim.radiusSm))
                .border(
                    OrderTrackingSheetDim.border,
                    OrderTrackingSheetPalette.ProductThumbBorder,
                    RoundedCornerShape(OrderTrackingSheetDim.radiusSm),
                )
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            LoadingAsyncImage(
                model = item.imageUrl,
                contentDescription = item.productName,
                modifier = Modifier
                    .size(OrderTrackingSheetDim.thumb)
                    .clip(RoundedCornerShape(OrderTrackingSheetDim.radiusSm)),
                contentScale = ContentScale.Crop,
                error = {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = OrderTrackingSheetPalette.Muted,
                        modifier = Modifier.size(OrderTrackingSheetDim.iconLg),
                    )
                },
            )
        }
        Text(
            text = "${item.productName} x${item.quantity}",
            fontSize = OrderTrackingSheetDim.body,
            color = OrderTrackingSheetPalette.TitleDark,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = OrderTrackingSheetDim.padH),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "$${String.format("%.2f", item.price * item.quantity)}",
            fontSize = OrderTrackingSheetDim.body,
            fontWeight = FontWeight.SemiBold,
            color = OrderTrackingSheetPalette.TitleDark,
        )
    }
}

@Composable
private fun OrderTrackingPricingSection(
    productsSubtotal: Double,
    serviceFee: Double,
    deliveryFee: Double,
    total: Double,
) {
    Column(verticalArrangement = Arrangement.spacedBy(OrderTrackingSheetDim.pricingRowGap)) {
        PricingLine(label = "Subtotal productos", amount = productsSubtotal)
        if (serviceFee > 0) {
            PricingLine(label = "Tarifa de servicio", amount = serviceFee)
        }
        if (deliveryFee > 0) {
            PricingLine(label = "Envío", amount = deliveryFee)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = OrderTrackingSheetDim.gapXs)
                .background(OrderTrackingSheetPalette.TotalBarBackground, RoundedCornerShape(OrderTrackingSheetDim.radiusMd))
                .padding(horizontal = OrderTrackingSheetDim.padH14, vertical = OrderTrackingSheetDim.padV12),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Total",
                fontSize = OrderTrackingSheetDim.total,
                fontWeight = FontWeight.Bold,
                color = OrderTrackingSheetPalette.Primary,
            )
            Text(
                text = "$${String.format("%.2f", total)}",
                fontSize = OrderTrackingSheetDim.total,
                fontWeight = FontWeight.Bold,
                color = OrderTrackingSheetPalette.Primary,
            )
        }
    }
}

@Composable
private fun PricingLine(label: String, amount: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            fontSize = OrderTrackingSheetDim.price,
            lineHeight = OrderTrackingSheetDim.priceLine,
            color = OrderTrackingSheetPalette.Muted,
        )
        Text(
            text = "$${String.format("%.2f", amount)}",
            fontSize = OrderTrackingSheetDim.price,
            lineHeight = OrderTrackingSheetDim.priceLine,
            color = OrderTrackingSheetPalette.Muted,
        )
    }
}

@Composable
private fun OrderTrackingCourierFooter(tracking: OrderTracking) {
    Column(modifier = Modifier.fillMaxWidth()) {
        DashedDivider(modifier = Modifier.padding(top = OrderTrackingSheetDim.gapXs))
        tracking.deliveryMan?.let { dm ->
            CourierAssignedCard(dm = dm)
        } ?: CourierUnassignedCard()
    }
}

@Composable
private fun CourierUnassignedCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = OrderTrackingSheetDim.gapSm),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = OrderTrackingSheetPalette.Primary,
            modifier = Modifier.size(OrderTrackingSheetDim.iconMd),
        )
        Column(
            modifier = Modifier.padding(start = OrderTrackingSheetDim.padStart10),
            verticalArrangement = Arrangement.spacedBy(OrderTrackingSheetDim.textStackGap),
        ) {
            Text(
                text = "Aún no se ha asignado un repartidor.",
                fontSize = OrderTrackingSheetDim.body,
                lineHeight = OrderTrackingSheetDim.bodyLine,
                fontWeight = FontWeight.SemiBold,
                color = OrderTrackingSheetPalette.TitleDark,
            )
            Text(
                text = "Te notificaremos cuando tu pedido esté en camino.",
                fontSize = OrderTrackingSheetDim.bodySm,
                lineHeight = OrderTrackingSheetDim.bodySmLine,
                color = OrderTrackingSheetPalette.Muted,
            )
        }
    }
}

@Composable
private fun CourierAssignedCard(dm: OrderTrackingDeliveryMan) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = OrderTrackingSheetDim.gapSm)
            .clip(RoundedCornerShape(OrderTrackingSheetDim.radiusMd))
            .background(OrderTrackingSheetPalette.IconTileBackground)
            .then(
                if (!dm.celphone.isNullOrBlank()) {
                    Modifier.clickable {
                        context.startActivity(
                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:${dm.celphone}")),
                        )
                    }
                } else {
                    Modifier
                },
            )
            .padding(OrderTrackingSheetDim.padH),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val courierImageUrl = dm.profilePhotoUrl?.let { url ->
            if (url.startsWith("http")) url
            else BuildConfig.BASE_URL.substringBefore("api/").dropLast(1) + url
        }
        if (!courierImageUrl.isNullOrBlank()) {
            LoadingAsyncImage(
                model = courierImageUrl,
                contentDescription = dm.name,
                modifier = Modifier
                    .size(OrderTrackingSheetDim.avatar)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                placeholderBackground = Color.White,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(OrderTrackingSheetDim.avatar)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = OrderTrackingSheetPalette.Muted,
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = OrderTrackingSheetDim.padH),
        ) {
            Text(
                text = "Repartidor",
                fontSize = OrderTrackingSheetDim.caption,
                fontWeight = FontWeight.SemiBold,
                color = OrderTrackingSheetPalette.Primary,
            )
            Text(
                text = dm.name,
                fontSize = OrderTrackingSheetDim.body,
                fontWeight = FontWeight.SemiBold,
                color = OrderTrackingSheetPalette.TitleDark,
            )
        }
        if (!dm.celphone.isNullOrBlank()) {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = "Llamar",
                tint = OrderTrackingSheetPalette.Primary,
            )
        }
    }
}

@Composable
private fun DashedDivider(modifier: Modifier = Modifier) {
    val color = OrderTrackingSheetPalette.ProductThumbBorder
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(OrderTrackingSheetDim.border),
    ) {
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f),
        )
    }
}

@Composable
private fun OrderTrackingDeliveredRatings(
    tracking: OrderTracking,
    rateSubmitting: Boolean,
    rateError: String?,
    onSubmitDeliveryRating: (Int) -> Unit,
    onSubmitShopRating: (Int) -> Unit,
    onSubmitProductRating: (String, Int) -> Unit,
    onClearRateError: () -> Unit,
) {
    Text(
        text = "Valora tu experiencia",
        style = MaterialTheme.typography.titleMedium,
        color = OrderTrackingSheetPalette.Primary,
        modifier = Modifier.padding(top = OrderTrackingSheetDim.gapSm),
    )
    if (tracking.canRateShop || tracking.shopRating != null) {
        StarRatingBlock(
            title = "Restaurante",
            subtitle = tracking.shopName ?: "Tienda",
            existingRating = tracking.shopRating,
            canRate = tracking.canRateShop,
            rateSubmitting = rateSubmitting,
            onSelect = onSubmitShopRating,
        )
    }
    if (tracking.deliveryMan != null && (tracking.canRateDelivery || tracking.deliveryRating != null)) {
        StarRatingBlock(
            title = "Repartidor",
            subtitle = tracking.deliveryMan.name,
            existingRating = tracking.deliveryRating,
            canRate = tracking.canRateDelivery,
            rateSubmitting = rateSubmitting,
            onSelect = onSubmitDeliveryRating,
        )
    }
    tracking.items.filter { it.canRate || it.rating != null }.forEach { item ->
        StarRatingBlock(
            title = "Producto",
            subtitle = "${item.productName} x${item.quantity}",
            existingRating = item.rating,
            canRate = item.canRate,
            rateSubmitting = rateSubmitting,
            onSelect = { onSubmitProductRating(item.productId, it) },
        )
    }
    if (rateSubmitting) {
        Text(text = "Enviando…", style = MaterialTheme.typography.bodySmall)
    }
    rateError?.let { err ->
        Text(
            text = err,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.clickable { onClearRateError() },
        )
    }
}

@Composable
private fun StarRatingBlock(
    title: String,
    subtitle: String,
    existingRating: Int?,
    canRate: Boolean,
    rateSubmitting: Boolean,
    onSelect: (Int) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = OrderTrackingSheetDim.gapSm)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = OrderTrackingSheetPalette.Primary,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = OrderTrackingSheetPalette.Muted,
        )
        if (canRate) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(OrderTrackingSheetDim.rowGap),
                modifier = Modifier.padding(top = OrderTrackingSheetDim.gapMd),
            ) {
                for (s in 1..5) {
                    FilterChip(
                        selected = false,
                        onClick = { if (!rateSubmitting) onSelect(s) },
                        enabled = !rateSubmitting,
                        label = { Text("$s ⭐") },
                    )
                }
            }
        } else if (existingRating != null) {
            val r = existingRating.coerceIn(1, 5)
            Text(
                text = "Tu valoración: ${"⭐".repeat(r)}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = OrderTrackingSheetDim.gapSm),
            )
        }
    }
}

private fun shouldShowFinishButton(tracking: OrderTracking): Boolean =
    tracking.isDelivered && !tracking.hasPendingRatings

@Composable
private fun OrderTrackingDeliveryCodePendingCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OrderTrackingSheetPalette.StatusBackground, RoundedCornerShape(OrderTrackingSheetDim.radiusLg))
            .padding(horizontal = OrderTrackingSheetDim.padH14, vertical = OrderTrackingSheetDim.padV14),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(OrderTrackingSheetDim.gapMd),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(OrderTrackingSheetDim.iconLg),
            color = OrderTrackingSheetPalette.Primary,
            strokeWidth = 2.dp,
        )
        Text(
            text = "Tu código de entrega",
            fontSize = OrderTrackingSheetDim.sectionLabel,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = OrderTrackingSheetDim.sectionLabelTracking,
            color = OrderTrackingSheetPalette.Muted,
        )
        Text(
            text = "Estamos generando tu código. Si no aparece en unos segundos, actualiza la app o contacta soporte.",
            fontSize = OrderTrackingSheetDim.statusSubtitle,
            lineHeight = OrderTrackingSheetDim.statusSubtitleLine,
            color = OrderTrackingSheetPalette.StatusSubtitle,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun OrderTrackingDeliveryCodeCard(code: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OrderTrackingSheetPalette.StatusBackground, RoundedCornerShape(OrderTrackingSheetDim.radiusLg))
            .padding(horizontal = OrderTrackingSheetDim.padH14, vertical = OrderTrackingSheetDim.padV14),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(OrderTrackingSheetDim.gapMd),
    ) {
        Text(
            text = "Tu código de entrega",
            fontSize = OrderTrackingSheetDim.sectionLabel,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = OrderTrackingSheetDim.sectionLabelTracking,
            color = OrderTrackingSheetPalette.Muted,
        )
        Text(
            text = code,
            fontSize = OrderTrackingSheetDim.deliveryCode,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp,
            color = OrderTrackingSheetPalette.Primary,
        )
        Text(
            text = "Muéstralo al repartidor para confirmar la entrega.",
            fontSize = OrderTrackingSheetDim.statusSubtitle,
            lineHeight = OrderTrackingSheetDim.statusSubtitleLine,
            color = OrderTrackingSheetPalette.StatusSubtitle,
            textAlign = TextAlign.Center,
        )
    }
}

private fun trackingStatusTitle(tracking: OrderTracking): String {
    if (tracking.courierArrivedAtCustomer && tracking.status.equals("ON_DELIVERY", ignoreCase = true)) {
        val name = tracking.deliveryMan?.name?.trim().orEmpty()
        return if (name.isNotEmpty()) "$name está afuera" else "Repartidor afuera"
    }
    return statusLabel(tracking.status, tracking.isServicePayment, tracking.isCarWash)
}

private fun trackingStatusSubtitle(tracking: OrderTracking): String {
    if (tracking.courierArrivedAtCustomer && tracking.status.equals("ON_DELIVERY", ignoreCase = true)) {
        return if (!tracking.deliveryCode.isNullOrBlank()) {
            "Comparte tu código de entrega con el repartidor"
        } else {
            "Tu pedido te está esperando en la puerta"
        }
    }
    val service = tracking.isServicePayment
    val carWash = tracking.isCarWash
    return when (tracking.status.uppercase()) {
        "PENDING" -> if (service) "Procesando tu pago de servicios" else "Esperando confirmación de la tienda"
        "CONFIRMED" -> if (service) "Tu solicitud fue confirmada" else "Gracias por tu compra"
        "PREPARING" -> tracking.estimatedPreparationMinutes?.let { mins ->
            if (carWash) "Tiempo estimado de lavado: $mins min" else "Tiempo estimado de preparación: $mins min"
        } ?: if (carWash) "Tu vehículo se está lavando" else "Tu pedido se está preparando"
        "READY_FOR_PICKUP" -> when {
            service -> "Estamos buscando un repartidor para pagar tus servicios"
            carWash -> "Secado y aspirado en proceso"
            else -> "Listo para que el repartidor lo recoja"
        }
        "ASSIGNED" -> if (service) {
            tracking.estimatedDeliveryMinutes?.let { mins ->
                "Llegada estimada al punto de pago: ~$mins min"
            } ?: run {
                val name = tracking.deliveryMan?.name?.trim().orEmpty()
                if (name.isNotEmpty()) {
                    "El repartidor $name va en camino a pagar tus servicios"
                } else {
                    "El repartidor va en camino a pagar tus servicios"
                }
            }
        } else if (carWash) {
            tracking.estimatedDeliveryMinutes?.let { mins ->
                "Detallado en proceso · llegada estimada: ~$mins min"
            } ?: "Detallado en proceso"
        } else {
            tracking.estimatedDeliveryMinutes?.let { mins ->
                "Llegada estimada al recoger: ~$mins min"
            } ?: "Un repartidor irá por tu pedido pronto"
        }
        "ON_DELIVERY" -> tracking.estimatedDeliveryMinutes?.let { mins ->
            "Llegada estimada: ~$mins min"
        } ?: if (service) {
            "Tus servicios ya fueron pagados y van rumbo a tu domicilio"
        } else if (carWash) {
            "Tu vehículo va en camino a tu domicilio"
        } else {
            "Tu pedido va en camino a tu domicilio"
        }
        "DELIVERED" -> when {
            service -> "Tu pago de servicios fue entregado"
            carWash -> "¡Servicio completado!"
            else -> "¡Buen provecho!"
        }
        "CANCELLED" -> "Este pedido fue cancelado"
        else -> ""
    }
}

private fun showsStatusCheckBadge(status: String): Boolean =
    status.uppercase() !in setOf("PENDING", "CANCELLED")

private fun statusCardIcon(status: String) = when (status.uppercase()) {
    "PREPARING", "READY_FOR_PICKUP" -> Icons.Default.Inventory2
    else -> Icons.Default.ShoppingBag
}

private fun statusLabel(
    status: String,
    servicePayment: Boolean = false,
    carWash: Boolean = false,
): String = when (status.uppercase()) {
    "PENDING" -> "Pendiente"
    "CONFIRMED" -> "Confirmado"
    "PREPARING" -> if (carWash) "Lavando" else "En preparación"
    "READY_FOR_PICKUP" -> when {
        servicePayment -> "Buscando repartidor"
        carWash -> "Secado y Aspirado"
        else -> "Listo para recoger"
    }
    "ASSIGNED" -> if (carWash) "Detallado" else "Asignado a repartidor"
    "ON_DELIVERY" -> "En camino"
    "DELIVERED" -> "Entregado"
    "CANCELLED" -> "Cancelado"
    else -> status
}
