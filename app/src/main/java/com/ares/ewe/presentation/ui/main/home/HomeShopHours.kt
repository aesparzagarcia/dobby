package com.ares.ewe.presentation.ui.main.home

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
