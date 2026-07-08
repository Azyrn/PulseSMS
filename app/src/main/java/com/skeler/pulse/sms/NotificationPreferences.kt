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

class NotificationPreferences(
    private val context: Context,
) {
    private val store: DataStore<Preferences>
        get() = context.dataStore

    val quickReplyEnabled: Flow<Boolean> =
        store.data.map { prefs -> prefs[KEY_QUICK_REPLY] ?: true }

    val quickComposeEnabled: Flow<Boolean> =
        store.data.map { prefs -> prefs[KEY_QUICK_COMPOSE] ?: false }

    suspend fun isQuickReplyEnabled(): Boolean =
        quickReplyEnabled.first()

    suspend fun setQuickReplyEnabled(enabled: Boolean) {
        store.edit { prefs -> prefs[KEY_QUICK_REPLY] = enabled }
    }

    suspend fun isQuickComposeEnabled(): Boolean =
        quickComposeEnabled.first()

    suspend fun setQuickComposeEnabled(enabled: Boolean) {
        store.edit { prefs -> prefs[KEY_QUICK_COMPOSE] = enabled }
    }

    companion object {
        private val Context.dataStore by preferencesDataStore(name = "notification_prefs")
        private val KEY_QUICK_REPLY = booleanPreferencesKey("quick_reply_enabled")
        private val KEY_QUICK_COMPOSE = booleanPreferencesKey("quick_compose_enabled")
    }
}
