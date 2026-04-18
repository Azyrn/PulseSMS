package com.skeler.pulse.messaging.model

import com.skeler.pulse.contracts.messaging.ComposerTransition
import com.skeler.pulse.contracts.messaging.ConversationSyncState
import com.skeler.pulse.contracts.messaging.MessageDirection
import com.skeler.pulse.contracts.messaging.MessageRenderItem
import com.skeler.pulse.contracts.messaging.MessageStatus
import com.skeler.pulse.contracts.messaging.MessageTimeline
import com.skeler.pulse.contracts.messaging.MessagingState
import com.skeler.pulse.contracts.messaging.RowSyncState
import com.skeler.pulse.contracts.messaging.SendBlockReason
import com.skeler.pulse.contracts.messaging.SendEligibility
import com.skeler.pulse.contracts.messaging.SyncRecoveryEnvelope
import com.skeler.pulse.messaging.api.ConversationSnapshot
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class MessagingReducerTest {

    @Test
    fun `sending completed clears draft and marks success transition`() {
        val initial = MessagingStateFactory.initial("conv-1")
        val updated = MessagingReducer.reduce(
            MessagingReducer.reduce(initial, MessagingMutation.DraftUpdated("hello")),
            MessagingMutation.SendingCompleted,
        ) as MessagingState.Ready

        assertEquals("", updated.composer.draft.text)
        assertEquals(ComposerTransition.SendSucceeded, updated.composer.transition)
    }

    @Test
    fun `history loaded blocks composer when business verification is pending`() {
        val initial = MessagingStateFactory.initial("business-verification-pending")

        val updated = MessagingReducer.reduce(
            initial,
            MessagingMutation.HistoryLoaded(
                ConversationSnapshot(
                    timeline = MessageTimeline(
                        items = persistentListOf(
                            MessageRenderItem(
                                id = "msg-1",
                                conversationId = "business-verification-pending",
                                direction = MessageDirection.OUTBOUND,
                                senderDisplayName = "Pulse Business",
                                bodyPreview = "Encrypted message",
                                sentAt = null,
                                receivedAt = null,
                                priority = com.skeler.pulse.contracts.messaging.BusinessPriority.Normal,
                                isBusinessVerified = false,
                                status = MessageStatus(
                                    delivery = com.skeler.pulse.contracts.messaging.DeliveryIndicator.Sent,
                                    security = com.skeler.pulse.contracts.protocol.PublicSecurityState.Protected,
                                    sync = RowSyncState.Idle,
                                ),
                            )
                        )
                    ),
                    eligibility = SendEligibility.Blocked(SendBlockReason.SenderVerificationPending),
                    syncState = ConversationSyncState.UpToDate,
                    lastSyncedAt = Instant.EPOCH,
                    complianceUpdatedAt = Instant.EPOCH,
                )
            ),
        ) as MessagingState.Ready

        val eligibility = updated.composer.eligibility as SendEligibility.Blocked
        assertEquals(SendBlockReason.SenderVerificationPending, eligibility.reason)
    }

    @Test
    fun `history loaded blocks composer when rate limited backoff is active`() {
        val initial = MessagingStateFactory.initial("business-primary")

        val updated = MessagingReducer.reduce(
            initial,
            MessagingMutation.HistoryLoaded(
                ConversationSnapshot(
                    timeline = MessageTimeline(items = persistentListOf()),
                    eligibility = SendEligibility.Blocked(SendBlockReason.RateLimited),
                    syncState = ConversationSyncState.Backoff(
                        SyncRecoveryEnvelope(
                            schemaVersion = 1,
                            queueKey = "business-primary",
                            dedupeKey = "msg-1",
                            attempt = 1,
                            maxAttempts = 5,
                            jitterWindowMillis = 0L,
                            nextRetryAt = Instant.ofEpochMilli(5_000L),
                            lastFailureCode = "http_429",
                        )
                    ),
                    lastSyncedAt = Instant.EPOCH,
                    complianceUpdatedAt = Instant.EPOCH,
                )
            ),
        ) as MessagingState.Ready

        val eligibility = updated.composer.eligibility as SendEligibility.Blocked
        assertEquals(SendBlockReason.RateLimited, eligibility.reason)
    }
}
