package com.ares.ewe.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ares.ewe.data.local.entity.FavoriteProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteProductDao {

    @Query("SELECT * FROM favorite_products ORDER BY createdAt DESC")
    fun getAll(): Flow<List<FavoriteProductEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_products WHERE productId = :productId)")
    fun isFavorite(productId: String): Flow<Boolean>

    @Query("SELECT * FROM favorite_products WHERE productId = :productId LIMIT 1")
    suspend fun getById(productId: String): FavoriteProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: FavoriteProductEntity)

    @Query("DELETE FROM favorite_products WHERE productId = :productId")
    suspend fun deleteById(productId: String)
}
