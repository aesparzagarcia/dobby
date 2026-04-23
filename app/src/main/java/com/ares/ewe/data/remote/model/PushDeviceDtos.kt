package com.ares.ewe.data.remote.model

import com.google.gson.annotations.SerializedName

data class RegisterPushDeviceRequest(
    @SerializedName("fcm_token") val fcmToken: String,
    @SerializedName("platform") val platform: String,
)

data class RegisterPushDeviceResponse(
    @SerializedName("ok") val ok: Boolean? = null,
)
