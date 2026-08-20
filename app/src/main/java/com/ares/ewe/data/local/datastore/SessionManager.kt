package com.ares.ewe.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    private val tokenStore = EncryptedTokenStore(context)
    private val migrationMutex = Mutex()
    @Volatile private var migrated = false

    private val _authToken = MutableStateFlow(tokenStore.accessToken)
    private val _refreshToken = MutableStateFlow(tokenStore.refreshToken)

    val authToken: Flow<String?> = flow {
        migrateLegacyTokensIfNeeded()
        _authToken.value = tokenStore.accessToken
        emitAll(_authToken)
    }

    val refreshToken: Flow<String?> = flow {
        migrateLegacyTokensIfNeeded()
        _refreshToken.value = tokenStore.refreshToken
        emitAll(_refreshToken)
    }

    val isLoggedIn: Flow<Boolean> = combine(authToken, refreshToken) { access, refresh ->
        !access.isNullOrBlank() || !refresh.isNullOrBlank()
    }

    val userId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.USER_ID]
    }

    val savedAddress: Flow<SavedAddress?> = context.dataStore.data.map { prefs ->
        val desc = prefs[Keys.SAVED_ADDRESS_DESCRIPTION] ?: return@map null
        val address = prefs[Keys.SAVED_ADDRESS_TEXT] ?: return@map null
        val lat = prefs[Keys.SAVED_ADDRESS_LAT]?.toDoubleOrNull() ?: return@map null
        val lng = prefs[Keys.SAVED_ADDRESS_LNG]?.toDoubleOrNull() ?: return@map null
        SavedAddress(description = desc, address = address, lat = lat, lng = lng)
    }

    private suspend fun migrateLegacyTokensIfNeeded() {
        if (migrated) return
        migrationMutex.withLock {
            if (migrated) return
            val hasSecure =
                !tokenStore.accessToken.isNullOrBlank() || !tokenStore.refreshToken.isNullOrBlank()
            if (!hasSecure) {
                val prefs = context.dataStore.data.first()
                val legacyAccess = prefs[Keys.AUTH_TOKEN]
                val legacyRefresh = prefs[Keys.REFRESH_TOKEN]
                if (!legacyAccess.isNullOrBlank() || !legacyRefresh.isNullOrBlank()) {
                    tokenStore.save(
                        accessToken = legacyAccess.orEmpty(),
                        refreshToken = legacyRefresh.orEmpty(),
                    )
                    _authToken.value = tokenStore.accessToken
                    _refreshToken.value = tokenStore.refreshToken
                }
            }
            // Always scrub plaintext leftovers after first launch of this version.
            context.dataStore.edit { prefs ->
                prefs.remove(Keys.AUTH_TOKEN)
                prefs.remove(Keys.REFRESH_TOKEN)
            }
            migrated = true
        }
    }

    suspend fun saveSession(accessToken: String, refreshToken: String, userId: String? = null) {
        migrateLegacyTokensIfNeeded()
        tokenStore.save(accessToken, refreshToken)
        _authToken.value = accessToken
        _refreshToken.value = refreshToken
        if (userId != null) {
            context.dataStore.edit { prefs ->
                prefs[Keys.USER_ID] = userId
            }
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
        migrateLegacyTokensIfNeeded()
        tokenStore.clear()
        _authToken.value = null
        _refreshToken.value = null
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.AUTH_TOKEN)
            prefs.remove(Keys.REFRESH_TOKEN)
            prefs.remove(Keys.USER_ID)
        }
    }

    /** Call once at app start so Flows see migrated tokens before the first API call. */
    suspend fun prepareSession() {
        migrateLegacyTokensIfNeeded()
        _authToken.value = tokenStore.accessToken
        _refreshToken.value = tokenStore.refreshToken
    }
}
