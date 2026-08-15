package com.ares.ewe.domain.repository

import com.ares.ewe.domain.model.BestSellerProduct
import com.ares.ewe.domain.model.FeaturedPlace
import com.ares.ewe.domain.model.HomeData
import com.ares.ewe.domain.model.PlaceItem
import com.ares.ewe.domain.model.ProductDetail
import com.ares.ewe.domain.model.ServiceDetail
import com.ares.ewe.domain.model.ShopProduct
import com.ares.ewe.domain.model.ShopProductsPage

interface PlacesRepository {
    suspend fun getPlaces(): List<PlaceItem>

    suspend fun getHome(): HomeData

    suspend fun getPromotions(): List<BestSellerProduct>

    suspend fun getBestSellers(): List<ShopProduct>

    suspend fun getFeaturedPlaces(): List<FeaturedPlace>

    suspend fun getShopProducts(shopId: String): ShopProductsPage

    suspend fun getProduct(productId: String): ProductDetail

    suspend fun getService(serviceId: String): ServiceDetail

    /** Tiendas con `lat`/`lng` desde `GET app/places` (para ETA en carrito por `shop_id`). */
    suspend fun getShopCoordinatesByShopId(): Map<String, Pair<Double, Double>>

    /**
     * Coords + tipos de tienda desde `GET app/places` (carrito: ETA y envío gratis CAR_WASH).
     */
    suspend fun getShopDeliveryLookup(): ShopDeliveryLookup
}

/** Resultado de [PlacesRepository.getShopDeliveryLookup]. */
data class ShopDeliveryLookup(
    val coordinatesByShopId: Map<String, Pair<Double, Double>>,
    val typeByShopId: Map<String, String>,
)
