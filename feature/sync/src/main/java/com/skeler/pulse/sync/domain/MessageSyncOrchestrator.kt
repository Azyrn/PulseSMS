package com.skeler.pulse.sync.domain

import com.skeler.pulse.contracts.ConversationId
import com.skeler.pulse.contracts.errors.SystemError
import com.skeler.pulse.contracts.observability.EventName
import com.skeler.pulse.contracts.observability.LogLevel
import com.skeler.pulse.contracts.observability.ObservabilityProvider
import com.skeler.pulse.contracts.observability.ObservabilityScope
import com.skeler.pulse.contracts.observability.ObservedEvent
import com.skeler.pulse.contracts.persistence.PersistedSyncEnvelope
import com.skeler.pulse.core.common.time.SystemTimeProvider
import com.skeler.pulse.core.common.time.TimeProvider
import com.skeler.pulse.database.api.BusinessComplianceStatusStore
import com.skeler.pulse.database.api.UpsertBusinessComplianceStatusRequest
import com.skeler.pulse.database.api.EncryptedMessageStore
import com.skeler.pulse.database.api.MessageStoreResult
import com.skeler.pulse.sync.api.ComplianceSyncResult
import com.skeler.pulse.sync.api.ComplianceSyncTransport
import com.skeler.pulse.sync.api.SyncTransport
import com.skeler.pulse.sync.api.SyncTransportResult

class MessageSyncOrchestrator(
    private val businessComplianceStatusStore: BusinessComplianceStatusStore,
    private val complianceTransport: ComplianceSyncTransport,
    private val encryptedMessageStore: EncryptedMessageStore,
    private val transport: SyncTransport,
    private val retryPolicy: SyncRetryPolicy,
    private val observabilityProvider: ObservabilityProvider,
    private val timeProvider: TimeProvider = SystemTimeProvider(),
) {
    private val scope = ObservabilityScope(
        feature = "sync",
        component = "MessageSyncOrchestrator",
        operation = "run",
    )

    suspend fun run(
        conversationId: ConversationId,
        batchSize: Int = DEFAULT_BATCH_SIZE,
    ): SyncRunResult {
        val pending = encryptedMessageStore.pendingSync(conversationId, batchSize)

        var synced = 0
        var retried = 0
        var failed = 0
        var nextRetryAtEpochMillis: Long? = null

        pending.forEach { envelope ->
            val traceContext = observabilityProvider.newTraceContext(
                correlationId = envelope.sync.dedupeKey,
                messageId = envelope.messageId,
                conversationId = envelope.conversationId,
            )
            when (val result = transport.send(envelope)) {
                SyncTransportResult.Success -> {
                    encryptedMessageStore.updateSync(
                        messageId = envelope.messageId,
                        sync = envelope.sync.copy(
                            attempt = envelope.sync.attempt,
                            nextRetryAtEpochMillis = null,
                            lastFailureCode = null,
                            completedAtEpochMillis = timeProvider.now().toEpochMilli(),
                        ),
                    )
                    observabilityProvider.logger(scope).log(
                        level = LogLevel.INFO,
                        event = ObservedEvent(EventName("sync.message_synced")),
                        traceContext = traceContext,
                    )
                    synced += 1
                }

                is SyncTransportResult.RetryableFailure -> {
                    val nextAttempt = envelope.sync.attempt + 1
                    if (nextAttempt >= envelope.sync.maxAttempts) {
                        encryptedMessageStore.updateSync(
                            messageId = envelope.messageId,
                            sync = envelope.sync.copy(
                                attempt = nextAttempt,
                                lastFailureCode = result.code,
                                nextRetryAtEpochMillis = null,
                                completedAtEpochMillis = null,
                            ),
                        )
                        observabilityProvider.logger(scope).log(
                            level = LogLevel.ERROR,
                            event = ObservedEvent(EventName("sync.message_failed")),
                            traceContext = traceContext,
                        )
                        failed += 1
                    } else {
                        val nextRetryAt = retryPolicy.nextRetryAt(nextAttempt, timeProvider.now())
                        val nextRetryAtMillis = nextRetryAt.toEpochMilli()
                        nextRetryAtEpochMillis = minOfNullable(nextRetryAtEpochMillis, nextRetryAtMillis)
                        encryptedMessageStore.updateSync(
                            messageId = envelope.messageId,
                            sync = PersistedSyncEnvelope(
                                schemaVersion = envelope.sync.schemaVersion,
                                queueKey = envelope.sync.queueKey,
                                dedupeKey = envelope.sync.dedupeKey,
                                attempt = nextAttempt,
                                maxAttempts = envelope.sync.maxAttempts,
                                nextRetryAtEpochMillis = nextRetryAtMillis,
                                lastFailureCode = result.code,
                                completedAtEpochMillis = null,
                            ),
                        )
                        observabilityProvider.logger(scope).log(
                            level = LogLevel.WARN,
                            event = ObservedEvent(EventName("sync.message_retry_scheduled")),
                            traceContext = traceContext,
                        )
                        retried += 1
                    }
                }

                is SyncTransportResult.PermanentFailure -> {
                    encryptedMessageStore.updateSync(
                        messageId = envelope.messageId,
                        sync = envelope.sync.copy(
                            lastFailureCode = result.code,
                            nextRetryAtEpochMillis = null,
                            completedAtEpochMillis = null,
                        ),
                    )
                    observabilityProvider.logger(scope).log(
                        level = LogLevel.ERROR,
                        event = ObservedEvent(EventName("sync.message_failed")),
                        traceContext = traceContext,
                    )
                    failed += 1
                }
            }
        }

        val complianceNeedsRetry = when (val result = complianceTransport.fetchStatus(conversationId)) {
            is ComplianceSyncResult.Success -> {
                businessComplianceStatusStore.upsertStatus(
                    UpsertBusinessComplianceStatusRequest(
                        conversationId = conversationId,
                        schemaVersion = 1,
                        status = result.status.copy(
                            updatedAt = result.status.updatedAt ?: timeProvider.now(),
                        ),
                        updatedAt = timeProvider.now(),
                    )
                )
                observabilityProvider.logger(scope).log(
                    level = LogLevel.INFO,
                    event = ObservedEvent(EventName("sync.compliance_synced")),
                    traceContext = observabilityProvider.newTraceContext(
                        correlationId = "compliance:$conversationId",
                        conversationId = conversationId,
                    ),
                )
                false
            }

            is ComplianceSyncResult.RetryableFailure -> {
                observabilityProvider.logger(scope).log(
                    level = LogLevel.WARN,
                    event = ObservedEvent(EventName("sync.compliance_retry_scheduled")),
                    traceContext = observabilityProvider.newTraceContext(
                        correlationId = "compliance:$conversationId",
                        conversationId = conversationId,
                    ),
                )
                true
            }

            is ComplianceSyncResult.PermanentFailure -> {
                observabilityProvider.logger(scope).log(
                    level = LogLevel.ERROR,
                    event = ObservedEvent(EventName("sync.compliance_failed")),
                    traceContext = observabilityProvider.newTraceContext(
                        correlationId = "compliance:$conversationId",
                        conversationId = conversationId,
                    ),
                )
                false
            }
        }

        return if (failed == 0 && !complianceNeedsRetry) {
            SyncRunResult.Success(
                synced = synced,
                retried = retried,
                nextRetryAtEpochMillis = nextRetryAtEpochMillis,
                needsComplianceRetry = false,
            )
        } else {
            SyncRunResult.PartialFailure(
                synced = synced,
                retried = retried,
                failed = failed + if (complianceNeedsRetry) 1 else 0,
                nextRetryAtEpochMillis = nextRetryAtEpochMillis,
                needsComplianceRetry = complianceNeedsRetry,
            )
        }
    }

    companion object {
        private const val DEFAULT_BATCH_SIZE = 50
    }
}

private fun minOfNullable(current: Long?, candidate: Long): Long =
    current?.let { minOf(it, candidate) } ?: candidate

sealed interface SyncRunResult {
    data class Success(
        val synced: Int,
        val retried: Int,
        val nextRetryAtEpochMillis: Long? = null,
        val needsComplianceRetry: Boolean = false,
    ) : SyncRunResult

    data class PartialFailure(
        val synced: Int,
        val retried: Int,
        val failed: Int,
        val nextRetryAtEpochMillis: Long? = null,
        val needsComplianceRetry: Boolean = false,
    ) : SyncRunResult

    data class Failure(
        val error: SystemError,
    ) : SyncRunResult
}
