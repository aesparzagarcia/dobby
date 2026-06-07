package com.ares.ewe.di

import com.ares.ewe.domain.repository.CartRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CartRepositoryEntryPoint {
    fun cartRepository(): CartRepository
}
