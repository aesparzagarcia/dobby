package com.ares.ewe.di

import com.ares.ewe.BuildConfig
import com.ares.ewe.data.local.datastore.SessionManager
import com.ares.ewe.data.remote.TokenRefreshInterceptor
import com.ares.ewe.data.remote.api.DobbyApi
import com.ares.ewe.data.remote.api.GoogleDirectionsApi
import com.ares.ewe.data.remote.api.GooglePlacesApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlin.annotation.AnnotationRetention
import kotlin.annotation.Retention

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GoogleRetrofit

@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {

    companion object {
        private const val GOOGLE_PLACES_BASE_URL = "https://maps.googleapis.com/maps/api/"
    }

    @Provides
    @Singleton
    @DobbyNoAuthClient
    fun provideNoAuthOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        sessionManager: SessionManager,
        tokenRefreshInterceptor: TokenRefreshInterceptor
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val authInterceptor = Interceptor { chain ->
            val req = chain.request()
            if (req.header("Authorization") != null) {
                chain.proceed(req)
            } else {
                val token = runBlocking { sessionManager.authToken.first() }
                val request = if (!token.isNullOrBlank()) {
                    req.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } else req
                chain.proceed(request)
            }
        }
        return OkHttpClient.Builder()
            .addInterceptor(tokenRefreshInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Separate client without Dobby auth/refresh: avoids sending Bearer to Google and prevents
     * treating Places 401 as session expiry. Adds Android cert headers for Places (New) APIs.
     */
    @Provides
    @Singleton
    @GoogleRetrofit
    fun provideGoogleRetrofit(): Retrofit {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
        if (BuildConfig.PLACES_ANDROID_CERT.isNotBlank()) {
            builder.addInterceptor(Interceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .addHeader("X-Android-Package", BuildConfig.APPLICATION_ID)
                        .addHeader("X-Android-Cert", BuildConfig.PLACES_ANDROID_CERT)
                        .build()
                )
            })
        }
        return Retrofit.Builder()
            .baseUrl(GOOGLE_PLACES_BASE_URL)
            .client(builder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Directions API is a classic web service: do not send Places Android headers. Use a key whose
     * application restriction allows web service usage (see [com.ares.ewe.BuildConfig.DIRECTIONS_API_KEY]).
     */
    @Provides
    @Singleton
    @DirectionsGoogleRetrofit
    fun provideDirectionsGoogleRetrofit(): Retrofit {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
            )
        }
        return Retrofit.Builder()
            .baseUrl(GOOGLE_PLACES_BASE_URL)
            .client(builder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideDobbyApi(retrofit: Retrofit): DobbyApi {
        return retrofit.create(DobbyApi::class.java)
    }

    @Provides
    @Singleton
    fun provideGooglePlacesApi(@GoogleRetrofit retrofit: Retrofit): GooglePlacesApi {
        return retrofit.create(GooglePlacesApi::class.java)
    }

    @Provides
    @Singleton
    fun provideGoogleDirectionsApi(
        @DirectionsGoogleRetrofit retrofit: Retrofit
    ): GoogleDirectionsApi {
        return retrofit.create(GoogleDirectionsApi::class.java)
    }
}
