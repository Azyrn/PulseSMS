package com.skeler.pulse.sync.domain

import com.skeler.pulse.contracts.errors.SystemError
import com.skeler.pulse.contracts.persistence.PayloadStoragePolicy
import com.skeler.pulse.contracts.persistence.PersistedMessageEnvelope
import com.skeler.pulse.contracts.persistence.PersistedSyncEnvelope
import com.skeler.pulse.database.api.BusinessComplianceStatusStore
import com.skeler.pulse.database.api.EncryptedMessageStore
import com.skeler.pulse.database.api.MessageStoreResult
import com.skeler.pulse.database.api.UpsertBusinessComplianceStatusRequest
import com.skeler.pulse.observability.PulseObservabilityProvider
import com.skeler.pulse.security.api.BusinessComplianceStatus
import com.skeler.pulse.sync.api.ComplianceSyncResult
import com.skeler.pulse.sync.api.ComplianceSyncTransport
import com.skeler.pulse.sync.api.SyncTransport
import com.skeler.pulse.sync.api.SyncTransportResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class MessageSyncOrchestratorTest {
    @Test
    fun `successful sync clears retry metadata`() = runBlocking {
        val store = FakeEncryptedMessageStore()
        val complianceStore = FakeBusinessComplianceStatusStore()
        val envelope = store.messages.single()
        val orchestrator = MessageSyncOrchestrator(
            businessComplianceStatusStore = complianceStore,
            complianceTransport = FakeComplianceSyncTransport(
                result = ComplianceSyncResult.Success(BusinessComplianceStatus())
            ),
            encryptedMessageStore = store,
            transport = object : SyncTransport {
                override suspend fun send(message: PersistedMessageEnvelope): SyncTransportResult = SyncTransportResult.Success
            },
            retryPolicy = SyncRetryPolicy(jitterRatio = 0.0),
            observabilityProvider = PulseObservabilityProvider("test.sync"),
        )

        val result = orchestrator.run(envelope.conversationId)

        assertTrue(result is SyncRunResult.Success)
        assertNull(store.updatedSync?.nextRetryAtEpochMillis)
        assertNull(store.updatedSync?.lastFailureCode)
    }

    @Test
    fun `retryable failure schedules retry`() = runBlocking {
        val store = FakeEncryptedMessageStore()
        val complianceStore = FakeBusinessComplianceStatusStore()
        val envelope = store.messages.single()
        val orchestrator = MessageSyncOrchestrator(
            businessComplianceStatusStore = complianceStore,
            complianceTransport = FakeComplianceSyncTransport(
                result = ComplianceSyncResult.Success(BusinessComplianceStatus())
            ),
            encryptedMessageStore = store,
            transport = object : SyncTransport {
                override suspend fun send(message: PersistedMessageEnvelope): SyncTransportResult =
                    SyncTransportResult.RetryableFailure("timeout")
            },
            retryPolicy = SyncRetryPolicy(jitterRatio = 0.0),
            observabilityProvider = PulseObservabilityProvider("test.sync"),
        )

        val result = orchestrator.run(envelope.conversationId)

        assertTrue(result is SyncRunResult.Success)
        assertEquals(1, store.updatedSync?.attempt)
        assertEquals("timeout", store.updatedSync?.lastFailureCode)
        assertTrue(store.updatedSync?.nextRetryAtEpochMillis != null)
    }

    @Test
    fun `successful compliance fetch persists status`() = runBlocking {
        val store = FakeEncryptedMessageStore()
        val complianceStore = FakeBusinessComplianceStatusStore()
        val orchestrator = MessageSyncOrchestrator(
            businessComplianceStatusStore = complianceStore,
            complianceTransport = FakeComplianceSyncTransport(
                result = ComplianceSyncResult.Success(
                    BusinessComplianceStatus(
                        senderVerified = true,
                        recipientVerified = true,
                        identityVerified = true,
                        tenDlcRegistered = false,
                    )
                )
            ),
            encryptedMessageStore = store,
            transport = object : SyncTransport {
                override suspend fun send(message: PersistedMessageEnvelope): SyncTransportResult = SyncTransportResult.Success
            },
            retryPolicy = SyncRetryPolicy(jitterRatio = 0.0),
            observabilityProvider = PulseObservabilityProvider("test.sync"),
        )

        val result = orchestrator.run("conv-1")

        assertTrue(result is SyncRunResult.Success)
        assertEquals("conv-1", complianceStore.lastRequest?.conversationId)
        assertEquals(false, complianceStore.lastRequest?.status?.tenDlcRegistered)
    }

    @Test
    fun `retryable compliance fetch requests worker retry`() = runBlocking {
        val store = FakeEncryptedMessageStore()
        val complianceStore = FakeBusinessComplianceStatusStore()
        val orchestrator = MessageSyncOrchestrator(
            businessComplianceStatusStore = complianceStore,
            complianceTransport = FakeComplianceSyncTransport(
                result = ComplianceSyncResult.RetryableFailure("http_429")
            ),
            encryptedMessageStore = store,
            transport = object : SyncTransport {
                override suspend fun send(message: PersistedMessageEnvelope): SyncTransportResult = SyncTransportResult.Success
            },
            retryPolicy = SyncRetryPolicy(jitterRatio = 0.0),
            observabilityProvider = PulseObservabilityProvider("test.sync"),
        )

        val result = orchestrator.run("conv-1")

        assertTrue(result is SyncRunResult.PartialFailure)
        assertEquals(1, (result as SyncRunResult.PartialFailure).failed)
        assertTrue(complianceStore.lastRequest == null)
    }

    private class FakeEncryptedMessageStore : EncryptedMessageStore {
        val messages = mutableListOf(
            PersistedMessageEnvelope(
                schemaVersion = 1,
                messageId = "msg-1",
                conversationId = "conv-1",
                bodyCiphertext = "cipher",
                bodyPreview = "",
                payloadStoragePolicy = PayloadStoragePolicy.CiphertextOnly,
                sentAtEpochMillis = 0L,
                receivedAtEpochMillis = null,
                sync = PersistedSyncEnvelope(
                    schemaVersion = 1,
                    queueKey = "conv-1",
                    dedupeKey = "msg-1",
                    attempt = 0,
                    maxAttempts = 5,
                    nextRetryAtEpochMillis = null,
                    lastFailureCode = null,
                ),
            )
        )
        var updatedSync: PersistedSyncEnvelope? = null

        override suspend fun store(request: com.skeler.pulse.database.api.StoreEncryptedMessageRequest): MessageStoreResult =
            MessageStoreResult.Failure(SystemError.PersistenceFailure())

        override fun observeConversation(conversationId: String): Flow<List<PersistedMessageEnvelope>> = emptyFlow()

        override fun observeAllMessages(): Flow<List<PersistedMessageEnvelope>> = emptyFlow()

        override suspend fun pendingSync(limit: Int): List<PersistedMessageEnvelope> = messages.take(limit)

        override suspend fun updateSync(
            messageId: String,
            sync: PersistedSyncEnvelope,
        ): MessageStoreResult {
            updatedSync = sync
            return MessageStoreResult.Success(messages.first().copy(sync = sync))
        }
    }

    private class FakeBusinessComplianceStatusStore : BusinessComplianceStatusStore {
        var lastRequest: UpsertBusinessComplianceStatusRequest? = null

        override suspend fun upsertStatus(request: UpsertBusinessComplianceStatusRequest) {
            lastRequest = request
        }
    }

    private class FakeComplianceSyncTransport(
        private val result: ComplianceSyncResult,
    ) : ComplianceSyncTransport {
        override suspend fun fetchStatus(conversationId: String): ComplianceSyncResult = result
    }
}
