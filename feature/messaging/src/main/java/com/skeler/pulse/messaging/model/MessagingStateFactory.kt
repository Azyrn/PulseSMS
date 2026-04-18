package com.skeler.pulse.messaging.model

import com.skeler.pulse.contracts.ConversationId
import com.skeler.pulse.contracts.messaging.ComposerState
import com.skeler.pulse.contracts.messaging.MessageDraft
import com.skeler.pulse.contracts.messaging.MessageTimeline
import com.skeler.pulse.contracts.messaging.MessagingState
import com.skeler.pulse.contracts.messaging.SendEligibility
import kotlinx.collections.immutable.persistentListOf

object MessagingStateFactory {
    fun initial(conversationId: ConversationId): MessagingState =
        MessagingState.LoadingHistory(
            conversationId = conversationId,
            timeline = MessageTimeline(items = persistentListOf()),
            composer = ComposerState(
                draft = MessageDraft.empty(),
                eligibility = SendEligibility.Allowed,
            ),
            reason = com.skeler.pulse.contracts.messaging.LoadReason.Initial,
        )
}
