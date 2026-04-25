package com.skeler.pulse.sms

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ImportantMessagePreferences(
    private val context: Context,
) {
    private val store: DataStore<Preferences>
        get() = context.dataStore

    val importantMessageIds: Flow<Set<Long>> =
        store.data.map { prefs ->
            prefs[KEY_IMPORTANT_MESSAGE_IDS]
                .orEmpty()
                .mapNotNull(String::toLongOrNull)
                .toSet()
        }

    suspend fun toggleImportant(messageId: Long) {
        store.edit { prefs ->
            val current = prefs[KEY_IMPORTANT_MESSAGE_IDS].orEmpty().toMutableSet()
            val serializedId = messageId.toString()
            if (!current.add(serializedId)) {
                current.remove(serializedId)
            }
            prefs[KEY_IMPORTANT_MESSAGE_IDS] = current
        }
    }

    companion object {
        private val Context.dataStore by preferencesDataStore(name = "important_message_prefs")
        private val KEY_IMPORTANT_MESSAGE_IDS = stringSetPreferencesKey("important_message_ids")
    }
}
