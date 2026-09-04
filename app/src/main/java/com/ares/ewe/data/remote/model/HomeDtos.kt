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
    @SerializedName("rate") val rate: Float = 0f,
    @SerializedName("rating_count") val ratingCount: Int = 0,
    @SerializedName("opening_hour") val openingHour: String? = null,
    @SerializedName("closing_hour") val closingHour: String? = null,
    @SerializedName("opening_days") val openingDays: List<String> = emptyList(),
    @SerializedName("lat") val lat: Double? = null,
    @SerializedName("lng") val lng: Double? = null,
)

data class BestSellerProductDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName("price") val price: Double,
    @SerializedName("rate") val rate: Float = 0f,
    @SerializedName("rating_count") val ratingCount: Int = 0,
    @SerializedName("has_promotion") val hasPromotion: Boolean = false,
    @SerializedName("discount") val discount: Int = 0,
    @SerializedName("shop_id") val shopId: String? = null,
)

data class PromotionProductDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName("price") val price: Double,
    @SerializedName("rate") val rate: Float = 0f,
    @SerializedName("rating_count") val ratingCount: Int = 0,
    @SerializedName("has_promotion") val hasPromotion: Boolean = false,
    @SerializedName("discount") val discount: Int = 0,
    @SerializedName("shop_id") val shopId: String? = null,
)

data class ShopProductDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("price") val price: Double,
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName("rate") val rate: Float = 0f,
    @SerializedName("rating_count") val ratingCount: Int = 0,
    @SerializedName("has_promotion") val hasPromotion: Boolean = false,
    @SerializedName("discount") val discount: Int = 0,
    @SerializedName("shop_id") val shopId: String? = null,
    @SerializedName("category") val category: String? = null,
)

data class ShopInfoDto(
    @SerializedName("status") val status: String,
    @SerializedName("opening_hour") val openingHour: String? = null,
    @SerializedName("closing_hour") val closingHour: String? = null,
    @SerializedName("opening_days") val openingDays: List<String> = emptyList(),
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("logo_url") val logoUrl: String? = null,
    @SerializedName("rate") val rate: Float = 0f,
    @SerializedName("rating_count") val ratingCount: Int = 0,
    @SerializedName("jobs_done") val jobsDone: Int = 0,
)

data class ShopProductsResponseDto(
    @SerializedName("shop") val shop: ShopInfoDto,
    @SerializedName("products") val products: List<ShopProductDto>,
)

data class ProductDetailDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("price") val price: Double,
    @SerializedName("imageUrls") val imageUrls: List<String>? = null,
    @SerializedName("rate") val rate: Float = 0f,
    @SerializedName("rating_count") val ratingCount: Int = 0,
    @SerializedName("has_promotion") val hasPromotion: Boolean = false,
    @SerializedName("discount") val discount: Int = 0,
    @SerializedName("shop_id") val shopId: String? = null,
    @SerializedName("shop_type") val shopType: String? = null,
)

data class ServiceDetailDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("logoUrl") val logoUrl: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("rate") val rate: Float = 0f,
    @SerializedName("address") val address: String? = null,
    @SerializedName("lat") val lat: Double? = null,
    @SerializedName("lng") val lng: Double? = null,
)
