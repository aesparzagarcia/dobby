package com.ares.ewe.core.network

import com.google.gson.JsonParser
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import retrofit2.HttpException

/**
 * Catálogo de fallos que ve el usuario. Cada tipo tiene su propio texto;
 * no se muestra el mensaje técnico de OkHttp o del sistema.
 */
enum class ApiErrorKind {
    /** El dispositivo no tiene red o no resuelve el host. */
    NO_INTERNET,

    /** El host existe, pero no acepta la conexión (servidor caído o puerto cerrado). */
    SERVER_UNAVAILABLE,

    /** La petición salió, pero no hubo respuesta a tiempo. */
    TIMEOUT,

    /** 401 / 403: sesión ausente o vencida. */
    AUTHORIZATION,

    /** 400: datos de la petición rechazados. */
    BAD_REQUEST,

    /** Otros 4xx (no encontrado, conflicto, etc.). */
    CLIENT,

    /** 5xx. */
    SERVER,

    /** Certificado o negociación TLS. */
    SSL,

    /** Cualquier otro fallo. */
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

fun Throwable.unwrap(): Throwable = causeChain().last()

private fun Throwable.causeChain(): List<Throwable> {
    val chain = ArrayList<Throwable>(4)
    var current: Throwable? = this
    while (current != null && current !in chain) {
        chain.add(current)
        current = current.cause
    }
    return chain
}

private fun Throwable.signalsServerDown(): Boolean {
    if (this is ConnectException) return true
    val message = message?.lowercase().orEmpty()
    return message.contains("econnrefused") ||
        message.contains("connection refused") ||
        message.contains("isconnected failed") ||
        message.contains("failed to connect")
}

private fun Throwable.signalsNoInternet(): Boolean {
    if (this is UnknownHostException || this is NoRouteToHostException) return true
    val message = message?.lowercase().orEmpty()
    return message.contains("unable to resolve host") ||
        message.contains("no address associated") ||
        message.contains("network is unreachable") ||
        message.contains("enetunreach") ||
        message.contains("ehostunreach")
}

private fun noInternetError() = UserFacingApiError(
    ApiErrorKind.NO_INTERNET,
    "Sin internet",
    "No hay conexión a internet. Verifica tu red e intenta de nuevo.",
)

private fun serverUnavailableError() = UserFacingApiError(
    ApiErrorKind.SERVER_UNAVAILABLE,
    "Servidor no disponible",
    "No se pudo conectar al servidor. Puede estar fuera de servicio. Intenta de nuevo en unos minutos.",
)

private fun timeoutError() = UserFacingApiError(
    ApiErrorKind.TIMEOUT,
    "Tiempo de espera",
    "El servidor tardó demasiado en responder. Intenta de nuevo.",
)

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
 * Walks the whole cause chain: OkHttp often wraps ECONNREFUSED inside [ConnectException]
 * and then inside an [android.system.ErrnoException] whose message is English.
 */
fun Throwable.toUserFacingApiError(): UserFacingApiError {
    val chain = causeChain()
    chain.firstOrNull { it is HttpException }?.let { return mapHttpException(it as HttpException) }
    if (chain.any { it is SocketTimeoutException }) return timeoutError()
    if (chain.any { it.signalsServerDown() }) return serverUnavailableError()
    if (chain.any { it.signalsNoInternet() }) return noInternetError()
    if (chain.any { it is SSLException }) {
        return UserFacingApiError(
            ApiErrorKind.SSL,
            "Conexión segura",
            "No se pudo establecer una conexión segura. Revisa la fecha y hora del dispositivo e intenta de nuevo.",
        )
    }
    if (chain.any { it is IOException }) return serverUnavailableError()
    return UserFacingApiError(
        ApiErrorKind.UNKNOWN,
        "No se pudo cargar",
        "Algo salió mal. Inténtalo de nuevo.",
    )
}

private fun mapHttpException(e: HttpException): UserFacingApiError {
    val code = e.code()
    val serverMsg = parseHttpErrorBody(e)
    return when (code) {
        401, 403 -> UserFacingApiError(
            ApiErrorKind.AUTHORIZATION,
            "Sesión",
            serverMsg
                ?.takeUnless { it.equals("Unauthorized", ignoreCase = true) || it.equals("Forbidden", ignoreCase = true) }
                ?: "Inicia sesión para continuar."
        )
        in 500..599 -> UserFacingApiError(
            ApiErrorKind.SERVER,
            "Error del servidor",
            "El servidor tuvo un problema. Inténtalo más tarde."
        )
        400 -> UserFacingApiError(
            ApiErrorKind.BAD_REQUEST,
            "Solicitud incorrecta",
            serverMsg ?: "Los datos enviados no son válidos. Revísalos e intenta de nuevo."
        )
        404 -> UserFacingApiError(
            ApiErrorKind.CLIENT,
            "No encontrado",
            serverMsg ?: "No se encontró lo que buscabas."
        )
        in 400..499 -> UserFacingApiError(
            ApiErrorKind.CLIENT,
            "Solicitud",
            serverMsg ?: "No se pudo completar la solicitud. Intenta de nuevo."
        )
        else -> UserFacingApiError(
            ApiErrorKind.UNKNOWN,
            "Error",
            serverMsg ?: "Respuesta inesperada del servidor (${code})."
        )
    }
}

fun Throwable.toUserFacingMessage(): String = toUserFacingApiError().formattedMessage()

/** True for HTTP 401/403 (guest or expired session). */
fun Throwable.isAuthorizationError(): Boolean =
    toUserFacingApiError().kind == ApiErrorKind.AUTHORIZATION
