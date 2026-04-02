package com.ares.ewe.data.repository

import com.ares.ewe.data.local.dao.FavoriteProductDao
import com.ares.ewe.data.local.entity.FavoriteProductEntity
import com.ares.ewe.di.ApplicationScope
import com.ares.ewe.domain.model.FavoriteProduct
import com.ares.ewe.domain.repository.FavoritesRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Singleton
class FavoritesRepositoryImpl @Inject constructor(
    private val favoriteProductDao: FavoriteProductDao,
    @ApplicationScope private val scope: CoroutineScope,
) : FavoritesRepository {

    override val favorites: Flow<List<FavoriteProduct>> = favoriteProductDao.getAll().map { list ->
        list.map { it.toDomain() }
    }

    override fun isFavorite(productId: String): Flow<Boolean> = favoriteProductDao.isFavorite(productId)

    override fun toggleFavorite(product: FavoriteProduct) {
        scope.launch {
            val existing = favoriteProductDao.getById(product.productId)
            if (existing == null) {
                favoriteProductDao.upsert(
                    FavoriteProductEntity(
                        productId = product.productId,
                        name = product.name,
                        price = product.price,
                        imageUrl = product.imageUrl,
                        rate = product.rate,
                        hasPromotion = product.hasPromotion,
                        discount = product.discount,
                    )
                )
            } else {
                favoriteProductDao.deleteById(product.productId)
            }
        }
    }
}

private fun FavoriteProductEntity.toDomain() = FavoriteProduct(
    productId = productId,
    name = name,
    price = price,
    imageUrl = imageUrl,
    rate = rate,
    hasPromotion = hasPromotion,
    discount = discount,
)
