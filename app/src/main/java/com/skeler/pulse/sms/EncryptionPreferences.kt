package com.skeler.pulse.sms

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class EncryptionPreferences(
    private val context: Context,
) {
    private val store: DataStore<Preferences>
        get() = context.dataStore

    val encryptionEnabled: Flow<Boolean> =
        store.data.map { prefs -> prefs[KEY_ENCRYPTION_ENABLED] ?: false }

    suspend fun isEncryptionEnabled(): Boolean = encryptionEnabled.first()

    suspend fun setEncryptionEnabled(value: Boolean) {
        store.edit { prefs -> prefs[KEY_ENCRYPTION_ENABLED] = value }
    }

    companion object {
        private val Context.dataStore by preferencesDataStore(name = "encryption_prefs")
        private val KEY_ENCRYPTION_ENABLED = booleanPreferencesKey("encryption_enabled")
    }
}
