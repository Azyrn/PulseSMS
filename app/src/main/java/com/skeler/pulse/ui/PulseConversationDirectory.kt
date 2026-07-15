package com.skeler.pulse.ui

import com.skeler.pulse.contracts.ConversationId
import java.util.Locale

data class PulseConversationMetadata(
    val conversationId: ConversationId,
    val title: String,
    val subtitle: String,
) {
    val initials: String =
        title.split(" ")
            .filter(String::isNotBlank)
            .take(2)
            .joinToString("") { it.take(1).uppercase(Locale.getDefault()) }
            .ifBlank { "#" }
}

object PulseConversationDirectory {
    fun metadata(conversationId: ConversationId): PulseConversationMetadata = when (conversationId) {
        "business-primary" -> PulseConversationMetadata(
            conversationId = conversationId,
            title = "Avery Stone",
            subtitle = "+1 (202) 555-0147",
        )

        "business-verification-pending" -> PulseConversationMetadata(
            conversationId = conversationId,
            title = "Jordan Lee",
            subtitle = "+1 (202) 555-0171",
        )

        "business-recipient-pending" -> PulseConversationMetadata(
            conversationId = conversationId,
            title = "Riley Park",
            subtitle = "+1 (202) 555-0186",
        )

        "business-identity-missing" -> PulseConversationMetadata(
            conversationId = conversationId,
            title = "Morgan Chen",
            subtitle = "+1 (202) 555-0192",
        )

        "business-10dlc-pending" -> PulseConversationMetadata(
            conversationId = conversationId,
            title = "Taylor Brooks",
            subtitle = "+1 (202) 555-0159",
        )

        else -> PulseConversationMetadata(
            conversationId = conversationId,
            title = conversationId.humanizeConversationId(),
            subtitle = "Business SMS",
        )
    }

    private fun String.humanizeConversationId(): String =
        split("-", "_")
            .filter(String::isNotBlank)
            .joinToString(" ") { word ->
                word.replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
                }
            }
}
