package com.skeler.pulse.sms

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.skeler.pulse.contact.matchesBlockedSenderKey
import com.skeler.pulse.contact.toBlockedSenderKeyOrNull
import com.skeler.pulse.contact.toCanonicalBlockedSenderKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class InboxThreadPreferences(
    private val context: Context,
) {
    private val store: DataStore<Preferences>
        get() = context.dataStore

    val pinnedThreadIds: Flow<Set<Long>> =
        store.data.map { prefs ->
            prefs[KEY_PINNED_THREAD_IDS]
                .orEmpty()
                .mapNotNull(String::toLongOrNull)
                .toSet()
        }

    val archivedThreadIds: Flow<Set<Long>> =
        store.data.map { prefs ->
            prefs[KEY_ARCHIVED_THREAD_IDS]
                .orEmpty()
                .mapNotNull(String::toLongOrNull)
                .toSet()
        }

    val blockedAddresses: Flow<Set<String>> =
        store.data.map { prefs ->
            prefs[KEY_BLOCKED_ADDRESSES].orEmpty().toCanonicalBlockedSenderKeys()
        }

    suspend fun togglePinned(threadId: Long) {
        store.edit { prefs ->
            val current = prefs[KEY_PINNED_THREAD_IDS].orEmpty().toMutableSet()
            val id = threadId.toString()
            if (!current.add(id)) {
                current.remove(id)
            }
            prefs[KEY_PINNED_THREAD_IDS] = current
        }
    }

    suspend fun toggleArchived(threadId: Long) {
        store.edit { prefs ->
            val current = prefs[KEY_ARCHIVED_THREAD_IDS].orEmpty().toMutableSet()
            val id = threadId.toString()
            if (!current.add(id)) {
                current.remove(id)
            }
            prefs[KEY_ARCHIVED_THREAD_IDS] = current
        }
    }

    suspend fun blockAddress(address: String) {
        val blockedKey = address.toBlockedSenderKeyOrNull() ?: return

        store.edit { prefs ->
            val current = prefs[KEY_BLOCKED_ADDRESSES].orEmpty().toMutableSet()
            current.removeAll { existing -> existing.matchesBlockedSenderKey(blockedKey) }
            current.add(blockedKey)
            prefs[KEY_BLOCKED_ADDRESSES] = current
        }
    }

    suspend fun unblockAddress(address: String) {
        val blockedKey = address.toBlockedSenderKeyOrNull() ?: return

        store.edit { prefs ->
            val current = prefs[KEY_BLOCKED_ADDRESSES].orEmpty().toMutableSet()
            if (current.removeAll { existing -> existing.matchesBlockedSenderKey(blockedKey) }) {
                prefs[KEY_BLOCKED_ADDRESSES] = current
            }
        }
    }

    suspend fun removeThread(threadId: Long) {
        val id = threadId.toString()
        store.edit { prefs ->
            val pinned = prefs[KEY_PINNED_THREAD_IDS].orEmpty().toMutableSet()
            val archived = prefs[KEY_ARCHIVED_THREAD_IDS].orEmpty().toMutableSet()
            val pinnedChanged = pinned.remove(id)
            val archivedChanged = archived.remove(id)
            if (pinnedChanged) prefs[KEY_PINNED_THREAD_IDS] = pinned
            if (archivedChanged) prefs[KEY_ARCHIVED_THREAD_IDS] = archived
        }
    }

    companion object {
        private val Context.dataStore by preferencesDataStore(name = "inbox_thread_prefs")
        private val KEY_PINNED_THREAD_IDS = stringSetPreferencesKey("pinned_thread_ids")
        private val KEY_ARCHIVED_THREAD_IDS = stringSetPreferencesKey("archived_thread_ids")
        private val KEY_BLOCKED_ADDRESSES = stringSetPreferencesKey("blocked_addresses")
    }
}
