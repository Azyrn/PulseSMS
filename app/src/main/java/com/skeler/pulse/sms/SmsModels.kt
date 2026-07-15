package com.skeler.pulse.sms

import android.provider.Telephony
import androidx.compose.runtime.Immutable
import java.time.Instant

internal const val CALLBACK_REQUEST_CODE_MULTIPLIER = 31L
internal const val CALLBACK_REQUEST_CODE_MODULUS = 2_147_483_648L

internal fun callbackRequestCode(token: String, partIndex: Int): Int {
    val rawRequestCode = token.hashCode().toLong() * CALLBACK_REQUEST_CODE_MULTIPLIER + partIndex
    return Math.floorMod(rawRequestCode, CALLBACK_REQUEST_CODE_MODULUS).toInt()
}

@Immutable
data class SystemSms(
    val id: Long,
    val address: String,
    val body: String,
    val date: Long,
    val type: Int,
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

internal class SmsSendException(message: String) : Exception(message)
