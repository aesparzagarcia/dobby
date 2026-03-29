package com.ares.ewe.di

import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention
import kotlin.annotation.Retention

/** Retrofit instance used only for Maps Directions API (web service). No Places/Android cert headers. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DirectionsGoogleRetrofit
