package com.ares.ewe.domain.model

data class UserAddress(
    val id: String,
    val label: String,
    val description: String? = null,
    val address: String,
    val lat: Double,
    val lng: Double,
    val isDefault: Boolean,
    val isActive: Boolean = true
)
