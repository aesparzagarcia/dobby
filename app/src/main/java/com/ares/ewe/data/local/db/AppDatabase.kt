package com.ares.ewe.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ares.ewe.data.local.dao.CartDao
import com.ares.ewe.data.local.entity.CartInfo

@Database(
    entities = [CartInfo::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cartDao(): CartDao
}
