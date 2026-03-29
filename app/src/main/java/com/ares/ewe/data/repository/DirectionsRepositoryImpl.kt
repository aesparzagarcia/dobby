package com.ares.ewe.data.repository

import android.util.Log
import com.ares.ewe.BuildConfig
import com.ares.ewe.data.remote.api.GoogleDirectionsApi
import com.ares.ewe.data.remote.model.GoogleDirectionsResponse
import com.ares.ewe.data.util.PolylineDecoder
import com.ares.ewe.domain.repository.DirectionsRepository
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import javax.inject.Inject

private const val TAG = "DirectionsRepo"

class DirectionsRepositoryImpl @Inject constructor(
    private val api: GoogleDirectionsApi
) : DirectionsRepository {

    override suspend fun getRoutePoints(origin: LatLng, destination: LatLng): Result<List<LatLng>> {
        return try {
            val originStr = "${origin.latitude},${origin.longitude}"
            val destStr = "${destination.latitude},${destination.longitude}"
            val key = BuildConfig.DIRECTIONS_API_KEY
            if (key.isBlank()) {
                Log.e(TAG, "DIRECTIONS_API_KEY is empty")
                return Result.failure(
                    IllegalStateException("DIRECTIONS_API_KEY is not set. Add DIRECTIONS_API_KEY (or MAPS/PLACES) in local.properties.")
                )
            }
            val response = api.getDirections(
                origin = originStr,
                destination = destStr,
                key = key
            )
            val body = response.body()
            if (!response.isSuccessful || body == null) {
                val errorBody = response.errorBody()?.string()
                val parsed = errorBody?.let {
                    Gson().fromJson(it, GoogleDirectionsResponse::class.java)
                }
                val status = parsed?.status ?: "HTTP ${response.code()}"
                val msg = parsed?.errorMessage ?: errorBody ?: status
                Log.e(TAG, "Directions API failed: status=$status, message=$msg")
                return Result.failure(IllegalStateException("Directions API: $status. $msg"))
            }
            if (body.status != "OK") {
                val msg = body.errorMessage ?: "Directions API: ${body.status}"
                Log.e(TAG, "Directions API status not OK: ${body.status}, $msg")
                if (body.status == "REQUEST_DENIED") {
                    Log.e(
                        TAG,
                        "REQUEST_DENIED: enable Directions API + billing, and use DIRECTIONS_API_KEY without Android app restriction (web service), or call Directions from your backend."
                    )
                }
                return Result.failure(IllegalStateException(msg))
            }
            val encoded = body.routes
                ?.firstOrNull()
                ?.overviewPolyline
                ?.points
            val points = if (!encoded.isNullOrBlank()) PolylineDecoder.decode(encoded) else emptyList()
            Log.d(TAG, "Route decoded: ${points.size} points")
            Result.success(points)
        } catch (e: Exception) {
            Log.e(TAG, "Directions request failed", e)
            Result.failure(e)
        }
    }
}
