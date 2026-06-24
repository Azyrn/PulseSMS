package com.skeler.pulse.sms

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.BaseColumns
import android.provider.Telephony
import android.util.Log
import com.skeler.pulse.R
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
import kotlinx.coroutines.withContext

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
        contentResolver.registerContentObserver(
            Telephony.Mms.CONTENT_URI, true, observer
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
     * @param maxCount Maximum number of recent messages to observe (default: no limit).
     */
    fun observeMessages(address: String, threadId: Long? = null, maxCount: Int = Int.MAX_VALUE): Flow<Pair<List<SystemSms>, Int>> = callbackFlow {
        var readJob: Job? = null
        fun scheduleRead() {
            readJob?.cancel()
            readJob = launch(ioDispatcher) {
                try {
                    val smsMessages = readMessages(address = address, threadId = threadId, limit = maxCount)
                    val mmsMessages = readMmsMessages(threadId = threadId, address = address, limit = maxCount)
                    val total = countConversationMessages(address, threadId)
                    trySend(mergeMessages(smsMessages, mmsMessages) to total)
                } catch (e: SecurityException) {
                    Log.w("SystemSmsReader", "READ_SMS permission not granted", e)
                    trySend(emptyList<SystemSms>() to 0)
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
        contentResolver.registerContentObserver(
            Telephony.Mms.CONTENT_URI, true, observer
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

        // Read SMS threads
        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            SMS_PROJECTION,
            null, null,
            "${Telephony.Sms.DATE} DESC",
        )
        cursor?.use {
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
                        snippet = it.getString(columns.body) ?: "",
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

        // Read MMS threads and merge
        try {
            val mmsCursor = contentResolver.query(
                Telephony.Mms.CONTENT_URI,
                arrayOf("_id", "date", "read", "msg_box", "thread_id"),
                "msg_box IN (${MMS_READ_BOXES.joinToString(", ") { "?" }})",
                MMS_READ_BOXES.map { it.toString() }.toTypedArray(),
                "date DESC",
            )
            mmsCursor?.use { c ->
                val idIdx = c.getColumnIndexOrThrow("_id")
                val dateIdx = c.getColumnIndexOrThrow("date")
                val readIdx = c.getColumnIndexOrThrow("read")
                val msgBoxIdx = c.getColumnIndexOrThrow("msg_box")
                val threadIdIdx = c.getColumnIndexOrThrow("thread_id")
                while (c.moveToNext()) {
                    val providerThreadId = c.getLong(threadIdIdx)
                    val mmsId = c.getLong(idIdx)
                    val dateSecs = c.getLong(dateIdx)
                    val isRead = c.getInt(readIdx) == 1
                    val dateMs = dateSecs * 1000L
                    if (providerThreadId <= 0L) continue
                    val mmsMsgBox = c.getInt(msgBoxIdx)
                    val accumulator = threads.getOrPut(providerThreadId) {
                        val addr = try {
                            val resolved = MmsAddressResolver.resolveAddress(context, mmsId, mmsMsgBox)
                            if (resolved.isNotEmpty() && MyPhoneNumberProvider.isMyNumber(context, resolved)) {
                                val (from, to) = MmsAddressResolver.resolveBothAddresses(context, mmsId)
                                val other = if (from == resolved) to else from
                                if (other.isNotEmpty()) other else resolved
                            } else {
                                resolved
                            }.ifEmpty { "Unknown" }
                        } catch (e: SecurityException) {
                            "Unknown"
                        }
                        val (partUri, partContentType) = MmsPartResolver.resolveFirstAttachmentInfo(context, mmsId)?.let { it.first to it.second } ?: (null to null)
                        MutableThreadAccumulator(
                            threadId = providerThreadId,
                            address = addr.normalizeAddressForDisplay(),
                            snippet = if (partUri != null) "" else context.getString(R.string.mms_body_placeholder),
                            date = dateMs,
                            lastMmsPartUri = partUri,
                            lastMmsContentType = partContentType,
                        )
                    }
                    accumulator.messageCount += 1
                    val isInbound = c.getInt(msgBoxIdx) == Telephony.Mms.MESSAGE_BOX_INBOX
                    if (!isRead && isInbound) accumulator.unreadCount += 1
                    if (dateMs > accumulator.date) {
                        accumulator.date = dateMs
                        val (newPartUri, newPartContentType) = MmsPartResolver.resolveFirstAttachmentInfo(context, mmsId)?.let { it.first to it.second } ?: (null to null)
                        accumulator.snippet = if (newPartUri != null) "" else context.getString(R.string.mms_body_placeholder)
                        accumulator.lastMmsPartUri = newPartUri
                        accumulator.lastMmsContentType = newPartContentType
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Cannot read MMS for thread listing", e)
        }

        val merged = threads.values
            .groupBy { it.address }
            .values
            .map { group ->
                val latest = group.maxBy { it.date }
                SmsThread(
                    threadId = group.firstOrNull { it.threadId > 0L }?.threadId
                        ?: group.first().threadId,
                    address = latest.address,
                    snippet = latest.snippet,
                    date = latest.date,
                    messageCount = group.sumOf { it.messageCount },
                    unreadCount = group.sumOf { it.unreadCount },
                    lastMmsPartUri = latest.lastMmsPartUri,
                    lastMmsContentType = latest.lastMmsContentType,
                )
            }
            .sortedByDescending { it.date }

        return merged
    }

    /**
     * Reads messages for a specific address, sorted oldest first.
     * @param limit Maximum number of recent messages to return (default: no limit).
     */
    fun readMessages(address: String, threadId: Long? = null, limit: Int = Int.MAX_VALUE): List<SystemSms> {
        val normalized = address.normalizeAddressForDisplay()
        val criteria = messageReadCriteria(threadId = threadId, address = address)
        val exactMessages = readMessages(criteria = criteria, normalizedAddress = normalized, limit = limit)
        if (exactMessages.isNotEmpty() || threadId.isProviderThreadId()) return exactMessages

        val fullScanCriteria = SmsProviderCriteria(
            selection = null,
            selectionArgs = emptyArray(),
            shouldFilterByAddress = true,
        )
        return readMessages(criteria = fullScanCriteria, normalizedAddress = normalized, limit = limit)
    }

    private fun readMessages(
        criteria: SmsProviderCriteria,
        normalizedAddress: String,
        limit: Int = Int.MAX_VALUE,
        beforeDate: Long? = null,
    ): List<SystemSms> {
        val messages = mutableListOf<SystemSms>()
        val hasLimit = limit < Int.MAX_VALUE

        val clauses = mutableListOf<String>()
        criteria.selection?.let { clauses.add(it) }
        if (beforeDate != null) {
            clauses.add("${Telephony.Sms.DATE} < ?")
        }
        val selection = clauses.joinToString(" AND ").ifBlank { null }

        val args = mutableListOf<String>()
        criteria.selectionArgs.forEach { args.add(it) }
        if (beforeDate != null) {
            args.add(beforeDate.toString())
        }
        val selectionArgs = args.toTypedArray()

        val sortOrder = if (hasLimit || beforeDate != null) {
            "${Telephony.Sms.DATE} DESC" + if (hasLimit) " LIMIT $limit" else ""
        } else {
            "${Telephony.Sms.DATE} ASC"
        }

        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            SMS_PROJECTION,
            selection,
            selectionArgs.ifEmpty { null },
            sortOrder,
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

        if (hasLimit || beforeDate != null) {
            messages.reverse()
        }

        return messages
    }

    /**
     * Reads older messages before a given date for pagination.
     * @param beforeDate Cursor date in millis — only messages strictly older than this are returned.
     * @param limit Maximum number of messages to return.
     */
    fun readOlderMessages(address: String, threadId: Long? = null, beforeDate: Long, limit: Int = DEFAULT_MESSAGE_LIMIT): List<SystemSms> {
        val normalized = address.normalizeAddressForDisplay()
        val criteria = messageReadCriteria(threadId = threadId, address = address)
        val smsMessages = readMessages(criteria = criteria, normalizedAddress = normalized, beforeDate = beforeDate, limit = limit)
        val mmsMessages = readMmsMessages(threadId = threadId, address = address, beforeDate = beforeDate, limit = limit)
        val merged = mergeMessages(smsMessages, mmsMessages)
        if (merged.isNotEmpty() || threadId.isProviderThreadId()) return merged

        val fullScanCriteria = SmsProviderCriteria(
            selection = null,
            selectionArgs = emptyArray(),
            shouldFilterByAddress = true,
        )
        val fullSms = readMessages(criteria = fullScanCriteria, normalizedAddress = normalized, beforeDate = beforeDate, limit = limit)
        val fullMms = readMmsMessages(threadId = null, address = address, beforeDate = beforeDate, limit = limit)
        return mergeMessages(fullSms, fullMms)
    }

    private fun readMmsMessages(
        threadId: Long?,
        address: String,
        limit: Int = Int.MAX_VALUE,
        beforeDate: Long? = null,
    ): List<SystemSms> {
        val resolvedThreadId = threadId ?: runCatching {
            Telephony.Threads.getOrCreateThreadId(context, address).takeIf { it > 0L }
        }.getOrNull()
        if (resolvedThreadId == null || resolvedThreadId <= 0L) return emptyList()

        val hasLimit = limit < Int.MAX_VALUE
        val placeholder = MMS_READ_BOXES.joinToString(", ") { "?" }
        val clauses = mutableListOf("thread_id = ?", "msg_box IN ($placeholder)")
        if (beforeDate != null) clauses.add("date < ?")
        val selection = clauses.joinToString(" AND ")

        val selectionArgs = mutableListOf(resolvedThreadId.toString()).apply {
            addAll(MMS_READ_BOXES.map { it.toString() })
        }
        if (beforeDate != null) selectionArgs.add((beforeDate / 1000).toString())

        val sortOrder = if (hasLimit || beforeDate != null) {
            "date DESC" + if (hasLimit) " LIMIT $limit" else ""
        } else {
            "date ASC"
        }

        val cursor = try {
            contentResolver.query(
                Telephony.Mms.CONTENT_URI,
                arrayOf("_id", "date", "read", "msg_box", "thread_id", "st", "resp_st"),
                selection,
                selectionArgs.toTypedArray(),
                sortOrder,
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "MMS read requires READ_MMS permission", e)
            return emptyList()
        } ?: return emptyList()

        return cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow("_id")
            val dateIdx = c.getColumnIndexOrThrow("date")
            val readIdx = c.getColumnIndexOrThrow("read")
            val msgBoxIdx = c.getColumnIndexOrThrow("msg_box")
            val stIdx = c.getColumnIndex("st")
            val respStIdx = c.getColumnIndex("resp_st")

            // Resolve the other-party address once — all MMS in this thread share
            // the same contact address regardless of direction (sent vs received)
            val (resolvedAddress, fromAddress, toAddress) = if (c.moveToFirst()) {
                val firstMmsId = c.getLong(idIdx)
                val firstMsgBox = if (c.getColumnIndex("msg_box") >= 0) c.getInt(msgBoxIdx) else null
                try {
                    val ra = MmsAddressResolver.resolveAddress(context, firstMmsId, firstMsgBox)
                        .ifEmpty { address }
                    MmsAddressResolver.resolveBothAddresses(context, firstMmsId)
                        .let { (fa, ta) ->
                            Triple(ra, fa.ifEmpty { null }, ta.ifEmpty { null })
                        }
                } catch (e: SecurityException) {
                    Triple(address, null, null)
                }.also { c.moveToPosition(-1) }
            } else {
                return@use emptyList()
            }

            val messages = mutableListOf<SystemSms>()
            while (c.moveToNext()) {
                val mmsId = c.getLong(idIdx)
                val dateSecs = c.getLong(dateIdx)
                val readInt = c.getInt(readIdx)
                val msgBox = c.getInt(msgBoxIdx)

                val partsResult = try {
                    MmsPartResolver.resolveParts(context, mmsId)
                } catch (e: SecurityException) {
                    MmsPartResolver.MmsPartsResult(null, null)
                }

                val body = partsResult.textBody.orEmpty()
                val partUri = partsResult.attachmentUri.also { uri ->
                    if (uri != null) Log.i(TAG, "MMS part URI: $uri for mmsId=$mmsId")
                }

                val smsType = when (msgBox) {
                    Telephony.Mms.MESSAGE_BOX_INBOX -> Telephony.Sms.MESSAGE_TYPE_INBOX
                    Telephony.Mms.MESSAGE_BOX_OUTBOX -> Telephony.Sms.MESSAGE_TYPE_OUTBOX
                    Telephony.Mms.MESSAGE_BOX_FAILED -> Telephony.Sms.MESSAGE_TYPE_FAILED
                    else -> Telephony.Sms.MESSAGE_TYPE_SENT
                }

                val st = stIdx.takeIf { it >= 0 }?.let { c.getInt(it) }
                val respSt = respStIdx.takeIf { it >= 0 }?.let { c.getInt(it) }
                val status = when {
                    respSt == 128 -> Telephony.Sms.STATUS_COMPLETE
                    respSt != null && respSt > 128 -> Telephony.Sms.STATUS_FAILED
                    st == 129 -> Telephony.Sms.STATUS_FAILED
                    st != null && st >= 128 -> Telephony.Sms.STATUS_COMPLETE
                    else -> Telephony.Sms.STATUS_NONE
                }

                messages.add(
                    SystemSms(
                        id = mmsId,
                        isMms = true,
                        address = resolvedAddress,
                        body = if (body.isEmpty() && partUri != null) "" else body.ifEmpty { context.getString(R.string.mms_body_placeholder) },
                        date = dateSecs * 1000L,
                        type = smsType,
                        read = readInt == 1,
                        threadId = resolvedThreadId,
                        mmsPartUri = partUri,
                        mmsContentType = partsResult.attachmentMimeType,
                        status = status,
                        fromAddress = fromAddress,
                        toAddress = toAddress,
                    ),
                )
            }

            if (hasLimit || beforeDate != null) {
                messages.reverse()
            }

            messages
        }
    }

    private fun mergeMessages(sms: List<SystemSms>, mms: List<SystemSms>): List<SystemSms> {
        if (mms.isEmpty()) return sms
        if (sms.isEmpty()) return mms
        // Both lists are pre-sorted by date ASC — linear merge O(N+M)
        val result = ArrayList<SystemSms>(sms.size + mms.size)
        var i = 0
        var j = 0
        while (i < sms.size && j < mms.size) {
            if (sms[i].date <= mms[j].date) result.add(sms[i++])
            else result.add(mms[j++])
        }
        while (i < sms.size) result.add(sms[i++])
        while (j < mms.size) result.add(mms[j++])
        return result
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
        if (targetIds.isNotEmpty()) {
            val (selection, selectionArgs) = targetIds.toIdSelection(Telephony.Sms._ID)
            val values = ContentValues().apply {
                put(Telephony.Sms.READ, if (unread) 0 else 1)
                put(Telephony.Sms.SEEN, if (unread) 0 else 1)
            }
            contentResolver.update(Telephony.Sms.CONTENT_URI, values, selection, selectionArgs)
        }
        if (threadId != null && threadId > 0L) {
            val mmsValues = ContentValues().apply {
                put("read", if (unread) 0 else 1)
            }
            contentResolver.update(
                Telephony.Mms.CONTENT_URI,
                mmsValues,
                "thread_id = ? AND read = ?",
                arrayOf(threadId.toString(), targetReadValue.toString()),
            )
        }
    }

    fun deleteThread(threadId: Long?, address: String) {
        // Delete MMS first (before SMS observer fires) to avoid race where readThreads
        // sees MMS data after SMS is already gone
        if (threadId != null && threadId > 0L) {
            val mmsCursor = contentResolver.query(
                Telephony.Mms.CONTENT_URI,
                arrayOf("_id"),
                "thread_id = ?",
                arrayOf(threadId.toString()),
                null,
            )
            mmsCursor?.use { c ->
                val idIdx = c.getColumnIndexOrThrow("_id")
                while (c.moveToNext()) {
                    val mmsId = c.getLong(idIdx)
                    contentResolver.delete(
                        Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, mmsId.toString()),
                        null, null,
                    )
                    deleteMmsPartCacheFile(mmsId)
                }
            }
        }
        val targetIds = resolveSmsIds(threadId = threadId, address = address)
        if (targetIds.isNotEmpty()) {
            val (selection, selectionArgs) = targetIds.toIdSelection(Telephony.Sms._ID)
            contentResolver.delete(Telephony.Sms.CONTENT_URI, selection, selectionArgs)
        }
        // Force re-read by notifying both URIs (some ROMs don't notify on MMS delete)
        contentResolver.notifyChange(Telephony.Mms.CONTENT_URI, null)
    }

    private fun deleteMmsPartCacheFile(mmsId: Long) {
        val file = java.io.File(context.cacheDir, "mms_parts/$mmsId")
        if (file.exists()) file.delete()
    }

    fun deleteMessage(messageId: Long) {
        contentResolver.delete(
            Telephony.Sms.CONTENT_URI,
            "${Telephony.Sms._ID} = ?",
            arrayOf(messageId.toString()),
        )
    }

    fun deleteMessages(messages: List<SystemSms>) {
        if (messages.isEmpty()) return
        val smsIds = messages.filter { !it.isMms }.map { it.id }
        val mmsIds = messages.filter { it.isMms }.map { it.id }
        if (smsIds.isNotEmpty()) {
            val (selection, selectionArgs) = smsIds.toIdSelection(Telephony.Sms._ID)
            contentResolver.delete(Telephony.Sms.CONTENT_URI, selection, selectionArgs)
        }
        if (mmsIds.isNotEmpty()) {
            for (mmsId in mmsIds) {
                contentResolver.delete(
                    Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, mmsId.toString()),
                    null, null,
                )
                deleteMmsPartCacheFile(mmsId)
            }
            contentResolver.notifyChange(Telephony.Mms.CONTENT_URI, null)
        }
    }

    suspend fun cleanupMessages(
        maxSmsPerThread: Int,
        maxMmsPerThread: Int,
        importantMessageIds: Set<Long>,
    ): Int = withContext(ioDispatcher) {
        var totalDeleted = 0

        if (maxSmsPerThread >= 0) {
            runCatching { getSmsThreadIds() }.getOrDefault(emptyList()).forEach { threadId ->
                runCatching {
                    val toDelete = getSmsIdsForCleanup(threadId, maxSmsPerThread, importantMessageIds)
                    if (toDelete.isNotEmpty()) {
                        toDelete.chunked(500).forEach { batch ->
                            val (selection, selectionArgs) = batch.toIdSelection(Telephony.Sms._ID)
                            contentResolver.delete(Telephony.Sms.CONTENT_URI, selection, selectionArgs)
                        }
                        totalDeleted += toDelete.size
                    }
                }
            }
        }

        if (maxMmsPerThread >= 0) {
            runCatching { getMmsThreadIds() }.getOrDefault(emptyList()).forEach { threadId ->
                runCatching {
                    val toDelete = getMmsIdsForCleanup(threadId, maxMmsPerThread, importantMessageIds)
                    if (toDelete.isNotEmpty()) {
                        for (mmsId in toDelete) {
                            contentResolver.delete(
                                Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, mmsId.toString()),
                                null, null,
                            )
                            deleteMmsPartCacheFile(mmsId)
                        }
                        contentResolver.notifyChange(Telephony.Mms.CONTENT_URI, null)
                        totalDeleted += toDelete.size
                    }
                }
            }
        }

        totalDeleted
    }

    private fun getSmsThreadIds(): List<Long> {
        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms.THREAD_ID),
            "${Telephony.Sms.THREAD_ID} > ?",
            arrayOf("0"),
            "${Telephony.Sms.THREAD_ID} ASC",
        ) ?: return emptyList()
        cursor.use {
            val idx = it.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val ids = mutableSetOf<Long>()
            while (it.moveToNext()) {
                ids.add(it.getLong(idx))
            }
            return ids.toList()
        }
    }

    private fun getMmsThreadIds(): List<Long> {
        val cursor = try {
            contentResolver.query(
                Telephony.Mms.CONTENT_URI,
                arrayOf("thread_id"),
                "thread_id > ? AND msg_box IN (${MMS_READ_BOXES.joinToString(", ") { "?" }})",
                arrayOf("0") + MMS_READ_BOXES.map { it.toString() }.toTypedArray(),
                "thread_id ASC",
            )
        } catch (e: SecurityException) {
            return emptyList()
        } ?: return emptyList()
        cursor.use {
            val idx = it.getColumnIndexOrThrow("thread_id")
            val ids = mutableSetOf<Long>()
            while (it.moveToNext()) {
                ids.add(it.getLong(idx))
            }
            return ids.toList()
        }
    }

    private fun getSmsIdsForCleanup(threadId: Long, keepCount: Int, importantIds: Set<Long>): List<Long> {
        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms._ID),
            "${Telephony.Sms.THREAD_ID} = ?",
            arrayOf(threadId.toString()),
            "${Telephony.Sms.DATE} ASC",
        ) ?: return emptyList()
        cursor.use {
            val idIdx = it.getColumnIndexOrThrow(Telephony.Sms._ID)
            val count = it.count
            val toDeleteCount = count - keepCount
            if (toDeleteCount <= 0) return emptyList()
            val ids = mutableListOf<Long>()
            var index = 0
            it.moveToPosition(-1)
            while (it.moveToNext()) {
                if (index >= toDeleteCount) break
                val id = it.getLong(idIdx)
                if (id !in importantIds) {
                    ids.add(id)
                }
                index++
            }
            return ids
        }
    }

    private fun getMmsIdsForCleanup(threadId: Long, keepCount: Int, importantIds: Set<Long>): List<Long> {
        val cursor = try {
            contentResolver.query(
                Telephony.Mms.CONTENT_URI,
                arrayOf("_id"),
                "thread_id = ? AND msg_box IN (${MMS_READ_BOXES.joinToString(", ") { "?" }})",
                arrayOf(threadId.toString()) + MMS_READ_BOXES.map { it.toString() }.toTypedArray(),
                "date ASC",
            )
        } catch (e: SecurityException) {
            return emptyList()
        } ?: return emptyList()
        cursor.use {
            val idIdx = it.getColumnIndexOrThrow("_id")
            val count = it.count
            val toDeleteCount = count - keepCount
            if (toDeleteCount <= 0) return emptyList()
            val ids = mutableListOf<Long>()
            var index = 0
            it.moveToPosition(-1)
            while (it.moveToNext()) {
                if (index >= toDeleteCount) break
                val id = it.getLong(idIdx)
                if (id !in importantIds) {
                    ids.add(id)
                }
                index++
            }
            return ids
        }
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

    private fun List<Long>.toIdSelection(idColumn: String): Pair<String, Array<String>> =
        "$idColumn IN (${joinToString { "?" }})" to map(Long::toString).toTypedArray()

    suspend fun sendSms(address: String, body: String, subscriptionId: Int? = null, waitForDelivery: Boolean = true, encryptedBody: String? = null) =
        smsSender.sendSms(address = address, body = body, subscriptionId = subscriptionId, waitForDelivery = waitForDelivery, encryptedBody = encryptedBody)

    suspend fun sendMms(address: String, text: String, imageUris: List<Uri> = emptyList()) =
        smsSender.sendMms(address = address, text = text, imageUris = imageUris)

    suspend fun sendVoiceMms(address: String, text: String, audioUri: Uri) =
        smsSender.sendVoiceMms(address = address, text = text, audioUri = audioUri)

    fun countConversationMessages(address: String, threadId: Long?): Int {
        val normalized = address.normalizeAddressForDisplay()
        val criteria = messageReadCriteria(threadId, address)
        var count = countSms(criteria, normalized)
        if (count == 0 && !threadId.isProviderThreadId()) {
            val fullScan = SmsProviderCriteria(null, emptyArray(), true)
            count = countSms(fullScan, normalized)
        }
        count += countMms(threadId, address)
        return count
    }

    private fun countSms(criteria: SmsProviderCriteria, normalizedAddress: String): Int {
        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS),
            criteria.selection,
            criteria.selectionArgs.ifEmpty { null },
            null,
        ) ?: return 0
        cursor.use {
            val addressIdx = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            var count = 0
            while (it.moveToNext()) {
                val rawAddress = it.getString(addressIdx).orEmpty()
                if (!criteria.shouldFilterByAddress || rawAddress.normalizeAddressForDisplay() == normalizedAddress) {
                    count++
                }
            }
            return count
        }
    }

    private fun countMms(threadId: Long?, address: String): Int {
        val resolvedThreadId = threadId ?: runCatching {
            Telephony.Threads.getOrCreateThreadId(context, address).takeIf { it > 0L }
        }.getOrNull()
        if (resolvedThreadId == null || resolvedThreadId <= 0L) return 0
        val placeholder = MMS_READ_BOXES.joinToString(", ") { "?" }
        val cursor = try {
            contentResolver.query(
                Telephony.Mms.CONTENT_URI,
                arrayOf("_id"),
                "thread_id = ? AND msg_box IN ($placeholder)",
                arrayOf(resolvedThreadId.toString()).plus(MMS_READ_BOXES.map { it.toString() }.toTypedArray()),
                null,
            )
        } catch (e: SecurityException) {
            return 0
        } ?: return 0
        return cursor.use { it.count }
    }

    companion object {
        private const val TAG = "SystemSmsReader"
        internal const val DEFAULT_MESSAGE_LIMIT = 200

        private val MMS_READ_BOXES = listOf(
            Telephony.Mms.MESSAGE_BOX_INBOX,
            Telephony.Mms.MESSAGE_BOX_OUTBOX,
            Telephony.Mms.MESSAGE_BOX_SENT,
            Telephony.Mms.MESSAGE_BOX_FAILED,
        )
        private val SMS_PROJECTION = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
            Telephony.Sms.READ,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.STATUS,
            "priority",
            Telephony.Sms.DATE_SENT,
        )
    }
}
