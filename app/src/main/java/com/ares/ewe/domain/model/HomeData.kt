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
    val openingDays: List<String> = emptyList(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val shopStatus: String? = null,
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
    val openingDays: List<String> = emptyList(),
    val products: List<ShopProduct>,
    val shopName: String? = null,
    val shopType: String? = null,
    val logoUrl: String? = null,
    val rate: Float = 0f,
    val ratingCount: Int = 0,
    val jobsDone: Int = 0,
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
    val shopType: String? = null,
)

data class ServiceDetail(
    val id: String,
    val name: String,
    val description: String?,
    val imageUrl: String?,
    val category: String?,
    val rate: Float = 0f,
    val address: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
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
    val instagramUrl: String?,
    /** 0 = normal … 3 = premium (más apariciones en carrusel). */
    val priority: Int = 0,
)

data class AdCarouselSlide(
    val key: String,
    val ad: Ad,
)

/** Un anuncio → un slide fijo. Varios → más slides según prioridad (3 premium = 4 apariciones). */
fun buildWeightedAdCarouselSlides(ads: List<Ad>): List<AdCarouselSlide> {
    if (ads.size <= 1) {
        return ads.map { AdCarouselSlide(key = it.id, ad = it) }
    }
    return ads.sortedWith(compareByDescending<Ad> { it.priority }.thenByDescending { it.id })
        .flatMap { ad ->
            val weight = ad.priority.coerceIn(0, 3) + 1
            (0 until weight).map { index ->
                AdCarouselSlide(key = "${ad.id}-$index", ad = ad)
            }
        }
}
