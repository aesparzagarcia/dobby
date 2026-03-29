package com.ares.ewe.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ares.ewe.data.local.entity.CartInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {

    @Query("SELECT * FROM cart ORDER BY productId")
    fun getAll(): Flow<List<CartInfo>>

    @Query("SELECT * FROM cart WHERE productId = :productId LIMIT 1")
    suspend fun getByProductId(productId: String): CartInfo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CartInfo)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CartInfo>)

    @Query("DELETE FROM cart WHERE productId = :productId")
    suspend fun deleteByProductId(productId: String)

    @Query("UPDATE cart SET quantity = :quantity WHERE productId = :productId")
    suspend fun updateQuantity(productId: String, quantity: Int)

    @Query("DELETE FROM cart")
    suspend fun deleteAll()
}
