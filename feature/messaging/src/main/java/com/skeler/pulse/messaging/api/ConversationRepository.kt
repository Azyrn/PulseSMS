package com.skeler.pulse.messaging.api

import com.skeler.pulse.contracts.ConversationId
import com.skeler.pulse.contracts.MessageId
import com.skeler.pulse.contracts.errors.MessagingSurfaceError
import com.skeler.pulse.contracts.messaging.ConversationSyncState
import com.skeler.pulse.contracts.messaging.MessageDraft
import com.skeler.pulse.contracts.messaging.MessageTimeline
import com.skeler.pulse.contracts.messaging.SendEligibility
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface ConversationRepository {
    fun observeInbox(): Flow<List<ConversationSummary>>

    fun observeConversation(conversationId: ConversationId): Flow<ConversationSnapshot>

    suspend fun requestRefresh(conversationId: ConversationId)

    suspend fun sendMessage(
        conversationId: ConversationId,
        draft: MessageDraft,
    ): SendMessageResult
}

data class ConversationSummary(
    val conversationId: ConversationId,
    val snippet: String,
    val timestamp: Instant?,
    val messageCount: Int,
    val syncState: ConversationSyncState,
)

data class ConversationSnapshot(
    val timeline: MessageTimeline,
    val eligibility: SendEligibility,
    val syncState: ConversationSyncState,
    val lastSyncedAt: Instant?,
    val complianceUpdatedAt: Instant?,
)

sealed interface SendMessageResult {
    data class Success(
        val messageId: MessageId,
    ) : SendMessageResult

    data class Failure(
        val error: MessagingSurfaceError,
    ) : SendMessageResult
}
