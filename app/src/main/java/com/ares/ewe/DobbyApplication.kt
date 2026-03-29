package com.ares.ewe

import android.app.Application
import com.ares.ewe.session.ProactiveAccessTokenRefresh
import com.google.android.gms.maps.MapsInitializer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class DobbyApplication : Application() {

    @Inject
    lateinit var proactiveAccessTokenRefresh: ProactiveAccessTokenRefresh

    override fun onCreate() {
        super.onCreate()
        proactiveAccessTokenRefresh.start()
        MapsInitializer.initialize(applicationContext, MapsInitializer.Renderer.LATEST) { }
    }
}