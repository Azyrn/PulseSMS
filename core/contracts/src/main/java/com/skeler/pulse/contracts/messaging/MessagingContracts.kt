package com.skeler.pulse.contracts.messaging

import com.skeler.pulse.contracts.ConversationId
import com.skeler.pulse.contracts.DedupeKey
import com.skeler.pulse.contracts.MessageId
import com.skeler.pulse.contracts.QueueKey
import com.skeler.pulse.contracts.RetryToken
import com.skeler.pulse.contracts.errors.MessagingSurfaceError
import com.skeler.pulse.contracts.errors.NetworkError
import com.skeler.pulse.contracts.protocol.PublicSecurityState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.time.Instant

sealed interface MessagingState {
    val conversationId: ConversationId
    val timeline: MessageTimeline
    val composer: ComposerState
    val sync: ConversationSyncState
    val lastSyncedAt: Instant?
    val complianceUpdatedAt: Instant?
    val surfaceError: MessagingSurfaceError?

    data class Idle(
        override val conversationId: ConversationId,
        override val timeline: MessageTimeline,
        override val composer: ComposerState,
        override val sync: ConversationSyncState = ConversationSyncState.Idle,
        override val lastSyncedAt: Instant? = null,
        override val complianceUpdatedAt: Instant? = null,
        override val surfaceError: MessagingSurfaceError? = null,
    ) : MessagingState

    data class LoadingHistory(
        override val conversationId: ConversationId,
        override val timeline: MessageTimeline,
        override val composer: ComposerState,
        override val sync: ConversationSyncState = ConversationSyncState.Syncing,
        override val lastSyncedAt: Instant? = null,
        override val complianceUpdatedAt: Instant? = null,
        override val surfaceError: MessagingSurfaceError? = null,
        val reason: LoadReason,
    ) : MessagingState

    data class Ready(
        override val conversationId: ConversationId,
        override val timeline: MessageTimeline,
        override val composer: ComposerState,
        override val sync: ConversationSyncState = ConversationSyncState.UpToDate,
        override val lastSyncedAt: Instant? = null,
        override val complianceUpdatedAt: Instant? = null,
        override val surfaceError: MessagingSurfaceError? = null,
    ) : MessagingState

    data class Sending(
        override val conversationId: ConversationId,
        override val timeline: MessageTimeline,
        override val composer: ComposerState,
        override val sync: ConversationSyncState,
        override val lastSyncedAt: Instant? = null,
        override val complianceUpdatedAt: Instant? = null,
        override val surfaceError: MessagingSurfaceError? = null,
        val inFlightMessageId: MessageId,
        val transition: ComposerTransition = ComposerTransition.SendStarted,
    ) : MessagingState

    data class Recovering(
        override val conversationId: ConversationId,
        override val timeline: MessageTimeline,
        override val composer: ComposerState,
        override val sync: ConversationSyncState,
        override val lastSyncedAt: Instant? = null,
        override val complianceUpdatedAt: Instant? = null,
        override val surfaceError: MessagingSurfaceError? = null,
        val retryToken: RetryToken,
    ) : MessagingState

    data class Blocked(
        override val conversationId: ConversationId,
        override val timeline: MessageTimeline,
        override val composer: ComposerState,
        override val sync: ConversationSyncState = ConversationSyncState.Idle,
        override val lastSyncedAt: Instant? = null,
        override val complianceUpdatedAt: Instant? = null,
        override val surfaceError: MessagingSurfaceError? = null,
        val reason: SendBlockReason,
    ) : MessagingState
}

data class MessageTimeline(
    val items: ImmutableList<MessageRenderItem>,
    val ordering: TimelineOrdering = TimelineOrdering.OldestToNewest,
    val anchor: TimelineAnchor? = null,
)

enum class TimelineOrdering {
    OldestToNewest,
    NewestToOldest,
}

data class TimelineAnchor(
    val messageId: MessageId,
    val reason: AnchorReason,
)

enum class AnchorReason {
    RestoreScroll,
    JumpToLatest,
    SendSuccess,
}

data class ComposerState(
    val draft: MessageDraft,
    val eligibility: SendEligibility,
    val focus: ComposerFocus = ComposerFocus.Idle,
    val transition: ComposerTransition? = null,
)

enum class ComposerFocus {
    Idle,
    Focused,
    AttachmentPicker,
}

enum class ComposerTransition {
    SendStarted,
    SendSucceeded,
    SendFailed,
    RecoverySucceeded,
    BlockPresented,
}

sealed interface SendEligibility {
    data object Allowed : SendEligibility
    data class Blocked(
        val reason: SendBlockReason,
    ) : SendEligibility
}

enum class SendBlockReason {
    SenderVerificationPending,
    RecipientVerificationPending,
    TenDlcRegistrationPending,
    MissingIdentityVerification,
    MissingEncryptionMaterial,
    RateLimited,
    Offline,
}

data class MessageRenderItem(
    val id: MessageId,
    val conversationId: ConversationId,
    val direction: MessageDirection,
    val senderDisplayName: String,
    val bodyPreview: String,
    val sentAt: Instant?,
    val receivedAt: Instant?,
    val priority: BusinessPriority,
    val isBusinessVerified: Boolean,
    val status: MessageStatus,
)

data class MessageStatus(
    val delivery: DeliveryIndicator,
    val security: PublicSecurityState,
    val sync: RowSyncState,
)

data class MessageDraft(
    val text: String,
    val attachments: ImmutableList<DraftAttachment>,
    val priority: BusinessPriority,
) {
    companion object {
        fun empty() = MessageDraft(
            text = "",
            attachments = persistentListOf(),
            priority = BusinessPriority.Normal,
        )
    }
}

data class DraftAttachment(
    val id: String,
    val mimeType: String,
    val localUri: String,
)

enum class MessageDirection {
    INBOUND,
    OUTBOUND,
}

enum class BusinessPriority {
    Low,
    Normal,
    High,
    Critical,
}

sealed interface DeliveryIndicator {
    data object Pending : DeliveryIndicator
    data object Queued : DeliveryIndicator
    data object Sent : DeliveryIndicator
    data object Delivered : DeliveryIndicator
    data object Read : DeliveryIndicator
    data class Failed(val error: MessagingSurfaceError) : DeliveryIndicator
}

sealed interface RowSyncState {
    data object Idle : RowSyncState
    data object Syncing : RowSyncState
    data class Failed(val error: NetworkError) : RowSyncState
}

sealed interface ConversationSyncState {
    data object Idle : ConversationSyncState
    data object Syncing : ConversationSyncState
    data object UpToDate : ConversationSyncState
    data class Backoff(val envelope: SyncRecoveryEnvelope) : ConversationSyncState
    data class Conflict(val resolutionId: String) : ConversationSyncState
    data class Failed(val error: NetworkError) : ConversationSyncState
}

data class SyncRecoveryEnvelope(
    val schemaVersion: Int,
    val queueKey: QueueKey,
    val dedupeKey: DedupeKey,
    val attempt: Int,
    val maxAttempts: Int,
    val jitterWindowMillis: Long,
    val nextRetryAt: Instant,
    val lastFailureCode: String?,
)

enum class LoadReason {
    Initial,
    Refresh,
    Pagination,
    Recovery,
}
