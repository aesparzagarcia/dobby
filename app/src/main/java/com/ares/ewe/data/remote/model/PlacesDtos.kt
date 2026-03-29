package com.ares.ewe.data.remote.model

import com.google.gson.annotations.SerializedName

data class PlacesResponse(
    @SerializedName("shops") val shops: List<ShopDto>,
    @SerializedName("services") val services: List<ServiceDto>
)

data class ShopDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("logoUrl") val logoUrl: String? = null,
    @SerializedName("type") val type: String,
    @SerializedName("rate") val rate: Float = 0f
)

data class ServiceDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("logoUrl") val logoUrl: String? = null,
    @SerializedName("category") val category: String,
    @SerializedName("rate") val rate: Float = 0f
)
