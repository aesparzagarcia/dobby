package com.ares.ewe.data.location

import com.google.android.gms.maps.model.LatLng

interface LocationProvider {
    suspend fun getLastLocation(): Result<LatLng>
}
