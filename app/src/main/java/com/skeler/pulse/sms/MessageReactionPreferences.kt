package com.skeler.pulse.sms

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MessageReactionPreferences(
    private val context: Context,
) {
    private val store: DataStore<Preferences>
        get() = context.dataStore

    val messageReactions: Flow<Map<Long, String>> =
        store.data.map { prefs ->
            prefs[KEY_MESSAGE_REACTIONS].orEmpty().mapNotNull { entry ->
                val colon = entry.lastIndexOf(':')
                if (colon <= 0) return@mapNotNull null
                val id = entry.substring(0, colon).toLongOrNull() ?: return@mapNotNull null
                val emoji = entry.substring(colon + 1)
                if (emoji.isEmpty()) null else id to emoji
            }.toMap()
        }

    suspend fun setReaction(messageId: Long, emoji: String?) {
        val id = messageId.toString()
        store.edit { prefs ->
            val current = prefs[KEY_MESSAGE_REACTIONS].orEmpty().toMutableSet()
            current.removeAll { it.startsWith("$id:") }
            if (!emoji.isNullOrBlank()) {
                current.add("$id:$emoji")
            }
            prefs[KEY_MESSAGE_REACTIONS] = current
        }
    }

    companion object {
        private val Context.dataStore by preferencesDataStore(name = "message_reaction_prefs")
        private val KEY_MESSAGE_REACTIONS = stringSetPreferencesKey("message_reactions")
    }
}
