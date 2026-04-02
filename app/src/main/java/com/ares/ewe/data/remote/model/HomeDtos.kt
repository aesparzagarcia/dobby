package com.ares.ewe.data.remote.model

import com.google.gson.annotations.SerializedName

data class HomeResponse(
    @SerializedName("featuredPlaces") val featuredPlaces: List<FeaturedPlaceDto>,
    @SerializedName("bestSellerProducts") val bestSellerProducts: List<BestSellerProductDto>
)

data class FeaturedPlaceDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("logoUrl") val logoUrl: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("kind") val kind: String,
    @SerializedName("rate") val rate: Float = 0f
)

data class BestSellerProductDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName("price") val price: Double,
    @SerializedName("rate") val rate: Float = 0f,
    @SerializedName("has_promotion") val hasPromotion: Boolean = false,
    @SerializedName("discount") val discount: Int = 0
)

data class PromotionProductDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName("price") val price: Double,
    @SerializedName("rate") val rate: Float = 0f,
    @SerializedName("has_promotion") val hasPromotion: Boolean = false,
    @SerializedName("discount") val discount: Int = 0
)

data class ShopProductDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("price") val price: Double,
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName("rate") val rate: Float = 0f,
    @SerializedName("has_promotion") val hasPromotion: Boolean = false,
    @SerializedName("discount") val discount: Int = 0,
)

data class ProductDetailDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("price") val price: Double,
    @SerializedName("imageUrls") val imageUrls: List<String>? = null,
    @SerializedName("rate") val rate: Float = 0f,
    @SerializedName("has_promotion") val hasPromotion: Boolean = false,
    @SerializedName("discount") val discount: Int = 0,
)

data class ServiceDetailDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("logoUrl") val logoUrl: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("rate") val rate: Float = 0f
)
