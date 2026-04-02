package com.ares.ewe.domain.model

data class FeaturedPlace(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val typeLabel: String,
    val isService: Boolean = false,
    val rate: Float = 0f
)

data class BestSellerProduct(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val price: Double,
    val rate: Float = 0f,
    val hasPromotion: Boolean = false,
    val discount: Int = 0
)

data class HomeData(
    val featuredPlaces: List<FeaturedPlace>,
    val bestSellerProducts: List<BestSellerProduct>
)

data class ShopProduct(
    val id: String,
    val name: String,
    val description: String?,
    val price: Double,
    val imageUrl: String?,
    val rate: Float = 0f,
    val hasPromotion: Boolean = false,
    val discount: Int = 0,
)

data class ProductDetail(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val imageUrls: List<String>,
    val rate: Float = 0f,
    val hasPromotion: Boolean = false,
    val discount: Int = 0,
)

data class ServiceDetail(
    val id: String,
    val name: String,
    val description: String?,
    val imageUrl: String?,
    val category: String?,
    val rate: Float = 0f
)

data class Ad(
    val id: String,
    val imageUrl: String?,
    val name: String,
    val description: String?,
    val address: String?,
    val contactPhone: String?,
    val whatsapp: String?,
    val email: String?,
    val facebookUrl: String?,
    val instagramUrl: String?
)
