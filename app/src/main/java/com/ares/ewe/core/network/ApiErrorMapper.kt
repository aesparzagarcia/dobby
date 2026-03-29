package com.ares.ewe.core.network

import com.google.gson.JsonParser
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import retrofit2.HttpException

/**
 * High-level classification for API / network failures shown in UI.
 */
enum class ApiErrorKind {
    /** Sin red, timeout, host inalcanzable, etc. */
    CONNECTION,

    /** 401 / 403 */
    AUTHORIZATION,

    /** 4xx distintos de auth (validación, no encontrado, etc.) */
    CLIENT,

    /** 5xx */
    SERVER,

    /** Otros */
    UNKNOWN,
}

/**
 * @param label Short title shown first so the user sees the *type* of error.
 * @param detail Human-readable explanation (Spanish).
 */
data class UserFacingApiError(
    val kind: ApiErrorKind,
    val label: String,
    val detail: String,
) {
    /** Single line for snackbars and existing [errorMessage] fields. */
    fun formattedMessage(): String = "$label: $detail"
}

fun Throwable.unwrap(): Throwable {
    var current = this
    while (current.cause != null && current.cause !== current) {
        current = current.cause!!
    }
    return current
}

private fun parseHttpErrorBody(e: HttpException): String? {
    return try {
        val body = e.response()?.errorBody()?.string().orEmpty()
        if (body.isBlank()) return null
        val el = JsonParser.parseString(body).asJsonObject
        when {
            el.has("error") && !el.get("error").isJsonNull -> el.get("error").asString.trim()
            el.has("message") && !el.get("message").isJsonNull -> el.get("message").asString.trim()
            else -> null
        }?.takeIf { it.isNotEmpty() }
    } catch (_: Exception) {
        null
    }
}

/**
 * Maps exceptions from Retrofit/OkHttp (and common I/O) to Spanish copy with a visible error *category*.
 */
fun Throwable.toUserFacingApiError(): UserFacingApiError {
    val t = unwrap()
    return when (t) {
        is HttpException -> mapHttpException(t)
        is SocketTimeoutException -> UserFacingApiError(
            ApiErrorKind.CONNECTION,
            "Tiempo de espera agotado",
            "La conexión tardó demasiado. Comprueba tu red e inténtalo de nuevo."
        )
        is UnknownHostException -> UserFacingApiError(
            ApiErrorKind.CONNECTION,
            "Sin conexión",
            "No se pudo contactar al servidor. Revisa tu internet o la URL del API."
        )
        is ConnectException -> UserFacingApiError(
            ApiErrorKind.CONNECTION,
            "Sin conexión",
            "No se pudo establecer la conexión. ¿El servidor está disponible?"
        )
        is SSLException -> UserFacingApiError(
            ApiErrorKind.CONNECTION,
            "Conexión segura",
            "Falló la negociación SSL. Comprueba fecha y hora del dispositivo o la red."
        )
        is IOException -> UserFacingApiError(
            ApiErrorKind.CONNECTION,
            "Sin conexión",
            "No hay conexión o se interrumpió. Inténtalo de nuevo."
        )
        else -> UserFacingApiError(
            ApiErrorKind.UNKNOWN,
            "Error",
            t.message?.takeIf { it.isNotBlank() }
                ?: "Algo salió mal. Inténtalo de nuevo."
        )
    }
}

private fun mapHttpException(e: HttpException): UserFacingApiError {
    val code = e.code()
    val serverMsg = parseHttpErrorBody(e)
    return when (code) {
        401, 403 -> UserFacingApiError(
            ApiErrorKind.AUTHORIZATION,
            "Autorización",
            serverMsg ?: "Tu sesión expiró o no tienes permiso. Vuelve a iniciar sesión."
        )
        in 500..599 -> UserFacingApiError(
            ApiErrorKind.SERVER,
            "Servidor",
            "El servidor tuvo un problema (${code}). Inténtalo más tarde."
        )
        404 -> UserFacingApiError(
            ApiErrorKind.CLIENT,
            "No encontrado",
            serverMsg ?: "No se encontró el recurso solicitado."
        )
        in 400..499 -> UserFacingApiError(
            ApiErrorKind.CLIENT,
            "Solicitud",
            serverMsg ?: "No se pudo completar la solicitud (${code})."
        )
        else -> UserFacingApiError(
            ApiErrorKind.UNKNOWN,
            "Error",
            serverMsg ?: "Respuesta inesperada del servidor (${code})."
        )
    }
}

fun Throwable.toUserFacingMessage(): String = toUserFacingApiError().formattedMessage()
