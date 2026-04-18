package com.skeler.pulse.messaging.domain

import com.skeler.pulse.messaging.api.ConversationRepository
import com.skeler.pulse.messaging.api.ConversationSummary
import kotlinx.coroutines.flow.Flow

class ObserveInboxUseCase(
    private val repository: ConversationRepository,
) {
    operator fun invoke(): Flow<List<ConversationSummary>> = repository.observeInbox()
}
