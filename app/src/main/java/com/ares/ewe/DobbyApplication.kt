package com.ares.ewe

import android.app.Application
import com.ares.ewe.core.crash.CrashlyticsJourney
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
        CrashlyticsJourney.setApp("dobby")
        proactiveAccessTokenRefresh.start()
        // LATEST triggers cloud "client parameters" RPCs (policy_maps_core_dynamite) that often
        // time out on emulators or devices without full Play Store — same Logcat noise, not our HTTP geocode.
        MapsInitializer.initialize(applicationContext, MapsInitializer.Renderer.LEGACY) { }
    }
}