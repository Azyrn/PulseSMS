package com.skeler.pulse.messaging.model

import com.skeler.pulse.contracts.MessageId
import com.skeler.pulse.contracts.errors.MessagingSurfaceError
import com.skeler.pulse.messaging.api.ConversationSnapshot

sealed interface MessagingMutation {
    data class HistoryLoaded(
        val snapshot: ConversationSnapshot,
    ) : MessagingMutation

    data class DraftUpdated(
        val value: String,
    ) : MessagingMutation

    data class SendingStarted(
        val messageId: MessageId,
    ) : MessagingMutation

    data object SendingCompleted : MessagingMutation

    data class SendFailed(
        val error: MessagingSurfaceError,
    ) : MessagingMutation
}
