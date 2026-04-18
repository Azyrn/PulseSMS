package com.skeler.pulse.database.api

import com.skeler.pulse.contracts.ConversationId
import com.skeler.pulse.contracts.MessageId
import com.skeler.pulse.contracts.errors.SystemError
import com.skeler.pulse.contracts.persistence.PayloadStoragePolicy
import com.skeler.pulse.contracts.persistence.PersistedMessageEnvelope
import com.skeler.pulse.contracts.persistence.PersistedSyncEnvelope
import com.skeler.pulse.security.model.EncryptedPayload
import kotlinx.coroutines.flow.Flow
import java.time.Instant

data class StoreEncryptedMessageRequest(
    val schemaVersion: Int,
    val messageId: MessageId,
    val conversationId: ConversationId,
    val encryptedPayload: EncryptedPayload,
    val bodyPreview: String,
    val payloadStoragePolicy: PayloadStoragePolicy,
    val sentAt: Instant?,
    val receivedAt: Instant?,
    val sync: PersistedSyncEnvelope,
)

sealed interface MessageStoreResult {
    data class Success(
        val envelope: PersistedMessageEnvelope,
    ) : MessageStoreResult

    data class Failure(
        val error: SystemError,
    ) : MessageStoreResult
}

interface EncryptedMessageStore {
    suspend fun store(request: StoreEncryptedMessageRequest): MessageStoreResult

    fun observeConversation(conversationId: ConversationId): Flow<List<PersistedMessageEnvelope>>

    fun observeAllMessages(): Flow<List<PersistedMessageEnvelope>>

    suspend fun pendingSync(limit: Int): List<PersistedMessageEnvelope>

    suspend fun updateSync(
        messageId: MessageId,
        sync: PersistedSyncEnvelope,
    ): MessageStoreResult
}
