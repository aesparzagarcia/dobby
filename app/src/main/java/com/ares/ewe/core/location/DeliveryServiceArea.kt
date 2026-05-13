package com.ares.ewe.core.location

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Zona desde `res/raw/service_area.kml` (primer anillo de coordenadas embebidas).
 *
 * **Bloqueo por configuración** solo si el KML no incluye ninguna etiqueta de coordenadas
 * (p. ej. export de My Maps solo con NetworkLink). Si la etiqueta existe pero el parseo falla,
 * no bloqueamos toda la app (evita el botón "Revisa área de entrega" por error de formato).
 */
@Singleton
class DeliveryServiceArea @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val config: AreaConfig by lazy { loadConfig() }

    fun hasValidEnforcedPolygon(): Boolean = (config.ring?.size ?: 0) >= 3

    fun isConfigBlockingSaves(): Boolean = config.kmlBundledButNoCoordinatesTag

    fun contains(latitude: Double, longitude: Double): Boolean {
        val r = config.ring
        if (r != null && r.size >= 3) {
            return GeoPolygon.contains(latitude, longitude, r)
        }
        if (config.kmlBundledButNoCoordinatesTag) {
            return false
        }
        return true
    }

    fun denialMessage(): String = when {
        config.kmlBundledButNoCoordinatesTag -> MISCONFIGURED_KML_MESSAGE
        config.polygonUnusable -> INVALID_POLYGON_MESSAGE
        else -> OUTSIDE_MESSAGE
    }

    private fun loadConfig(): AreaConfig {
        val id = context.resources.getIdentifier("service_area", "raw", context.packageName)
        if (id == 0) {
            return AreaConfig(ring = null, hasKmlRoot = false, kmlBundledButNoCoordinatesTag = false, polygonUnusable = false)
        }
        val raw = runCatching {
            context.resources.openRawResource(id).bufferedReader().use { it.readText() }
        }.getOrNull()?.trimBom().orEmpty()
        if (raw.isEmpty()) {
            return AreaConfig(null, false, false, false)
        }
        val xml = KmlCoordinateRingParser.stripXmlComments(raw)
        val hasKmlRoot = xml.contains("<kml", ignoreCase = true)
        val hasCoordinatesTag = xml.contains("<coordinates", ignoreCase = true)
        val ring = KmlCoordinateRingParser.parseFirstClosedRing(xml)
        val kmlBundledButNoCoordinatesTag = hasKmlRoot && !hasCoordinatesTag
        val polygonUnusable = hasCoordinatesTag && (ring == null || ring.size < 3)
        return AreaConfig(
            ring = ring,
            hasKmlRoot = hasKmlRoot,
            kmlBundledButNoCoordinatesTag = kmlBundledButNoCoordinatesTag,
            polygonUnusable = polygonUnusable
        )
    }

    private data class AreaConfig(
        val ring: List<LatLng>?,
        val hasKmlRoot: Boolean,
        val kmlBundledButNoCoordinatesTag: Boolean,
        val polygonUnusable: Boolean
    )

    companion object {
        const val OUTSIDE_MESSAGE =
            "Esta ubicación está fuera del área de entrega. Mueve el pin dentro del polígono permitido."

        const val MISCONFIGURED_KML_MESSAGE =
            "service_area.kml no incluye un polígono con coordenadas embebidas. My Maps a veces exporta solo un enlace: añade un Polygon con su lista de coordenadas o copia el ejemplo del repositorio."

        const val INVALID_POLYGON_MESSAGE =
            "No se pudo leer el polígono en service_area.kml. Revisa que la lista tenga al menos tres puntos lon,lat."

        const val OUTSIDE_LIMITS_LABEL = "Fuera de los límites"

        const val CONFIG_FIX_LABEL = "Revisa área de entrega"
    }
}

private fun String.trimBom(): String =
    if (startsWith("\uFEFF")) substring(1) else this

internal object GeoPolygon {
    fun contains(lat: Double, lng: Double, vertices: List<LatLng>): Boolean {
        if (vertices.size < 3) return false
        val closed = normalizedRing(vertices)
        val planar = PolyUtil.containsLocation(LatLng(lat, lng), closed, false)
        if (planar) return true
        return PolyUtil.containsLocation(LatLng(lat, lng), closed, true)
    }

    private fun normalizedRing(vertices: List<LatLng>): List<LatLng> {
        if (vertices.size < 2) return vertices
        val f = vertices.first()
        val l = vertices.last()
        val closedLoop = f.latitude == l.latitude && f.longitude == l.longitude
        return if (closedLoop) vertices.dropLast(1) else vertices
    }
}

internal object KmlCoordinateRingParser {

    fun stripXmlComments(xml: String): String =
        xml.replace(Regex("<!--[\\s\\S]*?-->"), "")

    fun parseFirstClosedRing(xml: String): List<LatLng>? {
        val startTag = xml.indexOf("<coordinates", ignoreCase = true)
        if (startTag < 0) return null
        val contentStart = xml.indexOf('>', startIndex = startTag)
        if (contentStart < 0) return null
        val endTag = xml.indexOf("</coordinates>", startIndex = contentStart, ignoreCase = true)
        if (endTag < 0) return null
        val inner = xml.substring(contentStart + 1, endTag).trim()
        if (inner.isEmpty()) return null

        parseLonLatOrder(inner, lonFirst = true)?.takeIf { it.size >= 3 }?.let { return closeRing(it) }
        parseLonLatOrder(inner, lonFirst = false)?.takeIf { it.size >= 3 }?.let { return closeRing(it) }
        return null
    }

    private fun parseLonLatOrder(inner: String, lonFirst: Boolean): List<LatLng>? {
        val points = ArrayList<LatLng>(32)
        for (token in inner.split(Regex("\\s+"))) {
            if (token.isBlank()) continue
            val parts = token.split(',')
            if (parts.size < 2) continue
            val a = parts[0].trim().toDoubleOrNull() ?: continue
            val b = parts[1].trim().toDoubleOrNull() ?: continue
            val lat: Double
            val lon: Double
            if (lonFirst) {
                lon = a
                lat = b
            } else {
                lat = a
                lon = b
            }
            if (lat !in -90.0..90.0 || lon !in -180.0..180.0) continue
            points.add(LatLng(lat, lon))
        }
        return points.takeIf { it.size >= 3 }
    }

    private fun closeRing(points: List<LatLng>): List<LatLng> {
        val out = ArrayList(points)
        val first = out.first()
        val last = out.last()
        if (first.latitude != last.latitude || first.longitude != last.longitude) {
            out.add(LatLng(first.latitude, first.longitude))
        }
        return out
    }
}
