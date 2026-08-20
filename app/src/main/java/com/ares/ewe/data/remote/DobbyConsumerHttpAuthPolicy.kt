package com.ares.ewe.data.remote

import com.ares.ewe.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request

internal object DobbyConsumerHttpAuthPolicy {
    const val HEADER_AUTH_RETRY = "X-Dobby-Auth-Retry"

    fun isDobbyBackendRequest(request: Request): Boolean {
        val base = BuildConfig.BASE_URL.toHttpUrlOrNull() ?: return false
        val url = request.url
        if (!url.host.equals(base.host, ignoreCase = true)) return false
        return url.port == base.port
    }

    fun shouldSkipRefresh(request: Request): Boolean {
        val u = request.url.toString()
        return u.contains("auth/request-otp") ||
            u.contains("auth/verify-otp") ||
            u.contains("auth/complete-registration") ||
            u.contains("/auth/refresh") ||
            u.contains("auth/session/logout")
    }
}
