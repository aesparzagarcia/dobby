package com.ares.ewe.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ares.ewe.data.local.entity.CartInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {

    @Query("SELECT * FROM cart ORDER BY productId, variantId")
    fun getAll(): Flow<List<CartInfo>>

    @Query("SELECT * FROM cart WHERE productId = :productId AND variantId = :variantId LIMIT 1")
    suspend fun getByLine(productId: String, variantId: String): CartInfo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CartInfo)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CartInfo>)

    @Query("DELETE FROM cart WHERE productId = :productId AND variantId = :variantId")
    suspend fun deleteByLine(productId: String, variantId: String)

    @Query("UPDATE cart SET quantity = :quantity WHERE productId = :productId AND variantId = :variantId")
    suspend fun updateQuantity(productId: String, variantId: String, quantity: Int)

    @Query("DELETE FROM cart")
    suspend fun deleteAll()

    @Query("DELETE FROM cart WHERE lineKind = :lineKind")
    suspend fun deleteByLineKind(lineKind: String)

    @Query("SELECT * FROM cart WHERE lineKind = :lineKind")
    suspend fun getByLineKind(lineKind: String): List<CartInfo>
}
