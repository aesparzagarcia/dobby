package com.ares.ewe.domain.repository

import com.ares.ewe.domain.model.UserAddress

interface UserAddressRepository {
    suspend fun getAddresses(): Result<List<UserAddress>>
    suspend fun createAddress(
        label: String,
        description: String? = null,
        address: String,
        lat: Double,
        lng: Double,
        isDefault: Boolean = true
    ): Result<UserAddress>
    suspend fun setDefaultAddress(id: String): Result<Unit>
    suspend fun deleteAddress(id: String): Result<Unit>
}
