package com.ares.ewe.di

import com.ares.ewe.domain.repository.OrderRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface OrderRepositoryEntryPoint {
    fun orderRepository(): OrderRepository
}
