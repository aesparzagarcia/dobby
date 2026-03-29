package com.ares.ewe.data.remote.model

import com.google.gson.annotations.SerializedName

data class PlacesAutocompleteResponse(
    @SerializedName("predictions") val predictions: List<PlacePrediction>? = null,
    @SerializedName("status") val status: String? = null
)

data class PlacePrediction(
    @SerializedName("description") val description: String,
    @SerializedName("place_id") val placeId: String,
    @SerializedName("structured_formatting") val structuredFormatting: StructuredFormatting? = null
)

data class StructuredFormatting(
    @SerializedName("main_text") val mainText: String? = null,
    @SerializedName("secondary_text") val secondaryText: String? = null
)

data class PlaceDetailsResponse(
    @SerializedName("result") val result: PlaceDetailsResult? = null,
    @SerializedName("status") val status: String? = null
)

data class PlaceDetailsResult(
    @SerializedName("geometry") val geometry: PlaceGeometry? = null
)

data class PlaceGeometry(
    @SerializedName("location") val location: PlaceLocation? = null
)

data class PlaceLocation(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double
)

data class GeocodeResponse(
    @SerializedName("results") val results: List<GeocodeResult>? = null,
    @SerializedName("status") val status: String? = null
)

data class GeocodeResult(
    @SerializedName("formatted_address") val formattedAddress: String? = null
)
