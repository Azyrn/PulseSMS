package com.skeler.pulse.database.data

import com.skeler.pulse.contracts.persistence.PayloadStoragePolicy
import com.skeler.pulse.contracts.persistence.PersistedSyncEnvelope
import com.skeler.pulse.database.api.MessageStoreResult
import com.skeler.pulse.database.api.StoreEncryptedMessageRequest
import com.skeler.pulse.observability.PulseObservabilityProvider
import com.skeler.pulse.security.model.EncryptedPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class RoomEncryptedMessageStoreTest {

    @Test
    fun `rejects ciphertext only records with previews`() = runBlocking {
        val store = RoomEncryptedMessageStore(
            encryptedMessageDao = FakeEncryptedMessageDao(),
            ciphertextCodec = Base64CiphertextCodec(),
            observabilityProvider = PulseObservabilityProvider("test.database"),
        )

        val result = store.store(
            StoreEncryptedMessageRequest(
                schemaVersion = 1,
                messageId = "msg-1",
                conversationId = "conv-1",
                encryptedPayload = EncryptedPayload(
                    keyAlias = "alias",
                    ciphertext = byteArrayOf(9, 9, 9),
                    initializationVector = byteArrayOf(1, 2, 3),
                    encryptedAt = Instant.parse("2026-04-17T00:00:00Z"),
                ),
                bodyPreview = "plaintext",
                payloadStoragePolicy = PayloadStoragePolicy.CiphertextOnly,
                sentAt = Instant.parse("2026-04-17T00:00:00Z"),
                receivedAt = null,
                sync = PersistedSyncEnvelope(
                    schemaVersion = 1,
                    queueKey = "queue-1",
                    dedupeKey = "dedupe-1",
                    attempt = 1,
                    maxAttempts = 3,
                    nextRetryAtEpochMillis = null,
                    lastFailureCode = null,
                ),
            )
        )

        assertTrue(result is MessageStoreResult.Failure)
    }

    private class FakeEncryptedMessageDao : EncryptedMessageDao {
        override suspend fun upsert(entity: EncryptedMessageEntity) = Unit

        override fun observeConversation(conversationId: String): Flow<List<EncryptedMessageEntity>> = flowOf(emptyList())

        override fun observeAllMessages(): Flow<List<EncryptedMessageEntity>> = flowOf(emptyList())

        override suspend fun pendingSync(limit: Int): List<EncryptedMessageEntity> = emptyList()

        override suspend fun findByMessageId(messageId: String): EncryptedMessageEntity? = null

        override suspend fun updateSync(
            messageId: String,
            queueKey: String,
            dedupeKey: String,
            attempt: Int,
            maxAttempts: Int,
            nextRetryAtEpochMillis: Long?,
            lastFailureCode: String?,
            completedAtEpochMillis: Long?,
        ): Int = 0
    }
}
