package com.ares.ewe.di

import android.content.Context
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ares.ewe.data.local.dao.CartDao
import com.ares.ewe.data.local.dao.FavoriteProductDao
import com.ares.ewe.data.local.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DATABASE_NAME = "ewe_db"
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `favorite_products` (
                    `productId` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `price` REAL NOT NULL,
                    `imageUrl` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`productId`)
                )
                """.trimIndent()
            )
        }
    }
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `favorite_products` ADD COLUMN `rate` REAL NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `favorite_products` ADD COLUMN `hasPromotion` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `favorite_products` ADD COLUMN `discount` INTEGER NOT NULL DEFAULT 0")
        }
    }
    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE cart ADD COLUMN listPrice REAL NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE cart ADD COLUMN hasPromotion INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE cart ADD COLUMN discount INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE cart ADD COLUMN pickupLatitude REAL")
            db.execSQL("ALTER TABLE cart ADD COLUMN pickupLongitude REAL")
        }
    }

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE cart ADD COLUMN shopId TEXT")
        }
    }

    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE cart ADD COLUMN description TEXT")
        }
    }

    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE cart ADD COLUMN lineKind TEXT NOT NULL DEFAULT 'PRODUCT'")
            db.execSQL("ALTER TABLE cart ADD COLUMN serviceId TEXT")
            db.execSQL("ALTER TABLE cart ADD COLUMN serviceNumber TEXT")
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        DATABASE_NAME
    ).addMigrations(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8,
    ).build()

    @Provides
    @Singleton
    fun provideCartDao(database: AppDatabase): CartDao = database.cartDao()

    @Provides
    @Singleton
    fun provideFavoriteProductDao(database: AppDatabase): FavoriteProductDao = database.favoriteProductDao()
}
