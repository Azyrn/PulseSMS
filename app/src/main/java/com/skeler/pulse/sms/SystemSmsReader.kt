package com.skeler.pulse.sms

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.core.content.ContextCompat
import com.skeler.pulse.contact.normalizeAddressForDisplay
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.time.Instant
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
    val isOutbound: Boolean
        get() = type == Telephony.Sms.MESSAGE_TYPE_SENT ||
            type == Telephony.Sms.MESSAGE_TYPE_QUEUED ||
            type == Telephony.Sms.MESSAGE_TYPE_OUTBOX ||
            type == Telephony.Sms.MESSAGE_TYPE_FAILED
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
        setThreadUnreadState(threadId = threadId, address = address, unread = false)
    }

    fun setThreadUnreadState(threadId: Long?, address: String, unread: Boolean) {
        val targetReadValue = if (unread) 1 else 0
        val targetIds = resolveSmsIds(
            threadId = threadId,
            address = address,
            inboundOnly = true,
            readValue = targetReadValue,
        )
        if (targetIds.isEmpty()) return
        val (selection, selectionArgs) = targetIds.toIdSelection()
        val values = ContentValues().apply {
            put(Telephony.Sms.READ, if (unread) 0 else 1)
            put(Telephony.Sms.SEEN, if (unread) 0 else 1)
        }
        contentResolver.update(Telephony.Sms.CONTENT_URI, values, selection, selectionArgs)
    }

    fun deleteThread(threadId: Long?, address: String) {
        val targetIds = resolveSmsIds(threadId = threadId, address = address)
        if (targetIds.isEmpty()) return
        val (selection, selectionArgs) = targetIds.toIdSelection()
        contentResolver.delete(Telephony.Sms.CONTENT_URI, selection, selectionArgs)
    }

    fun deleteMessage(messageId: Long) {
        contentResolver.delete(
            Telephony.Sms.CONTENT_URI,
            "${Telephony.Sms._ID} = ?",
            arrayOf(messageId.toString()),
        )
    }

    private fun resolveSmsIds(
        threadId: Long?,
        address: String,
        inboundOnly: Boolean = false,
        readValue: Int? = null,
    ): List<Long> {
        val normalizedAddress = address.normalizeAddressForDisplay()
        val selection = buildList {
            if (threadId != null) {
                add("${Telephony.Sms.THREAD_ID} = ?")
            }
            if (inboundOnly) {
                add("${Telephony.Sms.TYPE} = ?")
            }
            if (readValue != null) {
                add("${Telephony.Sms.READ} = ?")
            }
        }.joinToString(separator = " AND ").ifBlank { null }
        val selectionArgs = buildList {
            if (threadId != null) {
                add(threadId.toString())
            }
            if (inboundOnly) {
                add(Telephony.Sms.MESSAGE_TYPE_INBOX.toString())
            }
            if (readValue != null) {
                add(readValue.toString())
            }
        }.toTypedArray().ifEmpty { null }

        val ids = mutableListOf<Long>()
        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS),
            selection,
            selectionArgs,
            null,
        ) ?: return emptyList()
        cursor.use {
            while (it.moveToNext()) {
                val rawAddress = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)).orEmpty()
                if (threadId != null || rawAddress.normalizeAddressForDisplay() == normalizedAddress) {
                    ids.add(it.getLong(it.getColumnIndexOrThrow(Telephony.Sms._ID)))
                }
            }
        }
        return ids
    }

    private fun List<Long>.toIdSelection(): Pair<String, Array<String>> =
        "${Telephony.Sms._ID} IN (${joinToString { "?" }})" to map(Long::toString).toTypedArray()

    /**
     * Sends an SMS and writes it to the system SMS content provider.
     */
    @Suppress("DEPRECATION")
    suspend fun sendSms(address: String, body: String, subscriptionId: Int? = null) = withContext(ioDispatcher) {
        val smsManager = if (subscriptionId != null) {
            SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
        } else {
            SmsManager.getDefault()
        }
        val parts = smsManager.divideMessage(body)
        val messageUri = insertOutgoingMessage(address, body, Telephony.Sms.MESSAGE_TYPE_QUEUED)
        try {
            val deliveryIntents = buildDeliveryIntents(address, parts, messageUri)
            awaitSentCallbacks(address, parts, messageUri) { sentIntents ->
                smsManager.sendMultipartTextMessage(address, null, parts, sentIntents, deliveryIntents)
            }
            updateOutgoingMessage(messageUri, Telephony.Sms.MESSAGE_TYPE_SENT)
            awaitDeliveryCallbacks(address, parts, messageUri)
        } catch (exception: Exception) {
            updateOutgoingMessage(messageUri, Telephony.Sms.MESSAGE_TYPE_FAILED)
            throw exception
        }
    }

    private fun insertOutgoingMessage(address: String, body: String, messageType: Int): Uri? {
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, address)
            put(Telephony.Sms.BODY, body)
            put(Telephony.Sms.DATE, System.currentTimeMillis())
            put(Telephony.Sms.READ, 1)
            put(Telephony.Sms.SEEN, 1)
            put(Telephony.Sms.TYPE, messageType)
            put(Telephony.Sms.STATUS, Telephony.Sms.STATUS_PENDING)
        }
        return contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
    }

    private fun updateOutgoingMessage(messageUri: Uri?, messageType: Int) {
        if (messageUri == null) return
        val status = if (messageType == Telephony.Sms.MESSAGE_TYPE_SENT) {
            Telephony.Sms.STATUS_COMPLETE
        } else {
            Telephony.Sms.STATUS_FAILED
        }
        val values = ContentValues().apply {
            put(Telephony.Sms.TYPE, messageType)
            put(Telephony.Sms.STATUS, status)
        }
        contentResolver.update(messageUri, values, null, null)
    }

    private fun updateOutgoingStatus(messageUri: Uri?, status: Int) {
        if (messageUri == null) return
        val values = ContentValues().apply {
            put(Telephony.Sms.STATUS, status)
        }
        contentResolver.update(messageUri, values, null, null)
    }

    private suspend fun awaitSentCallbacks(
        address: String,
        parts: ArrayList<String>,
        messageUri: Uri?,
        send: (ArrayList<PendingIntent>) -> Unit,
    ) = withTimeout(SEND_CALLBACK_TIMEOUT_MILLIS) {
        suspendCancellableCoroutine { continuation ->
            val token = callbackToken(address, messageUri)
            val action = "$ACTION_SMS_SENT.$token"
            val remainingParts = AtomicInteger(parts.size)
            val completed = AtomicBoolean(false)
            val failures = Collections.synchronizedList(mutableListOf<Int>())
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action != action) return
                    if (resultCode != Activity.RESULT_OK) {
                        failures.add(resultCode)
                    }
                    if (remainingParts.decrementAndGet() == 0 && completed.compareAndSet(false, true)) {
                        context.unregisterReceiver(this)
                        if (failures.isEmpty()) {
                            continuation.resume(Unit)
                        } else {
                            continuation.resumeWithException(
                                SmsSendException("SMS send failed with result ${failures.first()}")
                            )
                        }
                    }
                }
            }
            ContextCompat.registerReceiver(
                context,
                receiver,
                IntentFilter(action),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            continuation.invokeOnCancellation {
                if (completed.compareAndSet(false, true)) {
                    runCatching { context.unregisterReceiver(receiver) }
                }
            }
            val sentIntents = buildCallbackIntents(
                action = action,
                token = token,
                parts = parts,
                messageUri = messageUri,
            )
            try {
                send(sentIntents)
            } catch (exception: Exception) {
                if (completed.compareAndSet(false, true)) {
                    runCatching { context.unregisterReceiver(receiver) }
                    continuation.resumeWithException(exception)
                }
            }
        }
    }

    private fun buildDeliveryIntents(
        address: String,
        parts: ArrayList<String>,
        messageUri: Uri?,
    ): ArrayList<PendingIntent> {
        val token = callbackToken(address, messageUri)
        val action = "$ACTION_SMS_DELIVERED.$token"
        return buildCallbackIntents(
            action = action,
            token = token,
            parts = parts,
            messageUri = messageUri,
        )
    }

    private suspend fun awaitDeliveryCallbacks(
        address: String,
        parts: ArrayList<String>,
        messageUri: Uri?,
    ) = runCatching {
        withTimeout(DELIVERY_CALLBACK_TIMEOUT_MILLIS) {
            suspendCancellableCoroutine { continuation ->
                val token = callbackToken(address, messageUri)
                val action = "$ACTION_SMS_DELIVERED.$token"
                val remainingParts = AtomicInteger(parts.size)
                val completed = AtomicBoolean(false)
                val failures = Collections.synchronizedList(mutableListOf<Int>())
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        if (intent.action != action) return
                        if (resultCode != Activity.RESULT_OK) {
                            failures.add(resultCode)
                        }
                        if (remainingParts.decrementAndGet() == 0 && completed.compareAndSet(false, true)) {
                            context.unregisterReceiver(this)
                            if (failures.isEmpty()) {
                                updateOutgoingStatus(messageUri, Telephony.Sms.STATUS_COMPLETE)
                            } else {
                                updateOutgoingStatus(messageUri, Telephony.Sms.STATUS_FAILED)
                            }
                            continuation.resume(Unit)
                        }
                    }
                }
                ContextCompat.registerReceiver(
                    context,
                    receiver,
                    IntentFilter(action),
                    ContextCompat.RECEIVER_NOT_EXPORTED,
                )
                continuation.invokeOnCancellation {
                    if (completed.compareAndSet(false, true)) {
                        runCatching { context.unregisterReceiver(receiver) }
                    }
                }
            }
        }
    }

    private fun callbackToken(address: String, messageUri: Uri?): String =
        "${System.currentTimeMillis()}_${messageUri?.lastPathSegment.orEmpty()}_${address.hashCode()}"

    private fun buildCallbackIntents(
        action: String,
        token: String,
        parts: ArrayList<String>,
        messageUri: Uri?,
    ): ArrayList<PendingIntent> {
        val intents = ArrayList<PendingIntent>(parts.size)
        repeat(parts.size) { index ->
            val intent = Intent(action)
                .setPackage(context.packageName)
                .putExtra(EXTRA_MESSAGE_URI, messageUri?.toString())
                .putExtra(EXTRA_PART_INDEX, index)
                .putExtra(EXTRA_PART_COUNT, parts.size)
            intents.add(
                PendingIntent.getBroadcast(
                    context,
                    (token.hashCode() * 31) + index,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            )
        }
        return intents
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
        private const val ACTION_SMS_SENT = "com.skeler.pulse.sms.SMS_SENT"
        private const val ACTION_SMS_DELIVERED = "com.skeler.pulse.sms.SMS_DELIVERED"
        private const val EXTRA_MESSAGE_URI = "message_uri"
        private const val EXTRA_PART_INDEX = "part_index"
        private const val EXTRA_PART_COUNT = "part_count"
        private const val SEND_CALLBACK_TIMEOUT_MILLIS = 60_000L
        private const val DELIVERY_CALLBACK_TIMEOUT_MILLIS = 180_000L

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

private class SmsSendException(message: String) : Exception(message)
