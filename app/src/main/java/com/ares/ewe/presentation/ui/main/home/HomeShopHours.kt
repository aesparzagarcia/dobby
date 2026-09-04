package com.ares.ewe.presentation.ui.main.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ares.ewe.domain.model.FeaturedPlace
import com.ares.ewe.domain.model.BestSellerProduct
import com.ares.ewe.domain.model.ShopProduct
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * Recomputes open/closed when the app resumes or every minute.
 * Without this, [isPlaceOpenNow] is frozen at first composition (e.g. "Cerrado" after unlocking the phone the next day).
 */
@Composable
fun rememberIsPlaceOpenNow(
    openingHour: String?,
    closingHour: String?,
    openingDays: List<String> = emptyList(),
): Boolean? {
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshKey by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshKey++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(refreshKey) {
        while (true) {
            delay(60_000L)
            refreshKey++
        }
    }

    return remember(openingHour, closingHour, openingDays, refreshKey) {
        isPlaceOpenNow(openingHour, closingHour, openingDays)
    }
}

private val WEEKDAY_CODES = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

private fun normalizedDays(raw: List<String>?): Set<String> {
    val cleaned = raw.orEmpty()
        .map { it.trim().uppercase(Locale.US) }
        .filter { it in WEEKDAY_CODES }
        .toSet()
    return if (cleaned.isEmpty()) WEEKDAY_CODES.toSet() else cleaned
}

private fun weekdayCode(date: java.time.LocalDate = java.time.LocalDate.now()): String =
    when (date.dayOfWeek) {
        java.time.DayOfWeek.MONDAY -> "MON"
        java.time.DayOfWeek.TUESDAY -> "TUE"
        java.time.DayOfWeek.WEDNESDAY -> "WED"
        java.time.DayOfWeek.THURSDAY -> "THU"
        java.time.DayOfWeek.FRIDAY -> "FRI"
        java.time.DayOfWeek.SATURDAY -> "SAT"
        java.time.DayOfWeek.SUNDAY -> "SUN"
    }

private fun weekdayNameEs(code: String): String =
    when (code) {
        "MON" -> "lunes"
        "TUE" -> "martes"
        "WED" -> "miércoles"
        "THU" -> "jueves"
        "FRI" -> "viernes"
        "SAT" -> "sábado"
        "SUN" -> "domingo"
        else -> code.lowercase(Locale.getDefault())
    }

/** Whether the place is open now; `null` if hours are unknown. */
fun isPlaceOpenNow(
    openingHour: String?,
    closingHour: String?,
    openingDays: List<String> = emptyList(),
): Boolean? {
    val days = normalizedDays(openingDays)
    val today = weekdayCode()
    val open = parseHour(openingHour)
    val close = parseHour(closingHour)
    if (open == null || close == null) {
        if (days.size < 7 && today !in days) return false
        return null
    }
    val now = LocalTime.now()
    val overnight = close.isBefore(open)
    if (overnight) {
        return if (!now.isBefore(open)) {
            today in days
        } else {
            weekdayCode(java.time.LocalDate.now().minusDays(1)) in days
        }
    }
    if (today !in days) return false
    return !now.isBefore(open) && now.isBefore(close)
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
    openingDays: List<String> = emptyList(),
): Boolean {
    if (shopStatus != null && shopStatus != "ACTIVE") return false
    return isPlaceOpenNow(openingHour, closingHour, openingDays) != false
}

/** e.g. "Abre hoy a las 8:00 AM" when closed outside hours; null if inactive or hours unknown. */
fun formatShopReopensLabel(
    shopStatus: String?,
    openingHour: String?,
    openingDays: List<String> = emptyList(),
): String? {
    if (shopStatus != null && shopStatus != "ACTIVE") return null
    val openRaw = openingHour?.trim().orEmpty()
    if (openRaw.isEmpty()) return null
    parseHour(openRaw) ?: return null
    val days = normalizedDays(openingDays)
    val today = weekdayCode()
    val now = LocalTime.now()
    val open = parseHour(openRaw)
    if (open != null && today in days && now.isBefore(open)) {
        return "Abre hoy a las ${formatHour12(openRaw)}"
    }
    for (offset in 1..7) {
        val date = java.time.LocalDate.now().plusDays(offset.toLong())
        val code = weekdayCode(date)
        if (code in days) {
            return if (offset == 1) {
                "Abre mañana a las ${formatHour12(openRaw)}"
            } else {
                "Abre el ${weekdayNameEs(code)} a las ${formatHour12(openRaw)}"
            }
        }
    }
    return "Abre a las ${formatHour12(openRaw)}"
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
        openingDays = shop.openingDays,
    )
}

/** Open/available featured places first; preserves API order within each group. */
fun isFeaturedPlaceAvailable(place: FeaturedPlace): Boolean =
    isPlaceOpenNow(place.openingHour, place.closingHour, place.openingDays) != false

fun sortFeaturedPlacesByAvailability(places: List<FeaturedPlace>): List<FeaturedPlace> =
    places
        .withIndex()
        .sortedWith(
            compareBy<IndexedValue<FeaturedPlace>> { (_, place) ->
                if (isFeaturedPlaceAvailable(place)) 0 else 1
            }.thenBy { (index, _) -> index },
        )
        .map { it.value }

/** Available shops first; preserves sales/API order within each group. */
fun sortBestSellersByShopAvailability(
    products: List<BestSellerProduct>,
    featuredPlaces: List<FeaturedPlace>,
): List<BestSellerProduct> = sortProductsByShopAvailability(products, featuredPlaces) { it.shopId }

fun sortShopProductsByShopAvailability(
    products: List<ShopProduct>,
    featuredPlaces: List<FeaturedPlace>,
): List<ShopProduct> = sortProductsByShopAvailability(products, featuredPlaces) { it.shopId }

private fun <T> sortProductsByShopAvailability(
    products: List<T>,
    featuredPlaces: List<FeaturedPlace>,
    shopId: (T) -> String?,
): List<T> =
    products
        .withIndex()
        .sortedWith(
            compareBy<IndexedValue<T>> { (_, product) ->
                if (isProductShopAvailableForOrders(shopId(product), featuredPlaces)) 0 else 1
            }.thenBy { (index, _) -> index },
        )
        .map { it.value }

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
