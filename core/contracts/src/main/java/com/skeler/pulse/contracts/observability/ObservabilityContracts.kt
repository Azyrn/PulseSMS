package com.skeler.pulse.contracts.observability

import com.skeler.pulse.contracts.ConversationId
import com.skeler.pulse.contracts.CorrelationId
import com.skeler.pulse.contracts.MessageId
import com.skeler.pulse.contracts.SpanId
import com.skeler.pulse.contracts.TraceId
import com.skeler.pulse.contracts.errors.DiagnosticError
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface ObservabilityProvider {
    val moduleName: String

    fun logger(scope: ObservabilityScope): StructuredLogger

    fun eventStream(config: EventStreamConfig = EventStreamConfig()): Flow<ObservabilityEvent>

    fun newTraceContext(
        correlationId: CorrelationId,
        messageId: MessageId? = null,
        conversationId: ConversationId? = null,
    ): TraceContext

    fun recordError(
        error: DiagnosticError,
        scope: ObservabilityScope,
        traceContext: TraceContext,
        attributes: List<LogAttribute> = emptyList(),
    )

    fun telemetryHooks(): TelemetryHooks
}

data class EventStreamConfig(
    val capacity: Int = 256,
    val samplingRate: Double = 1.0,
    val deferOnHotPath: Boolean = true,
)

data class TraceContext(
    val traceId: TraceId,
    val spanId: SpanId,
    val correlationId: CorrelationId,
    val messageId: MessageId?,
    val conversationId: ConversationId?,
    val createdAt: Instant,
)

data class ObservabilityScope(
    val feature: String,
    val component: String,
    val operation: String,
)

@JvmInline
value class EventName(val value: String)

data class ObservedEvent(
    val name: EventName,
)

data class LogAttribute(
    val key: String,
    val value: String,
    val classification: AttributeClassification = AttributeClassification.Safe,
)

enum class AttributeClassification {
    Safe,
    Hashed,
    InternalOnly,
}

interface StructuredLogger {
    fun log(
        level: LogLevel,
        event: ObservedEvent,
        traceContext: TraceContext,
        attributes: List<LogAttribute> = emptyList(),
    )
}

sealed interface ObservabilityEvent {
    val scope: ObservabilityScope
    val traceContext: TraceContext
    val recordedAt: Instant

    data class Log(
        override val scope: ObservabilityScope,
        override val traceContext: TraceContext,
        override val recordedAt: Instant,
        val level: LogLevel,
        val event: ObservedEvent,
        val attributes: List<LogAttribute> = emptyList(),
    ) : ObservabilityEvent

    data class Error(
        override val scope: ObservabilityScope,
        override val traceContext: TraceContext,
        override val recordedAt: Instant,
        val error: DiagnosticError,
        val attributes: List<LogAttribute> = emptyList(),
    ) : ObservabilityEvent

    data class Metric(
        override val scope: ObservabilityScope,
        override val traceContext: TraceContext,
        override val recordedAt: Instant,
        val name: String,
        val value: Double,
        val unit: String,
        val attributes: List<LogAttribute> = emptyList(),
    ) : ObservabilityEvent
}

enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR,
}

interface TelemetryHooks {
    fun onSpanStarted(
        name: String,
        traceContext: TraceContext,
        attributes: List<LogAttribute> = emptyList(),
    )

    fun onSpanFinished(
        name: String,
        traceContext: TraceContext,
        attributes: List<LogAttribute> = emptyList(),
    )

    fun onException(
        error: DiagnosticError,
        traceContext: TraceContext,
        attributes: List<LogAttribute> = emptyList(),
    )
}
