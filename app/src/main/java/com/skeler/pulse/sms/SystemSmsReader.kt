package com.skeler.pulse.sms

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import androidx.compose.runtime.Immutable
import com.skeler.pulse.contact.normalizeAddressForDisplay
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Data class representing a single SMS message from the system content provider.
 */
@Immutable
data class SystemSms(
    val id: Long,
    val address: String,
    val body: String,
    val date: Long,
    val type: Int, // 1=inbox, 2=sent, 3=draft
    val read: Boolean,
    val threadId: Long,
) {
    val isInbound: Boolean get() = type == Telephony.Sms.MESSAGE_TYPE_INBOX
    val isOutbound: Boolean get() = type == Telephony.Sms.MESSAGE_TYPE_SENT
    val timestamp: Instant get() = Instant.ofEpochMilli(date)
}

/**
 * Data class representing a conversation thread (grouped by address).
 */
@Immutable
data class SmsThread(
    val threadId: Long,
    val address: String,
    val snippet: String,
    val date: Long,
    val messageCount: Int,
    val unreadCount: Int,
) {
    val timestamp: Instant get() = Instant.ofEpochMilli(date)
}

/**
 * Reads real SMS messages from the Android system content provider (`content://sms`).
 *
 * This replaces the fake in-memory data with actual phone messages.
 * Requires [android.permission.READ_SMS] permission.
 */
class SystemSmsReader(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    private val contentResolver: ContentResolver get() = context.contentResolver

    /**
     * Observes all conversation threads as a reactive [Flow].
     * Emits a new list whenever the SMS content provider changes.
     */
    fun observeThreads(): Flow<List<SmsThread>> = callbackFlow {
        var readJob: Job? = null
        fun scheduleRead() {
            readJob?.cancel()
            readJob = launch(ioDispatcher) {
                try {
                    trySend(readThreads())
                } catch (e: SecurityException) {
                    Log.w("SystemSmsReader", "READ_SMS permission not granted", e)
                    trySend(emptyList())
                }
            }
        }

        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                scheduleRead()
            }
        }
        contentResolver.registerContentObserver(
            Telephony.Sms.CONTENT_URI, true, observer
        )
        // Initial emission
        scheduleRead()
        awaitClose {
            readJob?.cancel()
            contentResolver.unregisterContentObserver(observer)
        }
    }.distinctUntilChanged()

    /**
     * Observes messages for a specific address/thread as a reactive [Flow].
     */
    fun observeMessages(address: String, threadId: Long? = null): Flow<List<SystemSms>> = callbackFlow {
        var readJob: Job? = null
        fun scheduleRead() {
            readJob?.cancel()
            readJob = launch(ioDispatcher) {
                try {
                    trySend(readMessages(address = address, threadId = threadId))
                } catch (e: SecurityException) {
                    Log.w("SystemSmsReader", "READ_SMS permission not granted", e)
                    trySend(emptyList())
                }
            }
        }

        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                scheduleRead()
            }
        }
        contentResolver.registerContentObserver(
            Telephony.Sms.CONTENT_URI, true, observer
        )
        scheduleRead()
        awaitClose {
            readJob?.cancel()
            contentResolver.unregisterContentObserver(observer)
        }
    }.distinctUntilChanged()

    /**
     * Reads all conversation threads from the SMS provider,
     * grouped by sender address and sorted by most recent first.
     */
    fun readThreads(): List<SmsThread> {
        val threads = linkedMapOf<Long, MutableThreadAccumulator>()

        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            SMS_PROJECTION,
            null, null,
            "${Telephony.Sms.DATE} DESC",
        ) ?: return emptyList()

        cursor.use {
            while (it.moveToNext()) {
                val sms = it.toSystemSms()
                val accumulator = threads.getOrPut(sms.threadId) {
                    MutableThreadAccumulator(
                        threadId = sms.threadId,
                        address = sms.address.normalizeAddressForDisplay(),
                        snippet = sms.body.take(120),
                        date = sms.date,
                    )
                }
                accumulator.messageCount += 1
                if (!sms.read && sms.isInbound) {
                    accumulator.unreadCount += 1
                }
            }
        }

        return threads.values.map { accumulator ->
            SmsThread(
                threadId = accumulator.threadId,
                address = accumulator.address,
                snippet = accumulator.snippet,
                date = accumulator.date,
                messageCount = accumulator.messageCount,
                unreadCount = accumulator.unreadCount,
            )
        }
    }

    /**
     * Reads all messages for a specific address, sorted oldest first.
     */
    fun readMessages(address: String, threadId: Long? = null): List<SystemSms> {
        val normalized = address.normalizeAddressForDisplay()
        val selection = if (threadId != null) {
            "${Telephony.Sms.THREAD_ID} = ?"
        } else {
            null
        }
        val selectionArgs = if (threadId != null) {
            arrayOf(threadId.toString())
        } else {
            null
        }
        val messages = mutableListOf<SystemSms>()

        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            SMS_PROJECTION,
            selection,
            selectionArgs,
            "${Telephony.Sms.DATE} ASC",
        ) ?: return emptyList()

        cursor.use {
            while (it.moveToNext()) {
                val sms = it.toSystemSms()
                if (threadId != null || sms.address.normalizeAddressForDisplay() == normalized) {
                    messages.add(sms)
                }
            }
        }
        return messages
    }

    fun markThreadAsRead(threadId: Long?, address: String) {
        val normalizedAddress = address.normalizeAddressForDisplay()
        val selection = buildList {
            add("${Telephony.Sms.TYPE} = ?")
            add("${Telephony.Sms.READ} = 0")
            if (threadId != null) {
                add("${Telephony.Sms.THREAD_ID} = ?")
            } else {
                add("${Telephony.Sms.ADDRESS} = ?")
            }
        }.joinToString(separator = " AND ")
        val selectionArgs = buildList {
            add(Telephony.Sms.MESSAGE_TYPE_INBOX.toString())
            add(if (threadId != null) threadId.toString() else normalizedAddress)
        }.toTypedArray()
        val values = ContentValues().apply {
            put(Telephony.Sms.READ, 1)
            put(Telephony.Sms.SEEN, 1)
        }
        contentResolver.update(Telephony.Sms.CONTENT_URI, values, selection, selectionArgs)
    }

    /**
     * Sends an SMS and writes it to the system SMS content provider.
     */
    @Suppress("DEPRECATION")
    fun sendSms(address: String, body: String, subscriptionId: Int? = null) {
        val smsManager = if (subscriptionId != null) {
            SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
        } else {
            SmsManager.getDefault()
        }
        val parts = smsManager.divideMessage(body)
        smsManager.sendMultipartTextMessage(address, null, parts, null, null)

        // Write to sent folder
        val values = android.content.ContentValues().apply {
            put(Telephony.Sms.ADDRESS, address)
            put(Telephony.Sms.BODY, body)
            put(Telephony.Sms.DATE, System.currentTimeMillis())
            put(Telephony.Sms.READ, 1)
            put(Telephony.Sms.SEEN, 1)
            put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
        }
        contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
    }

    private fun Cursor.toSystemSms(): SystemSms = SystemSms(
        id = getLong(getColumnIndexOrThrow(Telephony.Sms._ID)),
        address = getString(getColumnIndexOrThrow(Telephony.Sms.ADDRESS)) ?: "Unknown",
        body = getString(getColumnIndexOrThrow(Telephony.Sms.BODY)) ?: "",
        date = getLong(getColumnIndexOrThrow(Telephony.Sms.DATE)),
        type = getInt(getColumnIndexOrThrow(Telephony.Sms.TYPE)),
        read = getInt(getColumnIndexOrThrow(Telephony.Sms.READ)) == 1,
        threadId = getLong(getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)),
    )

    private data class MutableThreadAccumulator(
        val threadId: Long,
        val address: String,
        val snippet: String,
        val date: Long,
        var messageCount: Int = 0,
        var unreadCount: Int = 0,
    )

    companion object {
        private val SMS_PROJECTION = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
            Telephony.Sms.READ,
            Telephony.Sms.THREAD_ID,
        )
    }
}
