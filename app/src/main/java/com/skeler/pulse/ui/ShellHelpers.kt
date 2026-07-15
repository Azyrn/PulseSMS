package com.skeler.pulse.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.fragment.app.FragmentActivity
import com.skeler.pulse.security.auth.BiometricAvailability
import com.skeler.pulse.sms.SystemSms
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal fun Instant.toInboxTimestamp(): String = when {
    atZone(ZoneId.systemDefault()).toLocalDate() == java.time.LocalDate.now() ->
        INBOX_TIME_FORMATTER.format(atZone(ZoneId.systemDefault()))
    else -> INBOX_DATE_FORMATTER.format(atZone(ZoneId.systemDefault()))
}

internal fun BiometricAvailability.lockScreenMessage(): String = when (this) {
    BiometricAvailability.Available -> "Tap to authenticate"
    BiometricAvailability.NoHardware -> "Strong biometric hardware is not available on this device."
    BiometricAvailability.HardwareUnavailable -> "Strong biometric hardware is temporarily unavailable."
    BiometricAvailability.NoneEnrolled -> "Enroll a strong biometric before using biometric login."
    BiometricAvailability.SecurityUpdateRequired -> "Install the required biometric sensor security update."
}

internal tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

internal sealed interface ConversationTimelineItem {
    val key: String
    val contentType: String

    data class DayDivider(
        override val key: String,
        val label: String,
    ) : ConversationTimelineItem {
        override val contentType: String = "conversation_day_divider"
    }

    data class UnreadDivider(
        val label: String,
        override val key: String,
    ) : ConversationTimelineItem {
        override val contentType: String = "conversation_unread_divider"
    }

    data class Message(
        val message: SystemSms,
    ) : ConversationTimelineItem {
        override val key: String = "conversation_message_${message.id}"
        override val contentType: String = "conversation_message"
    }
}

internal fun List<SystemSms>.toConversationTimeline(): List<ConversationTimelineItem> {
    if (isEmpty()) return emptyList()

    val items = ArrayList<ConversationTimelineItem>(size + 4)
    var lastDate: LocalDate? = null
    val unreadMessages = count { it.isInbound && !it.read }
    val firstUnreadMessageId = firstOrNull { it.isInbound && !it.read }?.id

    for (message in this) {
        val localDate = message.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
        if (localDate != lastDate) {
            items += ConversationTimelineItem.DayDivider(
                key = "conversation_day_${localDate}",
                label = localDate.toConversationDayLabel(),
            )
            lastDate = localDate
        }
        if (message.id == firstUnreadMessageId) {
            items += ConversationTimelineItem.UnreadDivider(
                key = "conversation_unread_${message.id}",
                label = if (unreadMessages == 1) "1 unread message"
                else "$unreadMessages unread messages",
            )
        }
        items += ConversationTimelineItem.Message(message)
    }

    return items
}

internal fun LocalDate.toConversationDayLabel(today: LocalDate = LocalDate.now()): String = when (this) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    else -> CONVERSATION_DAY_FORMATTER.format(this)
}

internal fun Instant.toConversationTime(): String =
    BUBBLE_TIME_FORMATTER.format(atZone(ZoneId.systemDefault()))

internal fun String.toAvatarInitials(): String =
    trim()
        .split(" ")
        .filter(String::isNotBlank)
        .take(2)
        .joinToString("") { it.take(1).uppercase() }
        .ifBlank { take(2).uppercase().ifBlank { "#" } }

internal fun String.isDirectAddressCandidate(): Boolean {
    if (isBlank()) return false
    return any(Char::isDigit) || contains('@') || any { it == '+' }
}

internal fun String.toConversationCategoryLabel(): String =
    if (any(Char::isLetter)) "Business SMS" else "Personal SMS"

internal fun String.toConversationMetaLabel(
    totalMessages: Int,
    unreadCount: Int,
    importantCount: Int,
): String {
    val parts = buildList {
        add(toConversationCategoryLabel())
        add("$totalMessages messages")
        if (unreadCount > 0) add("$unreadCount unread")
        if (importantCount > 0) add("$importantCount kept")
    }
    return parts.joinToString(" · ")
}

internal val INBOX_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
internal val INBOX_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")
internal val CONVERSATION_DAY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")
internal val BUBBLE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
