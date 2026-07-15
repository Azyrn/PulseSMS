package com.skeler.pulse.ui

import com.skeler.pulse.contact.matchesBlockedSenderKey
import com.skeler.pulse.contact.toBlockedSenderKeyOrNull
import com.skeler.pulse.sms.SmsThread
import com.skeler.pulse.sms.SystemSms

internal fun List<SystemSms>.hasUnreadInboundMessages(): Boolean = any { message ->
    message.isInbound && !message.read
}

private const val MinimumReplyablePhoneDigitCount = 7

internal fun String.isReplyableConversationAddress(): Boolean =
    none(Char::isLetter) && count(Char::isDigit) >= MinimumReplyablePhoneDigitCount

internal fun SystemSms.asReadIfInbound(): SystemSms = if (isInbound && !read) {
    copy(read = true)
} else {
    this
}

internal fun SmsThread.asRead(): SmsThread = if (unreadCount > 0) {
    copy(unreadCount = 0)
} else {
    this
}

internal fun SmsThread.matchesReadTarget(target: ReadConversationTarget): Boolean = when {
    threadId == target.threadId -> true
    target.threadId == null && address.equals(target.address, ignoreCase = false) -> true
    else -> false
}

internal fun SmsThread.isBlockedBy(blockedAddresses: Set<String>): Boolean =
    address.toBlockedSenderKeyOrNull()
        ?.let { threadKey -> blockedAddresses.any { blockedKey -> blockedKey.matchesBlockedSenderKey(threadKey) } }
        ?: false

internal fun List<SmsThread>.withoutBlockedAddresses(blockedAddresses: Set<String>): List<SmsThread> =
    filterNot { thread -> thread.isBlockedBy(blockedAddresses) }

data class RealInboxState(
    val threads: List<SmsThread> = emptyList(),
    val archivedThreads: List<SmsThread> = emptyList(),
    val pinnedThreadIds: Set<Long> = emptySet(),
    val archivedThreadIds: Set<Long> = emptySet(),
    val blockedAddresses: Set<String> = emptySet(),
    val loading: Boolean = true,
    val showLoadingCard: Boolean = false,
    val permissionDenied: Boolean = false,
    val isDefaultSmsApp: Boolean = true,
    val errorMessage: String? = null,
)

internal data class ReadConversationTarget(
    val address: String,
    val threadId: Long?,
)

data class RealConversationState(
    val address: String = "",
    val messages: List<SystemSms> = emptyList(),
    val loading: Boolean = true,
    val importantMessageIds: Set<Long> = emptySet(),
    val isReplyable: Boolean = true,
)

sealed interface SendState {
    data object Idle : SendState
    data class Sending(val body: String) : SendState
    data class Sent(val body: String) : SendState
    data class Failed(val body: String) : SendState
}

internal fun shouldStartSmsSend(sendState: SendState): Boolean = sendState !is SendState.Sending
