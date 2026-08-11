package com.ares.ewe.domain.repository

import com.ares.ewe.domain.model.AddressPrediction
import com.google.android.gms.maps.model.LatLng

interface PlacesAutocompleteRepository {
    suspend fun getAddressPredictions(
        input: String,
        latitude: Double? = null,
        longitude: Double? = null,
    ): Result<List<AddressPrediction>>
    suspend fun getPlaceLocation(placeId: String): Result<LatLng>
    suspend fun getAddressFromLocation(lat: Double, lng: Double): Result<String>
}
