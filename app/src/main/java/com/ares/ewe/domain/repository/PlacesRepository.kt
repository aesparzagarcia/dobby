package com.ares.ewe.domain.repository

import com.ares.ewe.domain.model.BestSellerProduct
import com.ares.ewe.domain.model.HomeData
import com.ares.ewe.domain.model.PlaceItem
import com.ares.ewe.domain.model.ProductDetail
import com.ares.ewe.domain.model.ServiceDetail
import com.ares.ewe.domain.model.ShopProduct

interface PlacesRepository {
    suspend fun getPlaces(): List<PlaceItem>

    suspend fun getHome(): HomeData

    suspend fun getPromotions(): List<BestSellerProduct>

    suspend fun getShopProducts(shopId: String): List<ShopProduct>

    suspend fun getProduct(productId: String): ProductDetail

    suspend fun getService(serviceId: String): ServiceDetail
}
