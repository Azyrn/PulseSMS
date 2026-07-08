package com.skeler.pulse.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import androidx.fragment.app.FragmentActivity
import com.skeler.pulse.R
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

internal fun BiometricAvailability.lockScreenMessage(resources: Resources): String = when (this) {
    BiometricAvailability.Available -> resources.getString(R.string.biometric_tap_to_auth)
    BiometricAvailability.NoHardware -> resources.getString(R.string.biometric_no_hardware)
    BiometricAvailability.HardwareUnavailable -> resources.getString(R.string.biometric_hw_unavailable)
    BiometricAvailability.NoneEnrolled -> resources.getString(R.string.biometric_none_enrolled)
    BiometricAvailability.SecurityUpdateRequired -> resources.getString(R.string.biometric_security_update)
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

    data class ReactionCard(
        val emoji: String,
        val referencedText: String,
        override val key: String,
    ) : ConversationTimelineItem {
        override val contentType: String = "conversation_reaction_card"
    }
}

data class UnmatchedReaction(
    val emoji: String,
    val referencedText: String,
    val date: Long,
) {
    val key: String get() = "conversation_reaction_$date"
}

internal fun buildConversationTimeline(
    messages: List<SystemSms>,
    unmatchedReactions: List<UnmatchedReaction>,
    unreadMessagesFormatter: (Int) -> String,
    todayLabel: String,
    yesterdayLabel: String,
): List<ConversationTimelineItem> {
    if (messages.isEmpty() && unmatchedReactions.isEmpty()) return emptyList()

    val items = ArrayList<ConversationTimelineItem>(messages.size + unmatchedReactions.size + 4)
    var lastDate: LocalDate? = null
    val unreadMessages = messages.count { it.isInbound && !it.read }
    val firstUnreadMessageId = messages.firstOrNull { it.isInbound && !it.read }?.id

    val allEntries = buildList<Pair<Long, Any>> {
        messages.forEach { add(Pair(it.date, it)) }
        unmatchedReactions.forEach { add(Pair(it.date, it)) }
        sortBy { it.first }
    }

    for (entry in allEntries) {
        val entryDate = java.time.Instant.ofEpochMilli(entry.first)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        if (entryDate != lastDate) {
            items += ConversationTimelineItem.DayDivider(
                key = "conversation_day_${entryDate}",
                label = entryDate.toConversationDayLabel(todayLabel, yesterdayLabel),
            )
            lastDate = entryDate
        }
        when (val data = entry.second) {
            is SystemSms -> {
                if (data.id == firstUnreadMessageId) {
                    items += ConversationTimelineItem.UnreadDivider(
                        key = "conversation_unread_${data.id}",
                        label = unreadMessagesFormatter(unreadMessages),
                    )
                }
                items += ConversationTimelineItem.Message(data)
            }
            is UnmatchedReaction -> {
                items += ConversationTimelineItem.ReactionCard(
                    emoji = data.emoji,
                    referencedText = data.referencedText,
                    key = data.key,
                )
            }
        }
    }

    return items
}

internal fun LocalDate.toConversationDayLabel(
    todayLabel: String,
    yesterdayLabel: String,
    today: LocalDate = LocalDate.now(),
): String = when (this) {
    today -> todayLabel
    today.minusDays(1) -> yesterdayLabel
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

internal fun String.toConversationCategoryLabel(
    businessLabel: String,
    personalLabel: String,
): String =
    if (any(Char::isLetter)) businessLabel else personalLabel

internal fun String.toConversationMetaLabel(
    categoryLabel: String,
    messagesLabel: String,
    unreadLabel: String?,
    keptLabel: String?,
): String {
    val parts = buildList {
        add(categoryLabel)
        add(messagesLabel)
        if (unreadLabel != null) add(unreadLabel)
        if (keptLabel != null) add(keptLabel)
    }
    return parts.joinToString(" · ")
}

internal val INBOX_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
internal val INBOX_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")
internal val CONVERSATION_DAY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")
internal val BUBBLE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
