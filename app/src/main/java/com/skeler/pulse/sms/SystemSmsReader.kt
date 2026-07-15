package com.skeler.pulse.sms

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Log
import com.skeler.pulse.contact.normalizeAddressForDisplay
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

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
    private val smsSender = SystemSmsSender(context = context, ioDispatcher = ioDispatcher)

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
    }.conflate().distinctUntilChanged()

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
    }.conflate().distinctUntilChanged()

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
            val columns = SmsCursorColumns(it)
            while (it.moveToNext()) {
                val address = it.getString(columns.address) ?: "Unknown"
                val resolvedThreadId = it.getLong(columns.threadId).let { providerThreadId ->
                    if (providerThreadId > 0L) providerThreadId else syntheticThreadId(address)
                }
                val accumulator = threads.getOrPut(resolvedThreadId) {
                    MutableThreadAccumulator(
                        threadId = resolvedThreadId,
                        address = address.normalizeAddressForDisplay(),
                        snippet = (it.getString(columns.body) ?: "").take(120),
                        date = it.getLong(columns.date),
                    )
                }
                accumulator.messageCount += 1
                val isUnread = it.getInt(columns.read) != 1
                val isInbound = it.getInt(columns.type) == Telephony.Sms.MESSAGE_TYPE_INBOX
                if (isUnread && isInbound) {
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
        val criteria = messageReadCriteria(threadId = threadId, address = address)
        val exactMessages = readMessages(criteria = criteria, normalizedAddress = normalized)
        if (exactMessages.isNotEmpty() || threadId.isProviderThreadId()) return exactMessages

        val fullScanCriteria = SmsProviderCriteria(
            selection = null,
            selectionArgs = emptyArray(),
            shouldFilterByAddress = true,
        )
        return readMessages(criteria = fullScanCriteria, normalizedAddress = normalized)
    }

    private fun readMessages(criteria: SmsProviderCriteria, normalizedAddress: String): List<SystemSms> {
        val messages = mutableListOf<SystemSms>()

        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            SMS_PROJECTION,
            criteria.selection,
            criteria.selectionArgs.ifEmpty { null },
            "${Telephony.Sms.DATE} ASC",
        ) ?: return emptyList()

        cursor.use {
            val columns = SmsCursorColumns(it)
            while (it.moveToNext()) {
                val sms = it.toSystemSms(columns)
                if (!criteria.shouldFilterByAddress || sms.address.normalizeAddressForDisplay() == normalizedAddress) {
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
        val criteria = smsIdCriteria(
            threadId = threadId,
            address = address,
            inboundOnly = inboundOnly,
            readValue = readValue,
        )
        val exactIds = resolveSmsIds(criteria = criteria, normalizedAddress = normalizedAddress)
        if (exactIds.isNotEmpty() || threadId.isProviderThreadId()) return exactIds

        val fullScanCriteria = SmsProviderCriteria(
            selection = buildList {
                if (inboundOnly) add("${Telephony.Sms.TYPE} = ?")
                if (readValue != null) add("${Telephony.Sms.READ} = ?")
            }.joinToString(separator = " AND ").ifBlank { null },
            selectionArgs = buildList {
                if (inboundOnly) add(Telephony.Sms.MESSAGE_TYPE_INBOX.toString())
                if (readValue != null) add(readValue.toString())
            }.toTypedArray(),
            shouldFilterByAddress = true,
        )
        return resolveSmsIds(criteria = fullScanCriteria, normalizedAddress = normalizedAddress)
    }

    private fun resolveSmsIds(criteria: SmsProviderCriteria, normalizedAddress: String): List<Long> {
        val ids = mutableListOf<Long>()
        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS),
            criteria.selection,
            criteria.selectionArgs.ifEmpty { null },
            null,
        ) ?: return emptyList()
        cursor.use {
            val idColumn = it.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addressColumn = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            while (it.moveToNext()) {
                val rawAddress = it.getString(addressColumn).orEmpty()
                if (!criteria.shouldFilterByAddress || rawAddress.normalizeAddressForDisplay() == normalizedAddress) {
                    ids.add(it.getLong(idColumn))
                }
            }
        }
        return ids
    }

    private fun List<Long>.toIdSelection(): Pair<String, Array<String>> =
        "${Telephony.Sms._ID} IN (${joinToString { "?" }})" to map(Long::toString).toTypedArray()

    suspend fun sendSms(address: String, body: String, subscriptionId: Int? = null) =
        smsSender.sendSms(address = address, body = body, subscriptionId = subscriptionId)

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
