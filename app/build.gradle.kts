import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
    alias(libs.plugins.firebase.crashlytics)
    id("kotlin-kapt")
    id("kotlinx-serialization")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        load(keystorePropertiesFile.inputStream())
    }
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
        versionCode = 6
        versionName = "1.0.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Shared API keys (same Google Cloud project; override in gradle.properties).
        buildConfigField("String", "PLACES_API_KEY", "\"${project.findProperty("PLACES_API_KEY") ?: ""}\"")
        buildConfigField("String", "PLACES_ANDROID_CERT", "\"${project.findProperty("PLACES_ANDROID_CERT") ?: ""}\"")
        val mapsApiKey = (project.findProperty("MAPS_API_KEY") as String?)?.takeIf { it.isNotBlank() }
            ?: (project.findProperty("PLACES_API_KEY") as String?) ?: ""
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
        val directionsKey = (project.findProperty("DIRECTIONS_API_KEY") as String?)?.takeIf { it.isNotBlank() }
            ?: mapsApiKey
        buildConfigField("String", "DIRECTIONS_API_KEY", "\"$directionsKey\"")
        manifestPlaceholders["PLACES_API_KEY"] = mapsApiKey
    }

    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            resValue("string", "app_name", "Dobbi Dev")
            buildConfigField("String", "ENVIRONMENT", "\"dev\"")
            val devHost = (project.findProperty("DEV_API_HOST") as String?)?.trim()?.takeIf { it.isNotBlank() }
                ?: "http://192.168.100.61:3001"
            val devPort = (project.findProperty("DEV_API_PORT") as String?)?.trim()?.takeIf { it.isNotBlank() }
            val devBase = if (devPort != null && !devHost.contains(":")) {
                "$devHost:$devPort"
            } else {
                devHost
            }
            buildConfigField("String", "BASE_URL", "\"$devBase/api/\"")
        }
        create("prod") {
            dimension = "environment"
            resValue("string", "app_name", "Dobbi")
            buildConfigField("String", "ENVIRONMENT", "\"prod\"")
            val prodHost = (project.findProperty("PROD_API_HOST") as String?)?.trim()?.takeIf { it.isNotBlank() }
                ?: "https://dobby-api-31lf.onrender.com"
            buildConfigField("String", "BASE_URL", "\"$prodHost/api/\"")
        }
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
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

    // DataStore + encrypted token storage (Android Keystore)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)

    // Maps & Location
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.maps.compose)
    // Misma lógica punto-en-polígono que el ecosistema Google Maps (evita desajustes con LatLng).
    implementation("com.google.maps.android:android-maps-utils:3.10.0")

    // Escáner de códigos de barras (pago de servicios)
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")

    // Firebase (FCM, Auth, Firestore) — use BOM only; do not add a second firebase-auth version.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
}