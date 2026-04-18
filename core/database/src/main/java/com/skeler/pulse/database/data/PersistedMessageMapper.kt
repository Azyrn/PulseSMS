package com.skeler.pulse.database.data

import com.skeler.pulse.contracts.persistence.PayloadStoragePolicy
import com.skeler.pulse.contracts.persistence.PersistedMessageEnvelope
import com.skeler.pulse.contracts.persistence.PersistedSyncEnvelope
import com.skeler.pulse.database.api.StoreEncryptedMessageRequest

internal object PersistedMessageMapper {
    fun toEntity(
        request: StoreEncryptedMessageRequest,
        ciphertext: String,
        correlationId: String = "",
    ): EncryptedMessageEntity = EncryptedMessageEntity(
        messageId = request.messageId,
        schemaVersion = request.schemaVersion,
        conversationId = request.conversationId,
        bodyCiphertext = ciphertext,
        bodyPreview = request.bodyPreview,
        payloadStoragePolicy = request.payloadStoragePolicy.name,
        sentAtEpochMillis = request.sentAt?.toEpochMilli(),
        receivedAtEpochMillis = request.receivedAt?.toEpochMilli(),
        queueKey = request.sync.queueKey,
        dedupeKey = request.sync.dedupeKey,
        attempt = request.sync.attempt,
        maxAttempts = request.sync.maxAttempts,
        nextRetryAtEpochMillis = request.sync.nextRetryAtEpochMillis,
        lastFailureCode = request.sync.lastFailureCode,
        syncCompletedAtEpochMillis = request.sync.completedAtEpochMillis,
        messageCorrelationId = correlationId,
    )

    fun toEnvelope(entity: EncryptedMessageEntity): PersistedMessageEnvelope = PersistedMessageEnvelope(
        schemaVersion = entity.schemaVersion,
        messageId = entity.messageId,
        conversationId = entity.conversationId,
        bodyCiphertext = entity.bodyCiphertext,
        bodyPreview = entity.bodyPreview,
        payloadStoragePolicy = PayloadStoragePolicy.valueOf(entity.payloadStoragePolicy),
        sentAtEpochMillis = entity.sentAtEpochMillis,
        receivedAtEpochMillis = entity.receivedAtEpochMillis,
        sync = PersistedSyncEnvelope(
            schemaVersion = entity.schemaVersion,
            queueKey = entity.queueKey,
            dedupeKey = entity.dedupeKey,
            attempt = entity.attempt,
            maxAttempts = entity.maxAttempts,
            nextRetryAtEpochMillis = entity.nextRetryAtEpochMillis,
            lastFailureCode = entity.lastFailureCode,
            completedAtEpochMillis = entity.syncCompletedAtEpochMillis,
        ),
    )
}
