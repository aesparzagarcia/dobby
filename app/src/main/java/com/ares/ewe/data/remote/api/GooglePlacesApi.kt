package com.ares.ewe.data.remote.api

import com.ares.ewe.data.remote.model.GeocodeResponse
import com.ares.ewe.data.remote.model.PlaceDetailsResponse
import com.ares.ewe.data.remote.model.PlacesAutocompleteResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Google Places API (legacy) for address search and place details.
 * Requires a valid API key with Places API enabled in Google Cloud Console.
 */
interface GooglePlacesApi {

    @GET("place/autocomplete/json")
    suspend fun getAutocomplete(
        @Query("input") input: String,
        @Query("key") key: String,
        @Query("types") types: String = "address",
        @Query("language") language: String = "en"
    ): PlacesAutocompleteResponse

    @GET("place/details/json")
    suspend fun getPlaceDetails(
        @Query("place_id") placeId: String,
        @Query("key") key: String,
        @Query("fields") fields: String = "geometry"
    ): PlaceDetailsResponse

    @GET("geocode/json")
    suspend fun getReverseGeocode(
        @Query("latlng") latlng: String,
        @Query("key") key: String,
        @Query("language") language: String = "en"
    ): GeocodeResponse
}
