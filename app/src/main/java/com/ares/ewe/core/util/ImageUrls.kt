package com.ares.ewe.core.util

import com.ares.ewe.BuildConfig

private val imageBaseUrl: String
    get() = BuildConfig.BASE_URL.removeSuffix("api/").trimEnd('/')

/** Relative `/uploads/...` → absolute URL with current [BuildConfig.BASE_URL] host. */
fun String.toFullImageUrl(): String {
    val trimmed = trim()
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
    return if (trimmed.startsWith("/")) "$imageBaseUrl$trimmed" else "$imageBaseUrl/$trimmed"
}

/**
 * URL lista para Coil: relativa, absoluta vigente o absoluta con host viejo (solo se usa el path).
 * Favoritos guardan la ruta en SQLite; sin esto Coil no puede cargar `/uploads/...`.
 */
fun String?.toDisplayImageUrl(): String? {
    if (isNullOrBlank()) return null
    val trimmed = trim()
    val path = when {
        trimmed.startsWith("http://") || trimmed.startsWith("https://") -> {
            runCatching { java.net.URI(trimmed).path }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?: trimmed
        }
        else -> trimmed
    }
    return path.toFullImageUrl()
}

/** Persist only `/uploads/...` so changing DEV_API_HOST does not break cached favorites. */
fun String?.normalizeImageUrlForStorage(): String? {
    if (isNullOrBlank()) return null
    val trimmed = trim()
    return when {
        trimmed.startsWith("http://") || trimmed.startsWith("https://") -> {
            runCatching { java.net.URI(trimmed).path }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
        }
        else -> trimmed
    }
}
