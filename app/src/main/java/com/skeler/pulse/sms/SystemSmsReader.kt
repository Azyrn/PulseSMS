package com.skeler.pulse.sms

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.telephony.SmsManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.Instant

/**
 * Data class representing a single SMS message from the system content provider.
 */
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
class SystemSmsReader(private val context: Context) {

    private val contentResolver: ContentResolver get() = context.contentResolver

    /**
     * Observes all conversation threads as a reactive [Flow].
     * Emits a new list whenever the SMS content provider changes.
     */
    fun observeThreads(): Flow<List<SmsThread>> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(readThreads())
            }
        }
        contentResolver.registerContentObserver(
            Telephony.Sms.CONTENT_URI, true, observer
        )
        // Initial emission
        trySend(readThreads())
        awaitClose { contentResolver.unregisterContentObserver(observer) }
    }.distinctUntilChanged()

    /**
     * Observes messages for a specific address/thread as a reactive [Flow].
     */
    fun observeMessages(address: String): Flow<List<SystemSms>> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(readMessages(address))
            }
        }
        contentResolver.registerContentObserver(
            Telephony.Sms.CONTENT_URI, true, observer
        )
        trySend(readMessages(address))
        awaitClose { contentResolver.unregisterContentObserver(observer) }
    }.distinctUntilChanged()

    /**
     * Reads all conversation threads from the SMS provider,
     * grouped by sender address and sorted by most recent first.
     */
    fun readThreads(): List<SmsThread> {
        val threads = mutableMapOf<String, MutableList<SystemSms>>()

        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            SMS_PROJECTION,
            null, null,
            "${Telephony.Sms.DATE} DESC",
        ) ?: return emptyList()

        cursor.use {
            while (it.moveToNext()) {
                val sms = it.toSystemSms()
                val key = sms.address.normalizeAddress()
                threads.getOrPut(key) { mutableListOf() }.add(sms)
            }
        }

        return threads.map { (address, messages) ->
            val latest = messages.first()
            SmsThread(
                threadId = latest.threadId,
                address = address,
                snippet = latest.body.take(120),
                date = latest.date,
                messageCount = messages.size,
                unreadCount = messages.count { !it.read && it.isInbound },
            )
        }.sortedByDescending { it.date }
    }

    /**
     * Reads all messages for a specific address, sorted oldest first.
     */
    fun readMessages(address: String): List<SystemSms> {
        val normalized = address.normalizeAddress()
        val allMessages = mutableListOf<SystemSms>()

        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            SMS_PROJECTION,
            null, null,
            "${Telephony.Sms.DATE} ASC",
        ) ?: return emptyList()

        cursor.use {
            while (it.moveToNext()) {
                val sms = it.toSystemSms()
                if (sms.address.normalizeAddress() == normalized) {
                    allMessages.add(sms)
                }
            }
        }
        return allMessages
    }

    /**
     * Sends an SMS and writes it to the system SMS content provider.
     */
    @Suppress("DEPRECATION")
    fun sendSms(address: String, body: String) {
        val smsManager = SmsManager.getDefault()
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

    /**
     * Normalizes phone numbers for grouping (strips non-digit except +).
     */
    private fun String.normalizeAddress(): String {
        // For short codes / alphanumeric senders, keep as-is
        if (this.any { it.isLetter() }) return this.trim()
        // For phone numbers, strip formatting
        return this.filter { it.isDigit() || it == '+' }
    }

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
