package com.skeler.pulse.observability

import com.skeler.pulse.contracts.observability.AttributeClassification
import com.skeler.pulse.contracts.observability.EventStreamConfig
import com.skeler.pulse.contracts.observability.LogAttribute
import com.skeler.pulse.contracts.observability.LogLevel
import com.skeler.pulse.contracts.observability.ObservabilityEvent
import com.skeler.pulse.contracts.observability.ObservabilityProvider
import com.skeler.pulse.contracts.observability.ObservabilityScope
import com.skeler.pulse.contracts.observability.ObservedEvent
import com.skeler.pulse.contracts.observability.StructuredLogger
import com.skeler.pulse.contracts.observability.TelemetryHooks
import com.skeler.pulse.contracts.observability.TraceContext
import com.skeler.pulse.contracts.errors.DiagnosticError
import com.skeler.pulse.core.common.ids.IdGenerator
import com.skeler.pulse.core.common.ids.UuidIdGenerator
import com.skeler.pulse.core.common.time.SystemTimeProvider
import com.skeler.pulse.core.common.time.TimeProvider
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.time.Instant

class PulseObservabilityProvider(
    override val moduleName: String,
    private val timeProvider: TimeProvider = SystemTimeProvider(),
    private val idGenerator: IdGenerator = UuidIdGenerator(),
    private val telemetryHooks: TelemetryHooks = NoOpTelemetryHooks,
    private val bufferCapacity: Int = 256,
) : ObservabilityProvider {

    private val events = MutableSharedFlow<ObservabilityEvent>(
        replay = 1,
        extraBufferCapacity = bufferCapacity,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override fun logger(scope: ObservabilityScope): StructuredLogger = PulseStructuredLogger(scope)

    override fun eventStream(config: EventStreamConfig): Flow<ObservabilityEvent> = events

    override fun newTraceContext(
        correlationId: String,
        messageId: String?,
        conversationId: String?,
    ): TraceContext = TraceContext(
        traceId = idGenerator.newId(),
        spanId = idGenerator.newId(),
        correlationId = correlationId,
        messageId = messageId,
        conversationId = conversationId,
        createdAt = timeProvider.now(),
    )

    override fun recordError(
        error: DiagnosticError,
        scope: ObservabilityScope,
        traceContext: TraceContext,
        attributes: List<LogAttribute>,
    ) {
        events.tryEmit(
            ObservabilityEvent.Error(
                scope = scope,
                traceContext = traceContext,
                recordedAt = timeProvider.now(),
                error = error,
                attributes = sanitize(attributes),
            )
        )
    }

    override fun telemetryHooks(): TelemetryHooks = telemetryHooks

    private inner class PulseStructuredLogger(
        private val scope: ObservabilityScope,
    ) : StructuredLogger {
        override fun log(
            level: LogLevel,
            event: ObservedEvent,
            traceContext: TraceContext,
            attributes: List<LogAttribute>,
        ) {
            events.tryEmit(
                ObservabilityEvent.Log(
                    scope = scope,
                    traceContext = traceContext,
                    recordedAt = timeProvider.now(),
                    level = level,
                    event = event,
                    attributes = sanitize(attributes),
                )
            )
        }
    }

    private fun sanitize(attributes: List<LogAttribute>): List<LogAttribute> =
        attributes.map { attribute ->
            if (attribute.classification == AttributeClassification.Safe && attribute.key.isSensitiveKey()) {
                attribute.copy(
                    value = REDACTED,
                    classification = AttributeClassification.InternalOnly,
                )
            } else {
                attribute
            }
        }

    private fun String.isSensitiveKey(): Boolean {
        val normalized = lowercase()
        return normalized.contains("plaintext") ||
            normalized.contains("cipher") ||
            normalized.contains("secret") ||
            normalized.contains("body") ||
            normalized.contains("phone") ||
            normalized.contains("key_material")
    }

    private companion object {
        const val REDACTED = "redacted"
    }
}

object NoOpTelemetryHooks : TelemetryHooks {
    override fun onSpanStarted(
        name: String,
        traceContext: TraceContext,
        attributes: List<LogAttribute>,
    ) = Unit

    override fun onSpanFinished(
        name: String,
        traceContext: TraceContext,
        attributes: List<LogAttribute>,
    ) = Unit

    override fun onException(
        error: DiagnosticError,
        traceContext: TraceContext,
        attributes: List<LogAttribute>,
    ) = Unit
}
