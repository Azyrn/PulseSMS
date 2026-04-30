package com.skeler.pulse.messaging.integration

import com.skeler.pulse.contracts.errors.SystemError
import com.skeler.pulse.contracts.messaging.ConversationSyncState
import com.skeler.pulse.contracts.persistence.PayloadStoragePolicy
import com.skeler.pulse.contracts.persistence.PersistedMessageEnvelope
import com.skeler.pulse.contracts.persistence.PersistedSyncEnvelope
import com.skeler.pulse.core.common.time.TimeProvider
import com.skeler.pulse.database.api.BusinessComplianceStatusStore
import com.skeler.pulse.database.api.EncryptedMessageStore
import com.skeler.pulse.database.api.MessageStoreResult
import com.skeler.pulse.database.api.StoreEncryptedMessageRequest
import com.skeler.pulse.database.api.UpsertBusinessComplianceStatusRequest
import com.skeler.pulse.messaging.data.DefaultConversationRepository
import com.skeler.pulse.messaging.domain.ObserveConversationUseCase
import com.skeler.pulse.messaging.domain.RequestConversationRefreshUseCase
import com.skeler.pulse.messaging.domain.SendMessageUseCase
import com.skeler.pulse.messaging.model.MessagingIntent
import com.skeler.pulse.messaging.ui.MessagingViewModel
import com.skeler.pulse.observability.PulseObservabilityProvider
import com.skeler.pulse.security.api.MessageProtector
import com.skeler.pulse.security.api.ProtectMessageRequest
import com.skeler.pulse.security.api.ProtectionResult
import com.skeler.pulse.security.data.StaticBusinessComplianceProvider
import com.skeler.pulse.security.api.BusinessComplianceStatus
import com.skeler.pulse.sync.api.SyncTransport
import com.skeler.pulse.sync.api.SyncTransportResult
import com.skeler.pulse.sync.api.ComplianceSyncResult
import com.skeler.pulse.sync.api.ComplianceSyncTransport
import com.skeler.pulse.sync.domain.MessageSyncOrchestrator
import com.skeler.pulse.sync.domain.SyncRetryPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MessagingSyncBackoffIntegrationTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `retry scheduled by sync updates messaging ui backoff state`() = runTest(dispatcher) {
        val store = MutableEncryptedMessageStore(
            initialEnvelopes = listOf(
                PersistedMessageEnvelope(
                    schemaVersion = 1,
                    messageId = "msg-1",
                    conversationId = "conv-1",
                    bodyCiphertext = "cipher",
                    bodyKeyAlias = "alias",
                    bodyInitializationVector = "iv",
                    bodyPreview = "",
                    payloadStoragePolicy = PayloadStoragePolicy.CiphertextOnly,
                    sentAtEpochMillis = 1_000L,
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
        )
        val repository = DefaultConversationRepository(
            encryptedMessageStore = store,
            messageProtector = UnusedMessageProtector,
            businessComplianceProvider = StaticBusinessComplianceProvider(
                statuses = mapOf("conv-1" to BusinessComplianceStatus())
            ),
        )
        val viewModel = MessagingViewModel(
            initialConversationId = "conv-1",
            observeConversation = ObserveConversationUseCase(repository),
            requestConversationRefresh = RequestConversationRefreshUseCase(repository),
            sendMessage = SendMessageUseCase(repository),
        )
        val orchestrator = MessageSyncOrchestrator(
            businessComplianceStatusStore = NoOpBusinessComplianceStatusStore,
            complianceTransport = object : ComplianceSyncTransport {
                override suspend fun fetchStatus(conversationId: String): ComplianceSyncResult =
                    ComplianceSyncResult.Success(BusinessComplianceStatus())
            },
            encryptedMessageStore = store,
            transport = object : SyncTransport {
                override suspend fun send(message: PersistedMessageEnvelope): SyncTransportResult =
                    SyncTransportResult.RetryableFailure("http_429")
            },
            retryPolicy = SyncRetryPolicy(
                baseDelayMillis = 1_000L,
                jitterRatio = 0.0,
            ),
            observabilityProvider = PulseObservabilityProvider("test.integration"),
            timeProvider = TimeProvider { Instant.ofEpochMilli(10_000L) },
        )

        viewModel.accept(MessagingIntent.LoadConversation)
        dispatcher.scheduler.advanceUntilIdle()

        orchestrator.run("conv-1")
        dispatcher.scheduler.advanceUntilIdle()

        val syncState = viewModel.state.value.sync as ConversationSyncState.Backoff
        assertEquals("http_429", syncState.envelope.lastFailureCode)
        assertEquals(11_000L, syncState.envelope.nextRetryAt.toEpochMilli())
        assertEquals(1, syncState.envelope.attempt)
        assertTrue(viewModel.state.value.lastSyncedAt != null)
    }

    private class MutableEncryptedMessageStore(
        initialEnvelopes: List<PersistedMessageEnvelope>,
    ) : EncryptedMessageStore {
        private val state = MutableStateFlow(initialEnvelopes)

        override suspend fun store(request: StoreEncryptedMessageRequest): MessageStoreResult =
            MessageStoreResult.Failure(SystemError.PersistenceFailure())

        override fun observeConversation(conversationId: String): Flow<List<PersistedMessageEnvelope>> =
            state.map { envelopes -> envelopes.filter { it.conversationId == conversationId } }

        override fun observeAllMessages(): Flow<List<PersistedMessageEnvelope>> = state

        override suspend fun pendingSync(limit: Int): List<PersistedMessageEnvelope> =
            state.value.take(limit)

        override suspend fun updateSync(
            messageId: String,
            sync: PersistedSyncEnvelope,
        ): MessageStoreResult {
            val updated = state.value.map { envelope ->
                if (envelope.messageId == messageId) {
                    envelope.copy(sync = sync)
                } else {
                    envelope
                }
            }
            state.value = updated
            return MessageStoreResult.Success(updated.first { it.messageId == messageId })
        }
    }

    private data object UnusedMessageProtector : MessageProtector {
        override suspend fun protect(request: ProtectMessageRequest): ProtectionResult =
            error("Not used in sync backoff integration test")
    }

    private data object NoOpBusinessComplianceStatusStore : BusinessComplianceStatusStore {
        override suspend fun upsertStatus(request: UpsertBusinessComplianceStatusRequest) = Unit
    }
}
