# Play Console: R8 shrinking + obfuscation + optimization (mín. 25% desde feb 2027).
# Crashlytics necesita números de línea; el nombre de archivo se oculta.
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions
-renamesourcefileattribute SourceFile

# Kotlin
-dontwarn kotlin.**
-dontwarn kotlinx.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata { *; }

# Gson / Retrofit DTOs (reflexión)
-keep class com.ares.ewe.data.remote.model.** { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepclassmembers enum * { *; }

# Retrofit / OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Hilt / Dagger (las libs ya traen consumer rules; conservar componentes)
-dontwarn dagger.**
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Maps / Location / Code scanner
-keep class com.google.android.gms.maps.** { *; }
-keep class com.google.android.gms.maps.model.** { *; }
-keep class com.google.maps.android.** { *; }
-dontwarn com.google.android.gms.**

# Compose / Coroutines: las AARs ya incluyen reglas. Conservar Continuation.
-keepclassmembers class kotlinx.coroutines.** { *; }

# Security / DataStore
-keep class androidx.security.crypto.** { *; }
-dontwarn org.bouncycastle.**

# Coil
-dontwarn coil.**
