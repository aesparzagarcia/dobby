package com.ares.ewe.data.repository

import com.ares.ewe.data.local.dao.CartDao
import com.ares.ewe.di.ApplicationScope
import com.ares.ewe.data.local.entity.CartInfo
import com.ares.ewe.domain.cart.CartLineKinds
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
        discount: Int,
        pickupLatitude: Double?,
        pickupLongitude: Double?,
        shopId: String?,
    ) {
        if (quantity <= 0) return
        scope.launch {
            // Productos y pagos de servicio no se mezclan en el mismo carrito.
            cartDao.deleteByLineKind(CartLineKinds.SERVICE)
            val existing = cartDao.getByProductId(productId)
            if (existing != null && !CartLineKinds.isService(existing.toCartItem())) {
                val mergedLat = pickupLatitude ?: existing.pickupLatitude
                val mergedLng = pickupLongitude ?: existing.pickupLongitude
                val mergedShopId = shopId ?: existing.shopId
                cartDao.insert(
                    existing.copy(
                        name = name,
                        price = price,
                        quantity = existing.quantity + quantity,
                        imageUrl = imageUrl ?: existing.imageUrl,
                        listPrice = listPrice,
                        hasPromotion = hasPromotion,
                        discount = discount,
                        pickupLatitude = mergedLat,
                        pickupLongitude = mergedLng,
                        shopId = mergedShopId,
                        lineKind = CartLineKinds.PRODUCT,
                        serviceId = null,
                        serviceNumber = null,
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
                        discount = discount,
                        pickupLatitude = pickupLatitude,
                        pickupLongitude = pickupLongitude,
                        shopId = shopId,
                        lineKind = CartLineKinds.PRODUCT,
                    )
                )
            }
        }
    }

    override fun addServiceItem(
        serviceId: String,
        serviceName: String,
        serviceNumber: String,
        amount: Double,
        imageUrl: String?,
        pickupLatitude: Double?,
        pickupLongitude: Double?,
    ) {
        val number = serviceNumber.trim()
        if (serviceId.isBlank() || number.isEmpty() || amount <= 0) return
        val lineId = CartLineKinds.serviceLineId(serviceId, number)
        scope.launch {
            cartDao.deleteByLineKind(CartLineKinds.PRODUCT)
            cartDao.insert(
                CartInfo(
                    productId = lineId,
                    name = serviceName,
                    price = amount,
                    quantity = 1,
                    imageUrl = imageUrl,
                    description = "Nº $number",
                    listPrice = 0.0,
                    hasPromotion = false,
                    discount = 0,
                    pickupLatitude = pickupLatitude,
                    pickupLongitude = pickupLongitude,
                    shopId = null,
                    lineKind = CartLineKinds.SERVICE,
                    serviceId = serviceId,
                    serviceNumber = number,
                )
            )
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
            val existing = cartDao.getByProductId(productId)
            if (existing != null && existing.lineKind.equals(CartLineKinds.SERVICE, ignoreCase = true)) {
                // Los pagos de servicio son una línea por número; no acumulan cantidad.
                return@launch
            }
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
    description = description,
    listPrice = listPrice,
    hasPromotion = hasPromotion,
    discount = discount,
    pickupLatitude = pickupLatitude,
    pickupLongitude = pickupLongitude,
    shopId = shopId,
    lineKind = lineKind,
    serviceId = serviceId,
    serviceNumber = serviceNumber,
)
