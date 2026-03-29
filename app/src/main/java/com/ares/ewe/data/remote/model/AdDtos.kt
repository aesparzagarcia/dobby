package com.ares.ewe.data.remote.model

import com.google.gson.annotations.SerializedName

data class AdDto(
    @SerializedName("id") val id: String,
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName("advertiserName") val advertiserName: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("contactPhone") val contactPhone: String? = null,
    @SerializedName("whatsapp") val whatsapp: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("facebookUrl") val facebookUrl: String? = null,
    @SerializedName("instagramUrl") val instagramUrl: String? = null
)
