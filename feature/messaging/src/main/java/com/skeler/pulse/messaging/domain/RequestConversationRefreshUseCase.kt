package com.skeler.pulse.messaging.domain

import com.skeler.pulse.contracts.ConversationId
import com.skeler.pulse.messaging.api.ConversationRepository

class RequestConversationRefreshUseCase(
    private val repository: ConversationRepository,
) {
    suspend operator fun invoke(conversationId: ConversationId) {
        repository.requestRefresh(conversationId)
    }
}
