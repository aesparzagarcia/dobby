package com.ares.ewe.presentation.ui.main.home

import com.ares.ewe.domain.model.FeaturedPlace
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Whether the place is open now; `null` if hours are unknown. */
fun isPlaceOpenNow(openingHour: String?, closingHour: String?): Boolean? {
    val open = parseHour(openingHour) ?: return null
    val close = parseHour(closingHour) ?: return null
    val now = LocalTime.now()
    return if (close.isAfter(open) || close == open) {
        !now.isBefore(open) && now.isBefore(close)
    } else {
        // Overnight window (e.g. 22:00 – 02:00)
        !now.isBefore(open) || now.isBefore(close)
    }
}

fun formatPlaceHoursRange(openingHour: String?, closingHour: String?): String? {
    val open = openingHour?.trim().orEmpty()
    val close = closingHour?.trim().orEmpty()
    if (open.isEmpty() || close.isEmpty()) return null
    return "${formatHour12(open)} - ${formatHour12(close)}"
}

/** Orders allowed when shop is ACTIVE and within opening hours (unknown hours → treat as open). */
fun isShopAvailableForOrders(
    shopStatus: String?,
    openingHour: String?,
    closingHour: String?,
): Boolean {
    if (shopStatus != null && shopStatus != "ACTIVE") return false
    return isPlaceOpenNow(openingHour, closingHour) != false
}

/** e.g. "Abre hoy a las 8:00 AM" when closed outside hours; null if inactive or hours unknown. */
fun formatShopReopensLabel(
    shopStatus: String?,
    openingHour: String?,
): String? {
    if (shopStatus != null && shopStatus != "ACTIVE") return null
    val openRaw = openingHour?.trim().orEmpty()
    if (openRaw.isEmpty()) return null
    parseHour(openRaw) ?: return null
    return "Abre hoy a las ${formatHour12(openRaw)}"
}

/** Home/promotions list items: match shop hours from featured places (ACTIVE shops on /home). */
fun isProductShopAvailableForOrders(
    shopId: String?,
    featuredPlaces: List<FeaturedPlace>,
): Boolean {
    val id = shopId?.trim().orEmpty()
    if (id.isEmpty()) return false
    val shop = featuredPlaces.find { it.id == id && !it.isService } ?: return false
    return isShopAvailableForOrders(
        shopStatus = "ACTIVE",
        openingHour = shop.openingHour,
        closingHour = shop.closingHour,
    )
}

private fun parseHour(raw: String?): LocalTime? {
    val s = raw?.trim().orEmpty()
    if (s.isEmpty()) return null
    return try {
        LocalTime.parse(s, DateTimeFormatter.ofPattern("H:mm"))
    } catch (_: Exception) {
        try {
            LocalTime.parse(s, DateTimeFormatter.ofPattern("HH:mm"))
        } catch (_: Exception) {
            null
        }
    }
}

private fun formatHour12(raw: String): String {
    val t = parseHour(raw) ?: return raw
    val h = if (t.hour == 0) 12 else if (t.hour > 12) t.hour - 12 else t.hour
    val amPm = if (t.hour < 12) "AM" else "PM"
    return String.format(Locale.US, "%d:%02d %s", h, t.minute, amPm)
}
