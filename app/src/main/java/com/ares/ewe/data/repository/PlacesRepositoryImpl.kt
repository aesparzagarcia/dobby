package com.ares.ewe.data.repository

import com.ares.ewe.BuildConfig
import com.ares.ewe.data.remote.api.DobbyApi
import com.ares.ewe.domain.model.BestSellerProduct
import com.ares.ewe.domain.model.FeaturedPlace
import com.ares.ewe.domain.model.HomeData
import com.ares.ewe.domain.model.PlaceItem
import com.ares.ewe.domain.model.ProductDetail
import com.ares.ewe.domain.model.ServiceDetail
import com.ares.ewe.domain.model.ShopProduct
import com.ares.ewe.domain.repository.PlacesRepository
import javax.inject.Inject

class PlacesRepositoryImpl @Inject constructor(
    private val api: DobbyApi
) : PlacesRepository {

    private val imageBaseUrl: String
        get() = BuildConfig.BASE_URL.removeSuffix("api/").trimEnd('/')

    private fun String.toFullImageUrl(): String =
        if (this.startsWith("http")) this else "$imageBaseUrl$this"

    override suspend fun getPlaces(): List<PlaceItem> {
        val response = api.getPlaces()
        val shops = response.shops.map { shop ->
            PlaceItem(
                id = shop.id,
                name = shop.name,
                imageUrl = shop.logoUrl?.toFullImageUrl(),
                typeLabel = when (shop.type) {
                    "RESTAURANT" -> "Restaurant"
                    "SHOP" -> "Shop"
                    "SERVICE_PROVIDER" -> "Service"
                    else -> shop.type
                }
            )
        }
        val services = response.services.map { service ->
            PlaceItem(
                id = service.id,
                name = service.name,
                imageUrl = service.logoUrl?.toFullImageUrl(),
                typeLabel = "Service"
            )
        }
        return shops + services
    }

    override suspend fun getHome(): HomeData {
        val response = api.getHome()
        val featuredPlaces = response.featuredPlaces.map { p ->
            val isService = p.kind == "service"
            FeaturedPlace(
                id = p.id,
                name = p.name,
                imageUrl = p.logoUrl?.toFullImageUrl(),
                typeLabel = when (p.kind) {
                    "shop" -> when (p.type) {
                        "RESTAURANT" -> "Restaurant"
                        "SHOP" -> "Shop"
                        "SERVICE_PROVIDER" -> "Service"
                        else -> p.type ?: "Shop"
                    }
                    "service" -> "Service"
                    else -> p.type ?: p.category ?: ""
                },
                isService = isService,
                rate = p.rate
            )
        }
        val bestSellerProducts = response.bestSellerProducts.map { p ->
            BestSellerProduct(
                id = p.id,
                name = p.name,
                imageUrl = p.imageUrl?.toFullImageUrl(),
                price = p.price,
                rate = p.rate,
                hasPromotion = p.hasPromotion,
                discount = p.discount
            )
        }
        return HomeData(featuredPlaces = featuredPlaces, bestSellerProducts = bestSellerProducts)
    }

    override suspend fun getPromotions(): List<BestSellerProduct> {
        return api.getPromotions().map { p ->
            BestSellerProduct(
                id = p.id,
                name = p.name,
                imageUrl = p.imageUrl?.toFullImageUrl(),
                price = p.price,
                rate = p.rate,
                hasPromotion = p.hasPromotion,
                discount = p.discount
            )
        }
    }

    override suspend fun getShopProducts(shopId: String): List<ShopProduct> {
        return api.getShopProducts(shopId).map { p ->
            ShopProduct(
                id = p.id,
                name = p.name,
                description = p.description,
                price = p.price,
                imageUrl = p.imageUrl?.toFullImageUrl(),
                rate = p.rate,
                hasPromotion = p.hasPromotion,
                discount = p.discount,
            )
        }
    }

    override suspend fun getProduct(productId: String): ProductDetail {
        val dto = api.getProduct(productId)
        val urls = (dto.imageUrls ?: emptyList()).map { it.toFullImageUrl() }
        return ProductDetail(
            id = dto.id,
            name = dto.name,
            description = dto.description ?: "",
            price = dto.price,
            imageUrls = urls,
            rate = dto.rate,
            hasPromotion = dto.hasPromotion,
            discount = dto.discount,
        )
    }

    override suspend fun getService(serviceId: String): ServiceDetail {
        val s = api.getService(serviceId)
        return ServiceDetail(
            id = s.id,
            name = s.name,
            description = s.description,
            imageUrl = s.logoUrl?.toFullImageUrl(),
            category = s.category,
            rate = s.rate
        )
    }
}
