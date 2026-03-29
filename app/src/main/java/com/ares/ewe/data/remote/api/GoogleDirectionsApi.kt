package com.ares.ewe.data.remote.api

import com.ares.ewe.data.remote.model.GoogleDirectionsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleDirectionsApi {

    @GET("directions/json")
    suspend fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("key") key: String,
        @Query("mode") mode: String = "driving"
    ): Response<GoogleDirectionsResponse>
}
