package com.ares.ewe.data.remote.model

import com.google.gson.annotations.SerializedName

data class AppRefreshRequest(
    @SerializedName("refreshToken") val refreshToken: String
)

data class AppRefreshResponse(
    @SerializedName("token") val token: String?,
    @SerializedName("refreshToken") val refreshToken: String?
)
