package com.ares.ewe.core.pricing

/** Solo hasta que llegue `serviceFeeRate` de `app/delivery-pricing-config`. */
const val DEFAULT_SERVICE_FEE_RATE = 0.08
/** IVA México por defecto; puede venir de `app/delivery-pricing-config`. */
const val DEFAULT_IVA_RATE = 0.16

data class OrderPricing(
    val productsSubtotal: Double,
    val delivery: DeliveryPricingBreakdown,
    val ivaRate: Double = DEFAULT_IVA_RATE,
    /** Fracción del subtotal (ej. 0.10). Viene del servidor. */
    val serviceFeeRate: Double = DEFAULT_SERVICE_FEE_RATE,
) {
    /** Tarifa de uso de la app con IVA incluido. */
    val serviceFee: Double =
        roundMoney(productsSubtotal * normalizeServiceFeeRate(serviceFeeRate))

    /** Base de la tarifa (sin IVA). */
    val serviceFeeNet: Double =
        splitServiceFeeIva(serviceFee, ivaRate).net

    /** IVA sobre la tarifa de uso. */
    val serviceFeeIva: Double =
        splitServiceFeeIva(serviceFee, ivaRate).iva

    val grandTotal: Double =
        roundMoney(productsSubtotal + serviceFee + delivery.finalDeliveryFee)
}

data class ServiceFeeIvaSplit(
    val net: Double,
    val iva: Double,
)

/** Desglosa una tarifa con IVA incluido en neto + IVA. */
fun splitServiceFeeIva(
    serviceFeeWithIva: Double,
    ivaRate: Double = DEFAULT_IVA_RATE,
): ServiceFeeIvaSplit {
    if (serviceFeeWithIva <= 0.0) return ServiceFeeIvaSplit(0.0, 0.0)
    val rate = normalizeIvaRate(ivaRate)
    if (rate <= 0.0) return ServiceFeeIvaSplit(roundMoney(serviceFeeWithIva), 0.0)
    val net = roundMoney(serviceFeeWithIva / (1.0 + rate))
    val iva = roundMoney(serviceFeeWithIva - net)
    return ServiceFeeIvaSplit(net = net, iva = iva)
}

/** Acepta 10 como porcentaje o 0.10 como fracción. */
fun normalizeServiceFeeRate(raw: Double, fallback: Double = DEFAULT_SERVICE_FEE_RATE): Double {
    if (!raw.isFinite()) return fallback
    val fraction = if (raw > 1.0) raw / 100.0 else raw
    if (fraction < 0.0 || fraction > 1.0) return fallback
    return kotlin.math.round(fraction * 10000.0) / 10000.0
}

fun normalizeIvaRate(raw: Double): Double {
    if (!raw.isFinite()) return DEFAULT_IVA_RATE
    val fraction = if (raw > 1.0) raw / 100.0 else raw
    if (fraction < 0.0 || fraction > 1.0) return DEFAULT_IVA_RATE
    return kotlin.math.round(fraction * 10000.0) / 10000.0
}

fun roundMoney(value: Double): Double =
    kotlin.math.round(value * 100.0) / 100.0
