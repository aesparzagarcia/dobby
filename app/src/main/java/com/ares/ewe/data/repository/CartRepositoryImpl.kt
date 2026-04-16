package com.ares.ewe.data.repository

import com.ares.ewe.data.local.dao.CartDao
import com.ares.ewe.di.ApplicationScope
import com.ares.ewe.data.local.entity.CartInfo
import com.ares.ewe.domain.model.CartItem
import com.ares.ewe.domain.repository.CartRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CartRepositoryImpl @Inject constructor(
    private val cartDao: CartDao,
    @ApplicationScope private val scope: CoroutineScope
) : CartRepository {

    override val items: Flow<List<CartItem>> = cartDao.getAll().map { list ->
        list.map { it.toCartItem() }
    }

    override fun addItem(
        productId: String,
        name: String,
        price: Double,
        quantity: Int,
        imageUrl: String?,
        listPrice: Double,
        hasPromotion: Boolean,
        discount: Int
    ) {
        if (quantity <= 0) return
        scope.launch {
            val existing = cartDao.getByProductId(productId)
            if (existing != null) {
                cartDao.insert(
                    existing.copy(
                        name = name,
                        price = price,
                        quantity = existing.quantity + quantity,
                        imageUrl = imageUrl ?: existing.imageUrl,
                        listPrice = listPrice,
                        hasPromotion = hasPromotion,
                        discount = discount
                    )
                )
            } else {
                cartDao.insert(
                    CartInfo(
                        productId = productId,
                        name = name,
                        price = price,
                        quantity = quantity,
                        imageUrl = imageUrl,
                        listPrice = listPrice,
                        hasPromotion = hasPromotion,
                        discount = discount
                    )
                )
            }
        }
    }

    override fun removeItem(productId: String) {
        scope.launch {
            cartDao.deleteByProductId(productId)
        }
    }

    override fun updateQuantity(productId: String, quantity: Int) {
        if (quantity <= 0) {
            removeItem(productId)
            return
        }
        scope.launch {
            cartDao.updateQuantity(productId, quantity)
        }
    }

    override fun clear() {
        scope.launch {
            cartDao.deleteAll()
        }
    }
}

private fun CartInfo.toCartItem() = CartItem(
    productId = productId,
    name = name,
    price = price,
    quantity = quantity,
    imageUrl = imageUrl,
    listPrice = listPrice,
    hasPromotion = hasPromotion,
    discount = discount
)
