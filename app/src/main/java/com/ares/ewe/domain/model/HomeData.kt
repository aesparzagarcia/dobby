package com.ares.ewe.domain.model

data class FeaturedPlace(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val typeLabel: String,
    val isService: Boolean = false,
    val shopType: String? = null,
    val serviceCategory: String? = null,
    val rate: Float = 0f,
    val ratingCount: Int = 0,
    val openingHour: String? = null,
    val closingHour: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

data class BestSellerProduct(
    val id: String,
    val name: String,
    val description: String? = null,
    val imageUrl: String?,
    val price: Double,
    val rate: Float = 0f,
    val ratingCount: Int = 0,
    val hasPromotion: Boolean = false,
    val discount: Int = 0,
    val shopId: String? = null,
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
    val ratingCount: Int = 0,
    val hasPromotion: Boolean = false,
    val discount: Int = 0,
    val shopId: String? = null,
    val category: String? = null,
)

data class ShopProductsPage(
    val shopStatus: String,
    val openingHour: String?,
    val closingHour: String?,
    val products: List<ShopProduct>,
)

data class ProductDetail(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val imageUrls: List<String>,
    val rate: Float = 0f,
    val ratingCount: Int = 0,
    val hasPromotion: Boolean = false,
    val discount: Int = 0,
    val shopId: String? = null,
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
