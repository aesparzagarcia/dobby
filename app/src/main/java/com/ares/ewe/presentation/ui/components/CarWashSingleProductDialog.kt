package com.ares.ewe.presentation.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ares.ewe.R

@Composable
fun CarWashSingleProductDialog(
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.car_wash_single_product_title)) },
        text = { Text(stringResource(R.string.car_wash_single_product_message)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.car_wash_single_product_ok))
            }
        },
    )
}
