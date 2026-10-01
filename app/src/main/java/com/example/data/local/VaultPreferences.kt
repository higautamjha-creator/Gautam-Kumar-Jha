package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.FirebaseRuntimeConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.vaultDataStore by preferencesDataStore(name = "docuvault_prefs")

data class SecurityLockSettings(
    val biometricLockEnabled: Boolean = true,
    val autoLockOnBackground: Boolean = true,
    val vaultPinHash: String = ""
)

class VaultPreferences(private val context: Context) {

    private object Keys {
        val PROJECT_ID = stringPreferencesKey("fb_project_id")
        val APPLICATION_ID = stringPreferencesKey("fb_app_id")
        val API_KEY = stringPreferencesKey("fb_api_key")
        val STORAGE_BUCKET = stringPreferencesKey("fb_storage_bucket")
        val WEB_CLIENT_ID = stringPreferencesKey("fb_web_client_id")
        val AUTO_SYNC_WIFI = booleanPreferencesKey("auto_sync_enabled")
        val OFFLINE_CACHE_DEFAULT = booleanPreferencesKey("offline_cache_default")
        val BIOMETRIC_LOCK_ENABLED = booleanPreferencesKey("biometric_lock_enabled")
        val AUTO_LOCK_BACKGROUND = booleanPreferencesKey("auto_lock_background")
        val VAULT_PIN_HASH = stringPreferencesKey("vault_pin_sha256_hash")
    }

    val firebaseConfigFlow: Flow<FirebaseRuntimeConfig> = context.vaultDataStore.data.map { prefs ->
        FirebaseRuntimeConfig(
            projectId = prefs[Keys.PROJECT_ID] ?: "",
            applicationId = prefs[Keys.APPLICATION_ID] ?: "",
            apiKey = prefs[Keys.API_KEY] ?: "",
            storageBucket = prefs[Keys.STORAGE_BUCKET] ?: "",
            webClientId = prefs[Keys.WEB_CLIENT_ID] ?: ""
        )
    }

    val autoSyncEnabledFlow: Flow<Boolean> = context.vaultDataStore.data.map { prefs ->
        prefs[Keys.AUTO_SYNC_WIFI] ?: true
    }

    val offlineCacheDefaultFlow: Flow<Boolean> = context.vaultDataStore.data.map { prefs ->
        prefs[Keys.OFFLINE_CACHE_DEFAULT] ?: true
    }

    val securityLockSettingsFlow: Flow<SecurityLockSettings> = context.vaultDataStore.data.map { prefs ->
        SecurityLockSettings(
            biometricLockEnabled = prefs[Keys.BIOMETRIC_LOCK_ENABLED] ?: true,
            autoLockOnBackground = prefs[Keys.AUTO_LOCK_BACKGROUND] ?: true,
            vaultPinHash = prefs[Keys.VAULT_PIN_HASH] ?: ""
        )
    }

    suspend fun saveFirebaseConfig(config: FirebaseRuntimeConfig) {
        context.vaultDataStore.edit { prefs ->
            prefs[Keys.PROJECT_ID] = config.projectId.trim()
            prefs[Keys.APPLICATION_ID] = config.applicationId.trim()
            prefs[Keys.API_KEY] = config.apiKey.trim()
            prefs[Keys.STORAGE_BUCKET] = config.storageBucket.trim()
            prefs[Keys.WEB_CLIENT_ID] = config.webClientId.trim()
        }
    }

    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        context.vaultDataStore.edit { prefs ->
            prefs[Keys.AUTO_SYNC_WIFI] = enabled
        }
    }

    suspend fun setOfflineCacheDefault(enabled: Boolean) {
        context.vaultDataStore.edit { prefs ->
            prefs[Keys.OFFLINE_CACHE_DEFAULT] = enabled
        }
    }

    suspend fun setBiometricLockEnabled(enabled: Boolean) {
        context.vaultDataStore.edit { prefs ->
            prefs[Keys.BIOMETRIC_LOCK_ENABLED] = enabled
        }
    }

    suspend fun setAutoLockOnBackground(enabled: Boolean) {
        context.vaultDataStore.edit { prefs ->
            prefs[Keys.AUTO_LOCK_BACKGROUND] = enabled
        }
    }

    suspend fun saveVaultPinHash(pinHash: String) {
        context.vaultDataStore.edit { prefs ->
            prefs[Keys.VAULT_PIN_HASH] = pinHash
        }
    }
}
