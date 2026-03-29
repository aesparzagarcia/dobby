package com.ares.ewe.data.remote.model

import com.google.gson.annotations.SerializedName

// OTP flow
data class RequestOtpRequest(
    @SerializedName("phone") val phone: String
)

data class RequestOtpResponse(
    @SerializedName("user_exists") val userExists: Boolean,
    @SerializedName("message") val message: String? = null
)

data class VerifyOtpRequest(
    @SerializedName("phone") val phone: String,
    @SerializedName("code") val code: String
)

data class VerifyOtpResponse(
    @SerializedName("token") val token: String? = null,
    @SerializedName("refreshToken") val refreshToken: String? = null,
    @SerializedName("user") val user: UserDto? = null,
    @SerializedName("requires_registration") val requiresRegistration: Boolean = false
)

data class CompleteRegistrationRequest(
    @SerializedName("phone") val phone: String,
    @SerializedName("name") val name: String,
    @SerializedName("last_name") val lastName: String,
    @SerializedName("email") val email: String
)

data class CompleteRegistrationResponse(
    @SerializedName("token") val token: String,
    @SerializedName("refreshToken") val refreshToken: String?,
    @SerializedName("user") val user: UserDto? = null
)

data class UserDto(
    @SerializedName("id") val id: String,
    @SerializedName("email") val email: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("last_name") val lastName: String? = null
)

// User addresses (many per user)
data class CreateAddressRequest(
    @SerializedName("label") val label: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("address") val address: String,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double,
    @SerializedName("is_default") val isDefault: Boolean = true
)

data class AddressDto(
    @SerializedName("id") val id: String,
    @SerializedName("label") val label: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("address") val address: String,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double,
    @SerializedName("is_default") val isDefault: Boolean = false,
    @SerializedName("is_active") val isActive: Boolean = true,
    @SerializedName("created_at") val createdAt: String? = null
)
