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

    suspend fun setMaxSmsPerThread(value: Int) {
        store.edit { prefs -> prefs[KEY_MAX_SMS_PER_THREAD] = value }
    }

    suspend fun setMaxMmsPerThread(value: Int) {
        store.edit { prefs -> prefs[KEY_MAX_MMS_PER_THREAD] = value }
    }

    suspend fun getMaxSmsPerThread(): Int = maxSmsPerThread.first()
    suspend fun getMaxMmsPerThread(): Int = maxMmsPerThread.first()

    companion object {
        private val KEY_MAX_SMS_PER_THREAD = intPreferencesKey("max_sms_per_thread")
        private val KEY_MAX_MMS_PER_THREAD = intPreferencesKey("max_mms_per_thread")
        const val KEEP_ALL = -1
        val PRESET_VALUES = listOf(10, 20, 50, 100, KEEP_ALL)
    }
}
