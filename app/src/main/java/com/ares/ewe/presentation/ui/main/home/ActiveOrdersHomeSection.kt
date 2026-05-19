package com.ares.ewe.presentation.ui.main.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ares.ewe.domain.model.ActiveOrder

@Composable
fun ActiveOrdersHomeSection(
    activeOrders: List<ActiveOrder>,
    onTrackOrderClick: (String) -> Unit,
    onMultipleOrdersClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (activeOrders.size) {
        0 -> Unit
        1 -> OrderTrackingSection(
            activeOrder = activeOrders.first(),
            onViewClick = { onTrackOrderClick(activeOrders.first().id) },
            modifier = modifier,
        )
        else -> ActiveOrdersSummaryCard(
            activeCount = activeOrders.size,
            onClick = onMultipleOrdersClick,
            modifier = modifier,
        )
    }
}
