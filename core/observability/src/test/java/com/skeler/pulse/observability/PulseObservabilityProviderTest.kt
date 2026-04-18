package com.skeler.pulse.observability

import com.skeler.pulse.contracts.observability.EventName
import com.skeler.pulse.contracts.observability.LogAttribute
import com.skeler.pulse.contracts.observability.LogLevel
import com.skeler.pulse.contracts.observability.ObservabilityEvent
import com.skeler.pulse.contracts.observability.ObservabilityScope
import com.skeler.pulse.contracts.observability.ObservedEvent
import com.skeler.pulse.core.common.ids.IdGenerator
import com.skeler.pulse.core.common.time.TimeProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class PulseObservabilityProviderTest {

    @Test
    fun `logger emits sanitized log event`() = runBlocking {
        val provider = PulseObservabilityProvider(
            moduleName = "core.observability",
            timeProvider = TimeProvider { Instant.parse("2026-04-17T00:00:00Z") },
            idGenerator = IdGenerator { "fixed-id" },
        )
        val scope = ObservabilityScope(
            feature = "security",
            component = "test",
            operation = "log",
        )
        val traceContext = provider.newTraceContext(
            correlationId = "corr-1",
            messageId = "msg-1",
            conversationId = "conv-1",
        )

        val pendingEvent = async { provider.eventStream().first() as ObservabilityEvent.Log }

        provider.logger(scope).log(
            level = LogLevel.INFO,
            event = ObservedEvent(EventName("security.negotiated")),
            traceContext = traceContext,
            attributes = listOf(LogAttribute(key = "plaintext_body", value = "hello")),
        )

        val event = pendingEvent.await()
        assertEquals("redacted", event.attributes.single().value)
        assertEquals("corr-1", event.traceContext.correlationId)
        assertTrue(event.recordedAt == Instant.parse("2026-04-17T00:00:00Z"))
    }
}
