package com.ares.ewe.data.repository

import com.ares.ewe.BuildConfig
import com.ares.ewe.core.location.AddressSearchRegion
import com.ares.ewe.data.remote.api.GooglePlacesApi
import com.ares.ewe.domain.model.AddressPrediction
import com.ares.ewe.domain.repository.PlacesAutocompleteRepository
import com.google.android.gms.maps.model.LatLng
import javax.inject.Inject

class PlacesAutocompleteRepositoryImpl @Inject constructor(
    private val api: GooglePlacesApi
) : PlacesAutocompleteRepository {

    override suspend fun getPlaceLocation(placeId: String): Result<LatLng> {
        if (BuildConfig.PLACES_API_KEY.isBlank()) {
            return Result.failure(IllegalStateException("PLACES_API_KEY is not set."))
        }
        return try {
            val response = api.getPlaceDetails(
                placeId = placeId,
                key = BuildConfig.PLACES_API_KEY,
                fields = "geometry"
            )
            if (response.status != "OK") {
                return Result.failure(Exception("Place details: ${response.status}"))
            }
            val location = response.result?.geometry?.location
                ?: return Result.failure(Exception("No geometry for place"))
            Result.success(LatLng(location.lat, location.lng))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAddressFromLocation(lat: Double, lng: Double): Result<String> {
        if (BuildConfig.PLACES_API_KEY.isBlank()) {
            return Result.failure(IllegalStateException("PLACES_API_KEY is not set."))
        }
        return try {
            val response = api.getReverseGeocode(
                latlng = "$lat,$lng",
                key = BuildConfig.PLACES_API_KEY,
                language = "es"
            )
            if (response.status != "OK") {
                val detail = when (response.status) {
                    "ZERO_RESULTS" -> "Sin resultados para esas coordenadas."
                    "REQUEST_DENIED" -> "Geocoding no autorizado: revisa la clave y restricciones en Google Cloud."
                    else -> "Geocoding: ${response.status}"
                }
                return Result.failure(Exception(detail))
            }
            val address = response.results?.firstOrNull()?.formattedAddress
                ?: return Result.failure(Exception("No address found"))
            Result.success(address)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAddressPredictions(
        input: String,
        latitude: Double?,
        longitude: Double?,
    ): Result<List<AddressPrediction>> {
        if (BuildConfig.PLACES_API_KEY.isBlank()) {
            return Result.failure(IllegalStateException("PLACES_API_KEY is not set. Add it to gradle.properties or build.gradle."))
        }
        return try {
            // Por ahora siempre Tala (ignora GPS). Cuando se amplíe, usar latitude/longitude.
            val response = api.getAutocomplete(
                input = AddressSearchRegion.localizedQuery(input),
                key = BuildConfig.PLACES_API_KEY,
                types = "geocode",
                language = "es",
                components = "country:mx",
                location = "${AddressSearchRegion.CENTER_LAT},${AddressSearchRegion.CENTER_LNG}",
                radius = AddressSearchRegion.RADIUS_METERS,
                strictbounds = true,
            )
            if (response.status != "OK" && response.status != "ZERO_RESULTS") {
                return Result.failure(Exception("Places API: ${response.status}"))
            }
            val list = (response.predictions ?: emptyList())
                .map { p ->
                    AddressPrediction(
                        placeId = p.placeId,
                        mainText = p.structuredFormatting?.mainText ?: p.description,
                        secondaryText = p.structuredFormatting?.secondaryText
                    )
                }
                .filter { prediction ->
                    val haystack = listOfNotNull(prediction.mainText, prediction.secondaryText)
                        .joinToString(" ")
                        .lowercase()
                    haystack.contains("tala")
                }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
