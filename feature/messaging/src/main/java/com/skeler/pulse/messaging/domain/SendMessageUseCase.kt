package com.skeler.pulse.messaging.domain

import com.skeler.pulse.contracts.ConversationId
import com.skeler.pulse.contracts.messaging.MessageDraft
import com.skeler.pulse.messaging.api.ConversationRepository
import com.skeler.pulse.messaging.api.SendMessageResult

class SendMessageUseCase(
    private val repository: ConversationRepository,
) {
    suspend operator fun invoke(
        conversationId: ConversationId,
        draft: MessageDraft,
    ): SendMessageResult = repository.sendMessage(conversationId, draft)
}
