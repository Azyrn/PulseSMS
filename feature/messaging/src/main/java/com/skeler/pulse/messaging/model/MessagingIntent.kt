package com.skeler.pulse.messaging.model

import com.skeler.pulse.contracts.messaging.BusinessPriority

sealed interface MessagingIntent {
    data object LoadConversation : MessagingIntent

    data class DraftChanged(
        val value: String,
    ) : MessagingIntent

    data class PriorityChanged(
        val priority: BusinessPriority,
    ) : MessagingIntent

    data object SendPressed : MessagingIntent
}
