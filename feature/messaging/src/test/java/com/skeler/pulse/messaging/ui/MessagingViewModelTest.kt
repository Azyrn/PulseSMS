package com.skeler.pulse.messaging.ui

import com.skeler.pulse.contracts.errors.SystemError
import com.skeler.pulse.contracts.messaging.ConversationSyncState
import com.skeler.pulse.contracts.messaging.MessageDraft
import com.skeler.pulse.contracts.messaging.MessageTimeline
import com.skeler.pulse.contracts.messaging.MessagingState
import com.skeler.pulse.messaging.api.ConversationSnapshot
import com.skeler.pulse.messaging.api.ConversationRepository
import com.skeler.pulse.messaging.api.ConversationSummary
import com.skeler.pulse.messaging.api.SendMessageResult
import com.skeler.pulse.messaging.domain.ObserveConversationUseCase
import com.skeler.pulse.messaging.domain.RequestConversationRefreshUseCase
import com.skeler.pulse.messaging.domain.SendMessageUseCase
import com.skeler.pulse.messaging.model.MessagingIntent
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MessagingViewModelTest {
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
    fun `draft change updates state`() {
        val viewModel = MessagingViewModel(
            initialConversationId = "conv-1",
            observeConversation = ObserveConversationUseCase(FakeConversationRepository()),
            requestConversationRefresh = RequestConversationRefreshUseCase(FakeConversationRepository()),
            sendMessage = SendMessageUseCase(FakeConversationRepository()),
        )

        dispatcher.scheduler.advanceUntilIdle()
        viewModel.accept(MessagingIntent.DraftChanged("hello"))

        assertEquals("hello", viewModel.state.value.composer.draft.text)
    }

    @Test
    fun `failed send keeps state available`() = runTest(dispatcher) {
        val repository = FakeConversationRepository(
            sendResult = SendMessageResult.Failure(SystemError.ValidationFailure()),
        )
        val viewModel = MessagingViewModel(
            initialConversationId = "conv-1",
            observeConversation = ObserveConversationUseCase(repository),
            requestConversationRefresh = RequestConversationRefreshUseCase(repository),
            sendMessage = SendMessageUseCase(repository),
        )

        dispatcher.scheduler.advanceUntilIdle()
        viewModel.accept(MessagingIntent.DraftChanged("hello"))
        viewModel.accept(MessagingIntent.SendPressed)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.surfaceError is SystemError.ValidationFailure)
    }

    @Test
    fun `load conversation preserves backoff sync state`() = runTest(dispatcher) {
        val viewModel = MessagingViewModel(
            initialConversationId = "conv-1",
            observeConversation = ObserveConversationUseCase(
                FakeConversationRepository(
                    snapshot = ConversationSnapshot(
                        timeline = MessageTimeline(items = persistentListOf()),
                        eligibility = com.skeler.pulse.contracts.messaging.SendEligibility.Allowed,
                        syncState = ConversationSyncState.Backoff(
                            envelope = com.skeler.pulse.contracts.messaging.SyncRecoveryEnvelope(
                                schemaVersion = 1,
                                queueKey = "conv-1",
                                dedupeKey = "msg-1",
                                attempt = 1,
                                maxAttempts = 5,
                                jitterWindowMillis = 0L,
                                nextRetryAt = java.time.Instant.ofEpochMilli(5_000L),
                                lastFailureCode = "http_429",
                            )
                        ),
                        lastSyncedAt = null,
                        complianceUpdatedAt = null,
                    )
                )
            ),
            requestConversationRefresh = RequestConversationRefreshUseCase(FakeConversationRepository()),
            sendMessage = SendMessageUseCase(FakeConversationRepository()),
        )

        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.sync is ConversationSyncState.Backoff)
    }

    @Test
    fun `load conversation requests refresh`() = runTest(dispatcher) {
        val repository = FakeConversationRepository()
        val viewModel = MessagingViewModel(
            initialConversationId = "conv-1",
            observeConversation = ObserveConversationUseCase(repository),
            requestConversationRefresh = RequestConversationRefreshUseCase(repository),
            sendMessage = SendMessageUseCase(repository),
        )

        viewModel.accept(MessagingIntent.LoadConversation)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, repository.refreshRequests)
    }

    private class FakeConversationRepository(
        private val snapshot: ConversationSnapshot = ConversationSnapshot(
            timeline = MessageTimeline(items = persistentListOf()),
            eligibility = com.skeler.pulse.contracts.messaging.SendEligibility.Allowed,
            syncState = ConversationSyncState.Idle,
            lastSyncedAt = null,
            complianceUpdatedAt = null,
        ),
        private val sendResult: SendMessageResult = SendMessageResult.Success("msg-1"),
    ) : ConversationRepository {
        var refreshRequests: Int = 0

        override fun observeInbox(): Flow<List<ConversationSummary>> = flowOf(emptyList())

        override fun observeConversation(conversationId: String): Flow<ConversationSnapshot> =
            flowOf(snapshot)

        override suspend fun requestRefresh(conversationId: String) {
            refreshRequests += 1
        }

        override suspend fun sendMessage(
            conversationId: String,
            draft: MessageDraft,
        ): SendMessageResult = sendResult
    }
}
