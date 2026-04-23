plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
    id("kotlin-kapt")
    id("kotlinx-serialization")
}

android {
    namespace = "com.ares.ewe"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.ares.ewe"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Emulator: 10.0.2.2 = host machine's localhost. Backend mounts at /api (e.g. /api/auth/request-otp).
        buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:3001/api/\"")
        // Replace with your Google Places API key (enable Places API in Cloud Console)
        buildConfigField("String", "PLACES_API_KEY", "\"${project.findProperty("PLACES_API_KEY") ?: ""}\"")
        // SHA-1 of your signing key (no colons), for Android app restriction. Get with: keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
        buildConfigField("String", "PLACES_ANDROID_CERT", "\"${project.findProperty("PLACES_ANDROID_CERT") ?: ""}\"")
        // Maps / Places bundle key (manifest / SDK).
        val mapsApiKey = (project.findProperty("MAPS_API_KEY") as String?)?.takeIf { it.isNotBlank() }
            ?: (project.findProperty("PLACES_API_KEY") as String?) ?: ""
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
        // Directions API (REST): prefer a separate key with "None" or "IP" app restriction — Android-only keys often get REQUEST_DENIED.
        val directionsKey = (project.findProperty("DIRECTIONS_API_KEY") as String?)?.takeIf { it.isNotBlank() }
            ?: mapsApiKey
        buildConfigField("String", "DIRECTIONS_API_KEY", "\"$directionsKey\"")
        manifestPlaceholders["PLACES_API_KEY"] = project.findProperty("PLACES_API_KEY") ?: ""
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    kapt {
        correctErrorTypes = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-stdlib:1.9.24")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.24")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.coil.compose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // DI
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    kapt(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)

    // OkHttp
    implementation(libs.okhttp)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.play.services)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)

    // Maps & Location
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.maps.compose)

    // Push (FCM)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
}