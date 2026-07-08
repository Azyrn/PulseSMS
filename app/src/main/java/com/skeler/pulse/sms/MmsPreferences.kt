package com.skeler.pulse.sms

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class MmsPreferences(
    private val context: Context,
) {
    private val store: DataStore<Preferences>
        get() = context.dataStore

    val maxImageSizeKb: Flow<Int> =
        store.data.map { prefs -> prefs[KEY_MAX_IMAGE_SIZE_KB] ?: DEFAULT_MAX_IMAGE_SIZE_KB }

    suspend fun getMaxImageSizeKb(): Int = maxImageSizeKb.first()

    suspend fun setMaxImageSizeKb(value: Int) {
        store.edit { prefs -> prefs[KEY_MAX_IMAGE_SIZE_KB] = value }
    }

    suspend fun getMmscUrl(): String? =
        store.data.map { prefs -> prefs[KEY_MMSC_URL] }.first()

    suspend fun getMmsProxy(): String? =
        store.data.map { prefs -> prefs[KEY_MMS_PROXY] }.first()

    suspend fun getMmsPort(): String? =
        store.data.map { prefs -> prefs[KEY_MMS_PORT] }.first()

    suspend fun setMmsProxy(proxyHost: String?, proxyPort: String?, mmscUrl: String?) {
        store.edit { prefs ->
            if (mmscUrl != null) prefs[KEY_MMSC_URL] = mmscUrl
            if (proxyHost != null) prefs[KEY_MMS_PROXY] = proxyHost
            if (proxyPort != null) prefs[KEY_MMS_PORT] = proxyPort
        }
    }

    companion object {
        private val Context.dataStore by preferencesDataStore(name = "mms_prefs")
        private val KEY_MAX_IMAGE_SIZE_KB = intPreferencesKey("mms_max_image_size_kb")
        private val KEY_MMSC_URL = stringPreferencesKey("mmsc_url")
        private val KEY_MMS_PROXY = stringPreferencesKey("mms_proxy")
        private val KEY_MMS_PORT = stringPreferencesKey("mms_port")
        const val DEFAULT_MAX_IMAGE_SIZE_KB = 500
        const val UNLIMITED = -1
    }
}
