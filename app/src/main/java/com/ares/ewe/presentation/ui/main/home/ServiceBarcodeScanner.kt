package com.ares.ewe.presentation.ui.main.home

import android.content.Context
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

/**
 * Opens the Google Play services barcode scanner and returns digits from the raw value.
 */
fun launchServiceBarcodeScanner(
    context: Context,
    onDigits: (String) -> Unit,
) {
    val options = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_CODE_93,
            Barcode.FORMAT_CODABAR,
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_ITF,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_PDF417,
            Barcode.FORMAT_DATA_MATRIX,
            Barcode.FORMAT_AZTEC,
        )
        .enableAutoZoom()
        .build()
    GmsBarcodeScanning.getClient(context, options)
        .startScan()
        .addOnSuccessListener { barcode ->
            val digits = extractServiceNumberDigits(barcode.rawValue.orEmpty())
            if (digits.isNotEmpty()) {
                onDigits(digits)
            }
        }
}

fun extractServiceNumberDigits(raw: String): String = raw.filter { it.isDigit() }
