package com.ares.ewe.di

import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention
import kotlin.annotation.Retention

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class DobbyNoAuthClient
