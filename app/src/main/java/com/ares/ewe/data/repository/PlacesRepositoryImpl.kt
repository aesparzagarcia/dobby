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
import com.ares.ewe.domain.model.ShopProductsPage
import com.ares.ewe.domain.repository.PlacesRepository
import com.ares.ewe.domain.repository.ShopDeliveryLookup
import com.ares.ewe.core.pricing.GeoDistance
import com.ares.ewe.core.util.serviceCategoryLabelEs
import com.ares.ewe.core.util.shopTypeLabelEs
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
                    "CAR_WASH" -> "Car wash"
                    else -> shop.type
                },
                latitude = shop.lat,
                longitude = shop.lng,
                shopType = shop.type,
                isService = false,
            )
        }
        val services = response.services.map { service ->
            PlaceItem(
                id = service.id,
                name = service.name,
                imageUrl = service.logoUrl?.toFullImageUrl(),
                typeLabel = "Service",
                latitude = null,
                longitude = null,
                shopType = null,
                isService = true,
            )
        }
        return shops + services
    }

    override suspend fun getShopCoordinatesByShopId(): Map<String, Pair<Double, Double>> =
        getShopDeliveryLookup().coordinatesByShopId

    override suspend fun getShopDeliveryLookup(): ShopDeliveryLookup {
        val places = getPlaces()
        val coordinates = linkedMapOf<String, Pair<Double, Double>>()
        val types = linkedMapOf<String, String>()
        for (place in places) {
            if (!place.isService) {
                place.shopType?.trim()?.takeIf { it.isNotEmpty() }?.let { types[place.id] = it }
            }
            val lat = place.latitude ?: continue
            val lng = place.longitude ?: continue
            if (!GeoDistance.isUsableWgs84Point(lat, lng)) continue
            coordinates[place.id] = Pair(lat, lng)
        }
        return ShopDeliveryLookup(
            coordinatesByShopId = coordinates,
            typeByShopId = types,
        )
    }

    private fun mapFeaturedPlace(p: com.ares.ewe.data.remote.model.FeaturedPlaceDto): FeaturedPlace {
        val isService = p.kind == "service"
        val shopType = if (isService) null else p.type
        val serviceCategory = if (isService) p.category else null
        val typeLabel = when {
            isService -> serviceCategoryLabelEs(p.category) ?: "Servicio"
            else -> shopTypeLabelEs(p.type)
        }
        return FeaturedPlace(
            id = p.id,
            name = p.name,
            imageUrl = p.logoUrl?.toFullImageUrl(),
            typeLabel = typeLabel,
            isService = isService,
            shopType = shopType,
            serviceCategory = serviceCategory,
            rate = p.rate,
            ratingCount = p.ratingCount,
            openingHour = p.openingHour,
            closingHour = p.closingHour,
            latitude = p.lat,
            longitude = p.lng,
        )
    }

    override suspend fun getHome(): HomeData {
        val response = api.getHome()
        val featuredPlaces = response.featuredPlaces.map(::mapFeaturedPlace)
        val bestSellerProducts = response.bestSellerProducts.map { p ->
            BestSellerProduct(
                id = p.id,
                name = p.name,
                description = p.description?.trim()?.takeIf { it.isNotEmpty() },
                imageUrl = p.imageUrl?.toFullImageUrl(),
                price = p.price,
                rate = p.rate,
                ratingCount = p.ratingCount,
                hasPromotion = p.hasPromotion,
                discount = p.discount,
                shopId = p.shopId,
            )
        }
        return HomeData(featuredPlaces = featuredPlaces, bestSellerProducts = bestSellerProducts)
    }

    override suspend fun getPromotions(): List<BestSellerProduct> {
        return api.getPromotions().map { p ->
            BestSellerProduct(
                id = p.id,
                name = p.name,
                description = p.description?.trim()?.takeIf { it.isNotEmpty() },
                imageUrl = p.imageUrl?.toFullImageUrl(),
                price = p.price,
                rate = p.rate,
                ratingCount = p.ratingCount,
                hasPromotion = p.hasPromotion,
                discount = p.discount,
                shopId = p.shopId,
            )
        }
    }

    override suspend fun getBestSellers(): List<ShopProduct> {
        return api.getBestSellers().map { p ->
            ShopProduct(
                id = p.id,
                name = p.name,
                description = p.description,
                price = p.price,
                imageUrl = p.imageUrl?.toFullImageUrl(),
                rate = p.rate,
                ratingCount = p.ratingCount,
                hasPromotion = p.hasPromotion,
                discount = p.discount,
                shopId = p.shopId,
                category = p.category,
            )
        }
    }

    override suspend fun getFeaturedPlaces(): List<FeaturedPlace> {
        return api.getFeaturedPlaces().map(::mapFeaturedPlace)
    }

    override suspend fun getShopProducts(shopId: String): ShopProductsPage {
        val response = api.getShopProducts(shopId)
        val products = response.products.map { p ->
            ShopProduct(
                id = p.id,
                name = p.name,
                description = p.description,
                price = p.price,
                imageUrl = p.imageUrl?.toFullImageUrl(),
                rate = p.rate,
                ratingCount = p.ratingCount,
                hasPromotion = p.hasPromotion,
                discount = p.discount,
                shopId = p.shopId ?: shopId,
                category = p.category,
            )
        }
        return ShopProductsPage(
            shopStatus = response.shop.status,
            openingHour = response.shop.openingHour,
            closingHour = response.shop.closingHour,
            products = products,
            shopName = response.shop.name,
            shopType = response.shop.type,
            logoUrl = response.shop.logoUrl?.toFullImageUrl(),
            rate = response.shop.rate,
            ratingCount = response.shop.ratingCount,
            jobsDone = response.shop.jobsDone,
        )
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
            ratingCount = dto.ratingCount,
            hasPromotion = dto.hasPromotion,
            discount = dto.discount,
            shopId = dto.shopId,
            shopType = dto.shopType,
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
            rate = s.rate,
            address = s.address,
            lat = s.lat,
            lng = s.lng,
        )
    }
}
