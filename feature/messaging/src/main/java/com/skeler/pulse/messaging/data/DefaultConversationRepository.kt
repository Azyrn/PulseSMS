package com.skeler.pulse.messaging.data

import com.skeler.pulse.contracts.ConversationId
import com.skeler.pulse.contracts.messaging.BusinessPriority
import com.skeler.pulse.contracts.messaging.ConversationSyncState
import com.skeler.pulse.contracts.messaging.DeliveryIndicator
import com.skeler.pulse.contracts.messaging.MessageDirection
import com.skeler.pulse.contracts.messaging.MessageDraft
import com.skeler.pulse.contracts.messaging.MessageRenderItem
import com.skeler.pulse.contracts.messaging.MessageStatus
import com.skeler.pulse.contracts.messaging.MessageTimeline
import com.skeler.pulse.contracts.messaging.RowSyncState
import com.skeler.pulse.contracts.messaging.SendBlockReason
import com.skeler.pulse.contracts.messaging.SendEligibility
import com.skeler.pulse.contracts.messaging.SyncRecoveryEnvelope
import com.skeler.pulse.contracts.errors.NetworkError
import com.skeler.pulse.contracts.errors.SystemError
import com.skeler.pulse.contracts.persistence.PayloadStoragePolicy
import com.skeler.pulse.contracts.persistence.PersistedMessageEnvelope
import com.skeler.pulse.contracts.persistence.PersistedSyncEnvelope
import com.skeler.pulse.core.common.ids.IdGenerator
import com.skeler.pulse.core.common.ids.UuidIdGenerator
import com.skeler.pulse.database.api.EncryptedMessageStore
import com.skeler.pulse.database.api.MessageStoreResult
import com.skeler.pulse.database.api.StoreEncryptedMessageRequest
import com.skeler.pulse.messaging.api.ConversationSnapshot
import com.skeler.pulse.messaging.api.ConversationRepository
import com.skeler.pulse.messaging.api.ConversationSummary
import com.skeler.pulse.messaging.api.SendMessageResult
import com.skeler.pulse.security.api.BusinessComplianceProvider
import com.skeler.pulse.security.api.BusinessComplianceStatus
import com.skeler.pulse.security.api.MessageProtector
import com.skeler.pulse.security.api.ProtectMessageRequest
import com.skeler.pulse.sync.api.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.map
import java.time.Instant

class DefaultConversationRepository(
    private val encryptedMessageStore: EncryptedMessageStore,
    private val messageProtector: MessageProtector,
    private val businessComplianceProvider: BusinessComplianceProvider,
    private val syncScheduler: SyncScheduler? = null,
    private val idGenerator: IdGenerator = UuidIdGenerator(),
) : ConversationRepository {

    override fun observeInbox(): Flow<List<ConversationSummary>> =
        encryptedMessageStore.observeAllMessages().map { envelopes ->
            envelopes
                .groupBy(PersistedMessageEnvelope::conversationId)
                .values
                .map { conversationEnvelopes ->
                    val sorted = conversationEnvelopes.sortedWith(
                        compareByDescending<PersistedMessageEnvelope> { envelope -> envelope.primaryTimestamp() }
                            .thenByDescending(PersistedMessageEnvelope::messageId)
                    )
                    val latest = sorted.first()
                    ConversationSummary(
                        conversationId = latest.conversationId,
                        snippet = latest.previewLabel(),
                        timestamp = latest.primaryTimestamp(),
                        messageCount = sorted.size,
                        syncState = sorted.toConversationSyncState(),
                    )
                }
                .sortedWith(compareByDescending<ConversationSummary> { it.timestamp }.thenBy { it.conversationId })
        }

    override fun observeConversation(conversationId: ConversationId): Flow<ConversationSnapshot> =
        combine(
            encryptedMessageStore.observeConversation(conversationId),
            businessComplianceProvider.observeStatus(conversationId),
        ) { envelopes, complianceStatus ->
            val syncState = envelopes.toConversationSyncState()
            ConversationSnapshot(
                timeline = MessageTimeline(
                    items = envelopes.map { envelope -> envelope.toRenderItem(complianceStatus) }.toImmutableList(),
                ),
                eligibility = complianceStatus.toEligibility(syncState),
                syncState = syncState,
                lastSyncedAt = envelopes.latestObservedAt(),
                complianceUpdatedAt = complianceStatus.updatedAt,
            )
        }

    override suspend fun requestRefresh(conversationId: ConversationId) {
        syncScheduler?.enqueueConversationSync(conversationId)
    }

    override suspend fun sendMessage(
        conversationId: ConversationId,
        draft: MessageDraft,
    ): SendMessageResult {
        if (draft.text.isBlank()) {
            return SendMessageResult.Failure(
                error = com.skeler.pulse.contracts.errors.SystemError.ValidationFailure(
                    message = "Draft text cannot be empty",
                )
            )
        }
        val complianceStatus = businessComplianceProvider.currentStatus(conversationId)
        val eligibility = complianceStatus.toEligibility(syncState = ConversationSyncState.UpToDate)
        if (eligibility is SendEligibility.Blocked) {
            return SendMessageResult.Failure(
                error = SystemError.ValidationFailure(
                    message = eligibility.reason.toBlockedSendMessage(),
                )
            )
        }

        val messageId = idGenerator.newId()
        return when (val protectedPayload = messageProtector.protect(
            ProtectMessageRequest(
                conversationId = conversationId,
                correlationId = messageId,
                plaintext = draft.text.encodeToByteArray(),
            )
        )) {
            is com.skeler.pulse.security.api.ProtectionResult.Failure ->
                SendMessageResult.Failure(protectedPayload.error)

            is com.skeler.pulse.security.api.ProtectionResult.Success -> {
                val storePolicy = PayloadStoragePolicy.CiphertextOnly
                when (val stored = encryptedMessageStore.store(
                    StoreEncryptedMessageRequest(
                        schemaVersion = 1,
                        messageId = messageId,
                        conversationId = conversationId,
                        encryptedPayload = protectedPayload.payload,
                        bodyPreview = "",
                        payloadStoragePolicy = storePolicy,
                        sentAt = protectedPayload.payload.encryptedAt,
                        receivedAt = null,
                        sync = PersistedSyncEnvelope(
                            schemaVersion = 1,
                            queueKey = conversationId,
                            dedupeKey = messageId,
                            attempt = 0,
                            maxAttempts = 5,
                            nextRetryAtEpochMillis = null,
                            lastFailureCode = null,
                            completedAtEpochMillis = null,
                        ),
                    )
                )) {
                    is MessageStoreResult.Failure -> SendMessageResult.Failure(stored.error)
                    is MessageStoreResult.Success -> {
                        syncScheduler?.enqueueConversationSync(conversationId)
                        SendMessageResult.Success(messageId)
                    }
                }
            }
        }
    }

    private fun PersistedMessageEnvelope.toRenderItem(
        complianceStatus: BusinessComplianceStatus,
    ): MessageRenderItem = MessageRenderItem(
        id = messageId,
        conversationId = conversationId,
        direction = MessageDirection.OUTBOUND,
        senderDisplayName = "Pulse Business",
        bodyPreview = previewLabel(),
        sentAt = sentAtEpochMillis?.let(Instant::ofEpochMilli),
        receivedAt = receivedAtEpochMillis?.let(Instant::ofEpochMilli),
        priority = BusinessPriority.Normal,
        isBusinessVerified = complianceStatus.senderVerified && complianceStatus.identityVerified,
        status = MessageStatus(
            delivery = toDeliveryIndicator(),
            security = com.skeler.pulse.contracts.protocol.PublicSecurityState.Protected,
            sync = toRowSyncState(),
        ),
    )

    private fun PersistedMessageEnvelope.toDeliveryIndicator(): DeliveryIndicator =
        when {
            sync.lastFailureCode != null && sync.nextRetryAtEpochMillis == null -> {
                DeliveryIndicator.Failed(
                    error = sync.toNetworkError(),
                )
            }

            sync.completedAtEpochMillis != null && receivedAtEpochMillis != null -> DeliveryIndicator.Delivered
            sync.completedAtEpochMillis != null -> DeliveryIndicator.Sent
            sync.nextRetryAtEpochMillis != null -> DeliveryIndicator.Queued
            else -> DeliveryIndicator.Pending
        }

    private fun PersistedMessageEnvelope.toRowSyncState(): RowSyncState =
        when {
            sync.lastFailureCode != null && sync.nextRetryAtEpochMillis == null -> {
                RowSyncState.Failed(
                    error = sync.toNetworkError(),
                )
            }

            sync.completedAtEpochMillis != null -> RowSyncState.Idle
            else -> RowSyncState.Syncing
        }

    private fun List<PersistedMessageEnvelope>.toConversationSyncState(): ConversationSyncState {
        val backoffEnvelope = maxByOrNull { it.sync.nextRetryAtEpochMillis ?: Long.MIN_VALUE }
            ?.takeIf { it.sync.nextRetryAtEpochMillis != null }
        if (backoffEnvelope != null) {
            return ConversationSyncState.Backoff(backoffEnvelope.toSyncRecoveryEnvelope())
        }

        val failureEnvelope = lastOrNull { it.sync.lastFailureCode != null }
        if (failureEnvelope != null) {
            return ConversationSyncState.Failed(failureEnvelope.sync.toNetworkError())
        }

        return if (isEmpty()) {
            ConversationSyncState.Idle
        } else if (any { it.sync.completedAtEpochMillis == null }) {
            ConversationSyncState.Syncing
        } else {
            ConversationSyncState.UpToDate
        }
    }

    private fun List<PersistedMessageEnvelope>.latestObservedAt(): Instant? =
        maxOfOrNull { envelope ->
            maxOf(
                envelope.sentAtEpochMillis ?: Long.MIN_VALUE,
                envelope.receivedAtEpochMillis ?: Long.MIN_VALUE,
                envelope.sync.completedAtEpochMillis ?: Long.MIN_VALUE,
            )
        }?.takeIf { it != Long.MIN_VALUE }
            ?.let(Instant::ofEpochMilli)

    private fun PersistedMessageEnvelope.toSyncRecoveryEnvelope(): SyncRecoveryEnvelope =
        SyncRecoveryEnvelope(
            schemaVersion = sync.schemaVersion,
            queueKey = sync.queueKey,
            dedupeKey = sync.dedupeKey,
            attempt = sync.attempt,
            maxAttempts = sync.maxAttempts,
            jitterWindowMillis = 0L,
            nextRetryAt = Instant.ofEpochMilli(sync.nextRetryAtEpochMillis ?: 0L),
            lastFailureCode = sync.lastFailureCode,
        )

    private fun PersistedSyncEnvelope.toNetworkError(): NetworkError = when (lastFailureCode) {
        "http_408" -> NetworkError.Timeout(
            message = "Sync request timed out before the gateway responded",
        )

        "http_401", "http_403" -> NetworkError.Unreachable(
            message = "Sync gateway rejected Pulse credentials",
        )

        "http_429" -> NetworkError.RateLimited(
            retryAt = nextRetryAtEpochMillis?.let(Instant::ofEpochMilli)
                ?: Instant.EPOCH,
            message = "Carrier or gateway rate limit reached",
        )

        "http_500", "http_502", "http_503", "http_504", "network_io" -> NetworkError.Unreachable(
            message = "Sync gateway is currently unreachable",
        )

        "invalid_endpoint" -> NetworkError.Unreachable(
            message = "Sync endpoint URL is invalid",
        )

        "transport_unconfigured" -> NetworkError.Unreachable(
            message = "Sync gateway is not configured on this build",
        )

        "missing_ciphertext" -> NetworkError.Unreachable(
            message = "Sync payload was rejected before dispatch",
        )

        null -> NetworkError.Unreachable()

        else -> NetworkError.Unreachable(
            message = "Sync failed: $lastFailureCode",
        )
    }

    private fun BusinessComplianceStatus.toEligibility(
        syncState: ConversationSyncState,
    ): SendEligibility = when {
        !senderVerified -> SendEligibility.Blocked(SendBlockReason.SenderVerificationPending)
        !recipientVerified -> SendEligibility.Blocked(SendBlockReason.RecipientVerificationPending)
        !identityVerified -> SendEligibility.Blocked(SendBlockReason.MissingIdentityVerification)
        !tenDlcRegistered -> SendEligibility.Blocked(SendBlockReason.TenDlcRegistrationPending)
        syncState is ConversationSyncState.Backoff && syncState.envelope.lastFailureCode == "http_429" ->
            SendEligibility.Blocked(SendBlockReason.RateLimited)
        else -> SendEligibility.Allowed
    }

    private fun SendBlockReason.toBlockedSendMessage(): String = when (this) {
        SendBlockReason.SenderVerificationPending -> "Business sender verification is still pending"
        SendBlockReason.RecipientVerificationPending -> "Recipient verification is still pending"
        SendBlockReason.TenDlcRegistrationPending -> "10DLC registration is still pending"
        SendBlockReason.MissingIdentityVerification -> "Business identity verification is still pending"
        SendBlockReason.MissingEncryptionMaterial -> "Secure messaging material is unavailable"
        SendBlockReason.RateLimited -> "Outbound business messaging is temporarily rate limited"
        SendBlockReason.Offline -> "Outbound business messaging is offline"
    }

    private fun PersistedMessageEnvelope.primaryTimestamp(): Instant? =
        listOfNotNull(
            sync.completedAtEpochMillis,
            receivedAtEpochMillis,
            sentAtEpochMillis,
        ).maxOrNull()?.let(Instant::ofEpochMilli)

    private fun PersistedMessageEnvelope.previewLabel(): String =
        bodyPreview.ifBlank { "Encrypted message" }

    private fun String.toBodyPreview(maxLength: Int = 120): String {
        val normalized = trim().replace("\\s+".toRegex(), " ")
        if (normalized.length <= maxLength) {
            return normalized
        }
        return normalized.take(maxLength - 1).trimEnd() + "…"
    }

}
