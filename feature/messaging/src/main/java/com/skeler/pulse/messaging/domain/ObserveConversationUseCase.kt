package com.skeler.pulse.messaging.domain

import com.skeler.pulse.contracts.ConversationId
import com.skeler.pulse.messaging.api.ConversationSnapshot
import com.skeler.pulse.messaging.api.ConversationRepository
import kotlinx.coroutines.flow.Flow

class ObserveConversationUseCase(
    private val repository: ConversationRepository,
) {
    operator fun invoke(conversationId: ConversationId): Flow<ConversationSnapshot> =
        repository.observeConversation(conversationId)
}
