package com.skeler.pulse.sms

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.draftStore: DataStore<Preferences> by preferencesDataStore(name = "draft_preferences")

class DraftPreferences(private val context: Context) {

    fun observeDraft(address: String): Flow<String> {
        val key = draftKey(address)
        return context.draftStore.data.map { prefs ->
            prefs[key] ?: ""
        }
    }

    suspend fun saveDraft(address: String, text: String) {
        val key = draftKey(address)
        context.draftStore.edit { prefs ->
            if (text.isBlank()) {
                prefs.remove(key)
            } else {
                prefs[key] = text
            }
        }
    }

    suspend fun clearDraft(address: String) {
        val key = draftKey(address)
        context.draftStore.edit { prefs ->
            prefs.remove(key)
        }
    }

    suspend fun clearAllDrafts() {
        context.draftStore.edit { prefs ->
            val keys = prefs.asMap().keys.filter { it.name.startsWith(DRAFT_PREFIX) }
            keys.forEach { prefs.remove(it) }
        }
    }

    companion object {
        private const val DRAFT_PREFIX = "draft_"
        private fun draftKey(address: String) = stringPreferencesKey("$DRAFT_PREFIX$address")
    }
}
