package com.skeler.pulse.sms

import android.database.Cursor
import android.provider.Telephony
import com.skeler.pulse.contact.normalizeAddressForDisplay

internal data class SmsProviderCriteria(
    val selection: String?,
    val selectionArgs: Array<String>,
    val shouldFilterByAddress: Boolean,
)

internal fun messageReadCriteria(threadId: Long?, address: String): SmsProviderCriteria =
    if (threadId.isProviderThreadId()) {
        SmsProviderCriteria(
            selection = "${Telephony.Sms.THREAD_ID} = ?",
            selectionArgs = arrayOf(threadId.toString()),
            shouldFilterByAddress = false,
        )
    } else {
        SmsProviderCriteria(
            selection = "${Telephony.Sms.ADDRESS} = ?",
            selectionArgs = arrayOf(address),
            shouldFilterByAddress = true,
        )
    }

internal fun smsIdCriteria(
    threadId: Long?,
    address: String,
    inboundOnly: Boolean,
    readValue: Int?,
): SmsProviderCriteria {
    val selectionParts = buildList {
        if (threadId.isProviderThreadId()) {
            add("${Telephony.Sms.THREAD_ID} = ?")
        } else {
            add("${Telephony.Sms.ADDRESS} = ?")
        }
        if (inboundOnly) add("${Telephony.Sms.TYPE} = ?")
        if (readValue != null) add("${Telephony.Sms.READ} = ?")
    }
    val selectionArgs = buildList {
        if (threadId.isProviderThreadId()) {
            add(threadId.toString())
        } else {
            add(address)
        }
        if (inboundOnly) add(Telephony.Sms.MESSAGE_TYPE_INBOX.toString())
        if (readValue != null) add(readValue.toString())
    }
    return SmsProviderCriteria(
        selection = selectionParts.joinToString(separator = " AND "),
        selectionArgs = selectionArgs.toTypedArray(),
        shouldFilterByAddress = !threadId.isProviderThreadId(),
    )
}

internal fun Long?.isProviderThreadId(): Boolean = this != null && this > 0L

internal fun syntheticThreadId(address: String): Long {
    val normalizedAddress = address.normalizeAddressForDisplay()
    val unsignedHash = normalizedAddress.hashCode().toLong() and 0xffffffffL
    return -1L - unsignedHash
}

internal class SmsCursorColumns(cursor: Cursor) {
    val id: Int = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
    val address: Int = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
    val body: Int = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
    val date: Int = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
    val type: Int = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)
    val read: Int = cursor.getColumnIndexOrThrow(Telephony.Sms.READ)
    val threadId: Int = cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
}

internal fun Cursor.toSystemSms(columns: SmsCursorColumns): SystemSms = SystemSms(
    id = getLong(columns.id),
    address = getString(columns.address) ?: "Unknown",
    body = getString(columns.body) ?: "",
    date = getLong(columns.date),
    type = getInt(columns.type),
    read = getInt(columns.read) == 1,
    threadId = getLong(columns.threadId),
)

internal fun SystemSms.resolvedThreadId(): Long =
    if (threadId > 0L) threadId else syntheticThreadId(address)

internal class MutableThreadAccumulator(
    val threadId: Long,
    val address: String,
    val snippet: String,
    val date: Long,
    var messageCount: Int = 0,
    var unreadCount: Int = 0,
)
