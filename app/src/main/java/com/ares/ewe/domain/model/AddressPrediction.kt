package com.ares.ewe.domain.model

data class AddressPrediction(
    val placeId: String,
    val mainText: String,
    val secondaryText: String?
)
