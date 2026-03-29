package com.ares.ewe.domain.repository

import com.google.android.gms.maps.model.LatLng

interface DirectionsRepository {
    suspend fun getRoutePoints(origin: LatLng, destination: LatLng): Result<List<LatLng>>
}
