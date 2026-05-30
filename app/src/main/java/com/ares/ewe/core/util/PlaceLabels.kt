package com.ares.ewe.core.util

fun shopTypeLabelEs(type: String?): String = when (type) {
    "RESTAURANT" -> "Restaurante"
    "SHOP" -> "Tienda"
    "SERVICE_PROVIDER" -> "Servicios"
    else -> type?.replace('_', ' ')?.replaceFirstChar { it.uppercase() } ?: "Tienda"
}

fun serviceCategoryLabelEs(category: String?): String? {
    val c = category?.trim().orEmpty()
    if (c.isEmpty()) return null
    return when (c.uppercase()) {
        "INTERNET" -> "Internet"
        "UTILITIES" -> "Servicios públicos"
        "OTHER" -> "Otros"
        else -> c.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }
}
