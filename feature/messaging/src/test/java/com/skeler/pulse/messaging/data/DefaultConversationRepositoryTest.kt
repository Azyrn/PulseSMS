package com.skeler.pulse.messaging.data

import com.skeler.pulse.contracts.errors.NetworkError
import com.skeler.pulse.contracts.messaging.ConversationSyncState
import com.skeler.pulse.contracts.messaging.DeliveryIndicator
import com.skeler.pulse.contracts.messaging.MessageDraft
import com.skeler.pulse.contracts.messaging.RowSyncState
import com.skeler.pulse.contracts.messaging.SendBlockReason
import com.skeler.pulse.contracts.messaging.SendEligibility
import com.skeler.pulse.contracts.persistence.PayloadStoragePolicy
import com.skeler.pulse.contracts.persistence.PersistedMessageEnvelope
import com.skeler.pulse.contracts.persistence.PersistedSyncEnvelope
import com.skeler.pulse.database.api.EncryptedMessageStore
import com.skeler.pulse.database.api.MessageStoreResult
import com.skeler.pulse.database.api.StoreEncryptedMessageRequest
import com.skeler.pulse.security.api.MessageProtector
import com.skeler.pulse.security.api.ProtectMessageRequest
import com.skeler.pulse.security.api.ProtectionResult
import com.skeler.pulse.security.data.StaticBusinessComplianceProvider
import com.skeler.pulse.security.api.BusinessComplianceStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class DefaultConversationRepositoryTest {
    @Test
    fun `maps auth sync failure into explicit row and delivery diagnostics`() = runTest {
        val repository = DefaultConversationRepository(
            encryptedMessageStore = FakeEncryptedMessageStore(
                envelopes = listOf(
                    envelope(lastFailureCode = "http_401")
                )
            ),
            messageProtector = UnusedMessageProtector,
            businessComplianceProvider = StaticBusinessComplianceProvider(
                statuses = mapOf("conv-1" to BusinessComplianceStatus())
            ),
        )

        val snapshot = repository.observeConversation("conv-1").first()
        val item = snapshot.timeline.items.single()

        val rowFailure = item.status.sync as RowSyncState.Failed
        val deliveryFailure = item.status.delivery as DeliveryIndicator.Failed
        assertTrue(rowFailure.error is NetworkError.Unreachable)
        assertEquals("Sync gateway rejected Pulse credentials", rowFailure.error.message)
        assertEquals("Sync gateway rejected Pulse credentials", deliveryFailure.error.message)
        assertTrue(snapshot.syncState is ConversationSyncState.Failed)
    }

    @Test
    fun `maps rate limit sync failure into rate limited delivery error`() = runTest {
        val repository = DefaultConversationRepository(
            encryptedMessageStore = FakeEncryptedMessageStore(
                envelopes = listOf(
                    envelope(lastFailureCode = "http_429", nextRetryAtEpochMillis = null)
                )
            ),
            messageProtector = UnusedMessageProtector,
            businessComplianceProvider = StaticBusinessComplianceProvider(
                statuses = mapOf("conv-1" to BusinessComplianceStatus())
            ),
        )

        val snapshot = repository.observeConversation("conv-1").first()
        val item = snapshot.timeline.items.single()

        val rowFailure = item.status.sync as RowSyncState.Failed
        assertTrue(rowFailure.error is NetworkError.RateLimited)
        assertEquals("Carrier or gateway rate limit reached", rowFailure.error.message)
        assertTrue(snapshot.syncState is ConversationSyncState.Failed)
    }

    @Test
    fun `maps retry schedule into conversation backoff state`() = runTest {
        val repository = DefaultConversationRepository(
            encryptedMessageStore = FakeEncryptedMessageStore(
                envelopes = listOf(
                    envelope(lastFailureCode = "http_429", nextRetryAtEpochMillis = 5_000L)
                )
            ),
            messageProtector = UnusedMessageProtector,
            businessComplianceProvider = StaticBusinessComplianceProvider(
                statuses = mapOf("conv-1" to BusinessComplianceStatus())
            ),
        )

        val snapshot = repository.observeConversation("conv-1").first()

        val syncState = snapshot.syncState as ConversationSyncState.Backoff
        assertEquals("http_429", syncState.envelope.lastFailureCode)
        assertEquals(5_000L, syncState.envelope.nextRetryAt.toEpochMilli())
    }

    @Test
    fun `maps verification pending conversation into unverified message item`() = runTest {
        val complianceUpdatedAt = Instant.parse("2026-04-17T08:30:00Z")
        val repository = DefaultConversationRepository(
            encryptedMessageStore = FakeEncryptedMessageStore(
                envelopes = listOf(
                    envelope(
                        conversationId = "business-verification-pending",
                        lastFailureCode = "http_401",
                    )
                )
            ),
            messageProtector = UnusedMessageProtector,
            businessComplianceProvider = StaticBusinessComplianceProvider(
                statuses = mapOf(
                    "business-verification-pending" to BusinessComplianceStatus(
                        senderVerified = false,
                        updatedAt = complianceUpdatedAt,
                    )
                )
            ),
        )

        val snapshot = repository.observeConversation("business-verification-pending").first()

        assertTrue(!snapshot.timeline.items.single().isBusinessVerified)
        val eligibility = snapshot.eligibility as SendEligibility.Blocked
        assertEquals(SendBlockReason.SenderVerificationPending, eligibility.reason)
        assertEquals(complianceUpdatedAt, snapshot.complianceUpdatedAt)
    }

    @Test
    fun `observed snapshot exposes compliance freshness timestamp`() = runTest {
        val repository = DefaultConversationRepository(
            encryptedMessageStore = FakeEncryptedMessageStore(
                envelopes = listOf(
                    envelope(lastFailureCode = "http_401")
                )
            ),
            messageProtector = UnusedMessageProtector,
            businessComplianceProvider = StaticBusinessComplianceProvider(
                statuses = mapOf(
                    "conv-1" to BusinessComplianceStatus(
                        updatedAt = Instant.parse("2026-04-17T09:00:00Z"),
                    )
                )
            ),
        )

        val snapshot = repository.observeConversation("conv-1").first()

        assertNotNull(snapshot.complianceUpdatedAt)
    }

    private fun envelope(
        conversationId: String = "conv-1",
        lastFailureCode: String,
        nextRetryAtEpochMillis: Long? = null,
    ): PersistedMessageEnvelope = PersistedMessageEnvelope(
        schemaVersion = 1,
        messageId = "message-1",
        conversationId = conversationId,
        bodyCiphertext = "ciphertext",
        bodyPreview = "",
        payloadStoragePolicy = PayloadStoragePolicy.CiphertextOnly,
        sentAtEpochMillis = 1_000L,
        receivedAtEpochMillis = null,
        sync = PersistedSyncEnvelope(
            schemaVersion = 1,
            queueKey = conversationId,
            dedupeKey = "message-1",
            attempt = 2,
            maxAttempts = 5,
            nextRetryAtEpochMillis = nextRetryAtEpochMillis,
            lastFailureCode = lastFailureCode,
        ),
    )

    private class FakeEncryptedMessageStore(
        private val envelopes: List<PersistedMessageEnvelope>,
    ) : EncryptedMessageStore {
        override suspend fun store(request: StoreEncryptedMessageRequest): MessageStoreResult =
            error("Not used in observe tests")

        override fun observeConversation(conversationId: String): Flow<List<PersistedMessageEnvelope>> =
            flowOf(envelopes)

        override fun observeAllMessages(): Flow<List<PersistedMessageEnvelope>> =
            flowOf(envelopes)

        override suspend fun pendingSync(limit: Int): List<PersistedMessageEnvelope> = envelopes.take(limit)

        override suspend fun updateSync(
            messageId: String,
            sync: PersistedSyncEnvelope,
        ): MessageStoreResult = error("Not used in observe tests")
    }

    private data object UnusedMessageProtector : MessageProtector {
        override suspend fun protect(request: ProtectMessageRequest): ProtectionResult =
            error("Not used in observe tests")
    }
}
