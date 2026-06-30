package com.skeler.pulse.ui

import com.skeler.pulse.contact.matchesBlockedSenderKey
import com.skeler.pulse.contact.toBlockedSenderKeyOrNull
import com.skeler.pulse.sms.ScheduledMessageEntity
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
    val mutedAddresses: Set<String> = emptySet(),
    val threadEmojis: Map<Long, String> = emptyMap(),
    val loading: Boolean = true,
    val showLoadingCard: Boolean = false,
    val permissionDenied: Boolean = false,
    val isDefaultSmsApp: Boolean = true,
    val errorMessage: String? = null,
    val drafts: Map<String, String> = emptyMap(),
    val scheduledAddresses: Set<String> = emptySet(),
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
    val messageReactions: Map<Long, String> = emptyMap(),
    val unmatchedReactions: List<UnmatchedReaction> = emptyList(),
    val isReplyable: Boolean = true,
    val hasMoreMessages: Boolean = false,
    val loadingMore: Boolean = false,
    val totalMessageCount: Int = 0,
    val searchQuery: String = "",
    val draft: String = "",
    val scheduledMessages: List<ScheduledMessageEntity> = emptyList(),
)

sealed interface SendState {
    data object Idle : SendState
    data class Sending(val body: String) : SendState
    data class Sent(val body: String) : SendState
    data class Failed(val body: String) : SendState
}

internal fun shouldStartSmsSend(sendState: SendState): Boolean = sendState !is SendState.Sending
