package com.ares.ewe.data.repository

import com.ares.ewe.data.remote.api.DobbyApi
import com.ares.ewe.data.remote.model.CreateAddressRequest
import com.ares.ewe.domain.model.UserAddress
import com.ares.ewe.domain.repository.UserAddressRepository
import javax.inject.Inject

class UserAddressRepositoryImpl @Inject constructor(
    private val api: DobbyApi
) : UserAddressRepository {

    override suspend fun getAddresses(): Result<List<UserAddress>> = runCatching {
        api.getAddresses().map { dto ->
            UserAddress(
                id = dto.id,
                label = dto.label,
                description = dto.description,
                address = dto.address,
                lat = dto.lat,
                lng = dto.lng,
                isDefault = dto.isDefault,
                isActive = dto.isActive
            )
        }
    }

    override suspend fun createAddress(
        label: String,
        description: String?,
        address: String,
        lat: Double,
        lng: Double,
        isDefault: Boolean
    ): Result<UserAddress> = runCatching {
        val dto = api.createAddress(
            CreateAddressRequest(
                label = label,
                description = description,
                address = address,
                lat = lat,
                lng = lng,
                isDefault = isDefault
            )
        )
        UserAddress(
            id = dto.id,
            label = dto.label,
            description = dto.description,
            address = dto.address,
            lat = dto.lat,
            lng = dto.lng,
            isDefault = dto.isDefault,
            isActive = dto.isActive
        )
    }

    override suspend fun setDefaultAddress(id: String): Result<Unit> = runCatching {
        api.setDefaultAddress(id)
    }

    override suspend fun deleteAddress(id: String): Result<Unit> = runCatching {
        api.deleteAddress(id)
    }
}
