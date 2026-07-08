package com.skeler.pulse.sms

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.cleanupDataStore: DataStore<Preferences> by preferencesDataStore(name = "message_cleanup_prefs")

class MessageCleanupPreferences(
    private val context: Context,
) {
    private val store: DataStore<Preferences> = context.cleanupDataStore

    val maxSmsPerThread: Flow<Int> =
        store.data.map { prefs -> prefs[KEY_MAX_SMS_PER_THREAD] ?: KEEP_ALL }

    val maxMmsPerThread: Flow<Int> =
        store.data.map { prefs -> prefs[KEY_MAX_MMS_PER_THREAD] ?: KEEP_ALL }

    val lastSmsValue: Flow<Int> =
        store.data.map { prefs -> prefs[KEY_LAST_SMS_VALUE] ?: DEFAULT_KEEP_COUNT }

    val lastMmsValue: Flow<Int> =
        store.data.map { prefs -> prefs[KEY_LAST_MMS_VALUE] ?: DEFAULT_KEEP_COUNT }

    suspend fun setMaxSmsPerThread(value: Int) {
        store.edit { prefs ->
            prefs[KEY_MAX_SMS_PER_THREAD] = value
            if (value != KEEP_ALL) prefs[KEY_LAST_SMS_VALUE] = value
        }
    }

    suspend fun setMaxMmsPerThread(value: Int) {
        store.edit { prefs ->
            prefs[KEY_MAX_MMS_PER_THREAD] = value
            if (value != KEEP_ALL) prefs[KEY_LAST_MMS_VALUE] = value
        }
    }

    suspend fun getMaxSmsPerThread(): Int = maxSmsPerThread.first()
    suspend fun getMaxMmsPerThread(): Int = maxMmsPerThread.first()
    suspend fun getLastSmsValue(): Int = lastSmsValue.first()
    suspend fun getLastMmsValue(): Int = lastMmsValue.first()

    companion object {
        private val KEY_MAX_SMS_PER_THREAD = intPreferencesKey("max_sms_per_thread")
        private val KEY_MAX_MMS_PER_THREAD = intPreferencesKey("max_mms_per_thread")
        private val KEY_LAST_SMS_VALUE = intPreferencesKey("last_sms_value")
        private val KEY_LAST_MMS_VALUE = intPreferencesKey("last_mms_value")
        const val KEEP_ALL = -1
        const val DEFAULT_KEEP_COUNT = 50
    }
}
