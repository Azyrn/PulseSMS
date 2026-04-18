package com.skeler.pulse.contracts.persistence

import com.skeler.pulse.contracts.ConversationId
import com.skeler.pulse.contracts.DedupeKey
import com.skeler.pulse.contracts.MessageId
import com.skeler.pulse.contracts.QueueKey

data class PersistedMessageEnvelope(
    val schemaVersion: Int,
    val messageId: MessageId,
    val conversationId: ConversationId,
    val bodyCiphertext: String?,
    val bodyPreview: String,
    val payloadStoragePolicy: PayloadStoragePolicy,
    val sentAtEpochMillis: Long?,
    val receivedAtEpochMillis: Long?,
    val sync: PersistedSyncEnvelope,
)

data class PersistedSyncEnvelope(
    val schemaVersion: Int,
    val queueKey: QueueKey,
    val dedupeKey: DedupeKey,
    val attempt: Int,
    val maxAttempts: Int,
    val nextRetryAtEpochMillis: Long?,
    val lastFailureCode: String?,
    val completedAtEpochMillis: Long? = null,
)

enum class PayloadStoragePolicy {
    CiphertextOnly,
    RedactedPreviewWithCiphertext,
}
