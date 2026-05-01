package com.skeler.pulse.database.data

import com.skeler.pulse.contracts.errors.SystemError
import com.skeler.pulse.contracts.observability.EventName
import com.skeler.pulse.contracts.observability.LogLevel
import com.skeler.pulse.contracts.observability.ObservabilityProvider
import com.skeler.pulse.contracts.observability.ObservabilityScope
import com.skeler.pulse.contracts.observability.ObservedEvent
import com.skeler.pulse.contracts.persistence.PayloadStoragePolicy
import com.skeler.pulse.contracts.persistence.PersistedMessageEnvelope
import com.skeler.pulse.contracts.persistence.PersistedSyncEnvelope
import com.skeler.pulse.database.api.EncryptedMessageStore
import com.skeler.pulse.database.api.MessageStoreResult
import com.skeler.pulse.database.api.StoreEncryptedMessageRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomEncryptedMessageStore(
    private val encryptedMessageDao: EncryptedMessageDao,
    private val ciphertextCodec: CiphertextCodec,
    private val observabilityProvider: ObservabilityProvider,
    private val clock: () -> Long = System::currentTimeMillis,
) : EncryptedMessageStore {

    private val scope = ObservabilityScope(
        feature = "database",
        component = "RoomEncryptedMessageStore",
        operation = "store",
    )

    override suspend fun store(request: StoreEncryptedMessageRequest): MessageStoreResult {
        val validation = validate(request)
        if (validation != null) {
            return MessageStoreResult.Failure(validation)
        }

        return try {
            val entity = PersistedMessageMapper.toEntity(
                request = request,
                ciphertext = ciphertextCodec.encode(request.encryptedPayload.ciphertext),
                initializationVector = ciphertextCodec.encode(request.encryptedPayload.initializationVector),
            )
            encryptedMessageDao.upsert(entity)

            val traceContext = observabilityProvider.newTraceContext(
                correlationId = request.sync.dedupeKey,
                messageId = request.messageId,
                conversationId = request.conversationId,
            )
            observabilityProvider.logger(scope).log(
                level = LogLevel.INFO,
                event = ObservedEvent(EventName("database.message_persisted")),
                traceContext = traceContext,
            )

            MessageStoreResult.Success(PersistedMessageMapper.toEnvelope(entity))
        } catch (_: Exception) {
            MessageStoreResult.Failure(SystemError.PersistenceFailure())
        }
    }

    override fun observeConversation(conversationId: String): Flow<List<PersistedMessageEnvelope>> =
        encryptedMessageDao.observeConversation(conversationId).map { entities ->
            entities.map(PersistedMessageMapper::toEnvelope)
        }

    override fun observeAllMessages(): Flow<List<PersistedMessageEnvelope>> =
        encryptedMessageDao.observeAllMessages().map { entities ->
            entities.map(PersistedMessageMapper::toEnvelope)
        }

    override suspend fun pendingSync(limit: Int): List<PersistedMessageEnvelope> =
        encryptedMessageDao.pendingSync(limit, clock()).map(PersistedMessageMapper::toEnvelope)

    override suspend fun pendingSync(conversationId: String, limit: Int): List<PersistedMessageEnvelope> =
        encryptedMessageDao.pendingSyncForConversation(conversationId, limit, clock())
            .map(PersistedMessageMapper::toEnvelope)

    override suspend fun updateSync(
        messageId: String,
        sync: PersistedSyncEnvelope,
    ): MessageStoreResult = try {
        val updated = encryptedMessageDao.updateSync(
            messageId = messageId,
            queueKey = sync.queueKey,
            dedupeKey = sync.dedupeKey,
            attempt = sync.attempt,
            maxAttempts = sync.maxAttempts,
            nextRetryAtEpochMillis = sync.nextRetryAtEpochMillis,
            lastFailureCode = sync.lastFailureCode,
            completedAtEpochMillis = sync.completedAtEpochMillis,
        )
        if (updated == 0) {
            MessageStoreResult.Failure(SystemError.PersistenceFailure(message = "Message not found"))
        } else {
            val envelope = encryptedMessageDao.findByMessageId(messageId)
                ?.let(PersistedMessageMapper::toEnvelope)
            if (envelope != null) {
                MessageStoreResult.Success(envelope)
            } else {
                MessageStoreResult.Failure(SystemError.PersistenceFailure(message = "Updated message missing"))
            }
        }
    } catch (_: Exception) {
        MessageStoreResult.Failure(SystemError.PersistenceFailure())
    }

    private fun validate(request: StoreEncryptedMessageRequest): SystemError.ValidationFailure? {
        if (request.encryptedPayload.ciphertext.isEmpty()) {
            return SystemError.ValidationFailure(message = "Ciphertext is required")
        }
        if (request.encryptedPayload.keyAlias.isBlank()) {
            return SystemError.ValidationFailure(message = "Key alias is required")
        }
        if (request.encryptedPayload.initializationVector.isEmpty()) {
            return SystemError.ValidationFailure(message = "Initialization vector is required")
        }
        if (request.payloadStoragePolicy == PayloadStoragePolicy.CiphertextOnly && request.bodyPreview.isNotEmpty()) {
            return SystemError.ValidationFailure(message = "Ciphertext-only records cannot store a preview")
        }
        if (request.sync.attempt > request.sync.maxAttempts) {
            return SystemError.ValidationFailure(message = "Retry attempt exceeds max attempts")
        }
        return null
    }
}
