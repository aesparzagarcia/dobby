package com.ares.ewe.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ares.ewe.data.local.dao.CartDao
import com.ares.ewe.data.local.dao.FavoriteProductDao
import com.ares.ewe.data.local.entity.CartInfo
import com.ares.ewe.data.local.entity.FavoriteProductEntity

@Database(
    entities = [CartInfo::class, FavoriteProductEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cartDao(): CartDao
    abstract fun favoriteProductDao(): FavoriteProductDao
}
