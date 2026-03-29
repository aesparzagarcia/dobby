package com.ares.ewe.data.remote.model

import com.google.gson.annotations.SerializedName

data class GoogleDirectionsResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("error_message") val errorMessage: String? = null,
    @SerializedName("routes") val routes: List<DirectionsRoute>?
)

data class DirectionsRoute(
    @SerializedName("overview_polyline") val overviewPolyline: DirectionsPolyline?
)

data class DirectionsPolyline(
    @SerializedName("points") val points: String?
)
