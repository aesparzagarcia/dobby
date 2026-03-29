package com.ares.ewe.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class SavedAddress(
    val description: String,
    val address: String,
    val lat: Double,
    val lng: Double
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val USER_ID = stringPreferencesKey("user_id")
        val SAVED_ADDRESS_DESCRIPTION = stringPreferencesKey("saved_address_description")
        val SAVED_ADDRESS_TEXT = stringPreferencesKey("saved_address_text")
        val SAVED_ADDRESS_LAT = stringPreferencesKey("saved_address_lat")
        val SAVED_ADDRESS_LNG = stringPreferencesKey("saved_address_lng")
    }

    val authToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUTH_TOKEN]
    }

    val refreshToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.REFRESH_TOKEN]
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        !prefs[Keys.AUTH_TOKEN].isNullOrBlank() || !prefs[Keys.REFRESH_TOKEN].isNullOrBlank()
    }

    val savedAddress: Flow<SavedAddress?> = context.dataStore.data.map { prefs ->
        val desc = prefs[Keys.SAVED_ADDRESS_DESCRIPTION] ?: return@map null
        val address = prefs[Keys.SAVED_ADDRESS_TEXT] ?: return@map null
        val lat = prefs[Keys.SAVED_ADDRESS_LAT]?.toDoubleOrNull() ?: return@map null
        val lng = prefs[Keys.SAVED_ADDRESS_LNG]?.toDoubleOrNull() ?: return@map null
        SavedAddress(description = desc, address = address, lat = lat, lng = lng)
    }

    suspend fun saveSession(accessToken: String, refreshToken: String, userId: String? = null) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTH_TOKEN] = accessToken
            prefs[Keys.REFRESH_TOKEN] = refreshToken
            userId?.let { prefs[Keys.USER_ID] = it }
        }
    }

    suspend fun saveAddress(description: String, address: String, lat: Double, lng: Double) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SAVED_ADDRESS_DESCRIPTION] = description
            prefs[Keys.SAVED_ADDRESS_TEXT] = address
            prefs[Keys.SAVED_ADDRESS_LAT] = lat.toString()
            prefs[Keys.SAVED_ADDRESS_LNG] = lng.toString()
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.AUTH_TOKEN)
            prefs.remove(Keys.REFRESH_TOKEN)
            prefs.remove(Keys.USER_ID)
        }
    }
}
