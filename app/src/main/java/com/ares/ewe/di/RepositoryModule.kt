package com.ares.ewe.di

import com.ares.ewe.data.repository.AdsRepositoryImpl
import com.ares.ewe.data.repository.AuthRepositoryImpl
import com.ares.ewe.data.repository.CartRepositoryImpl
import com.ares.ewe.data.repository.DirectionsRepositoryImpl
import com.ares.ewe.data.repository.FavoritesRepositoryImpl
import com.ares.ewe.data.repository.OrderRepositoryImpl
import com.ares.ewe.data.repository.PlacesAutocompleteRepositoryImpl
import com.ares.ewe.data.repository.ProfileRepositoryImpl
import com.ares.ewe.data.repository.PlacesRepositoryImpl
import com.ares.ewe.data.repository.UserAddressRepositoryImpl
import com.ares.ewe.domain.repository.AdsRepository
import com.ares.ewe.domain.repository.AuthRepository
import com.ares.ewe.domain.repository.CartRepository
import com.ares.ewe.domain.repository.DirectionsRepository
import com.ares.ewe.domain.repository.FavoritesRepository
import com.ares.ewe.domain.repository.OrderRepository
import com.ares.ewe.domain.repository.PlacesAutocompleteRepository
import com.ares.ewe.domain.repository.ProfileRepository
import com.ares.ewe.domain.repository.PlacesRepository
import com.ares.ewe.domain.repository.UserAddressRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindPlacesRepository(impl: PlacesRepositoryImpl): PlacesRepository

    @Binds
    @Singleton
    abstract fun bindAdsRepository(impl: AdsRepositoryImpl): AdsRepository

    @Binds
    @Singleton
    abstract fun bindPlacesAutocompleteRepository(impl: PlacesAutocompleteRepositoryImpl): PlacesAutocompleteRepository

    @Binds
    @Singleton
    abstract fun bindUserAddressRepository(impl: UserAddressRepositoryImpl): UserAddressRepository

    @Binds
    @Singleton
    abstract fun bindCartRepository(impl: CartRepositoryImpl): CartRepository

    @Binds
    @Singleton
    abstract fun bindFavoritesRepository(impl: FavoritesRepositoryImpl): FavoritesRepository

    @Binds
    @Singleton
    abstract fun bindOrderRepository(impl: OrderRepositoryImpl): OrderRepository

    @Binds
    @Singleton
    abstract fun bindDirectionsRepository(impl: DirectionsRepositoryImpl): DirectionsRepository
}
