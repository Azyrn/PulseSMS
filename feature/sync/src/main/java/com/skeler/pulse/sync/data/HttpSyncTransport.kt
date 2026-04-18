package com.skeler.pulse.sync.data

import com.skeler.pulse.contracts.observability.AttributeClassification
import com.skeler.pulse.contracts.observability.EventName
import com.skeler.pulse.contracts.observability.LogAttribute
import com.skeler.pulse.contracts.observability.LogLevel
import com.skeler.pulse.contracts.observability.ObservabilityProvider
import com.skeler.pulse.contracts.observability.ObservabilityScope
import com.skeler.pulse.contracts.observability.ObservedEvent
import com.skeler.pulse.contracts.persistence.PersistedMessageEnvelope
import com.skeler.pulse.sync.api.SyncTransport
import com.skeler.pulse.sync.api.SyncTransportResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class HttpSyncTransport(
    private val endpointConfig: SyncEndpointConfig,
    private val observabilityProvider: ObservabilityProvider,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : SyncTransport {

    private val scope = ObservabilityScope(
        feature = "sync",
        component = "HttpSyncTransport",
        operation = "send",
    )

    override suspend fun send(
        message: PersistedMessageEnvelope,
    ): SyncTransportResult = withContext(ioDispatcher) {
        if (!endpointConfig.isConfigured) {
            return@withContext SyncTransportResult.PermanentFailure(code = ERROR_UNCONFIGURED)
        }

        val traceContext = observabilityProvider.newTraceContext(
            correlationId = message.sync.dedupeKey,
            messageId = message.messageId,
            conversationId = message.conversationId,
        )
        val endpoint = runCatching { URL(endpointConfig.messagesSyncUrl()) }.getOrElse {
            observabilityProvider.logger(scope).log(
                level = LogLevel.ERROR,
                event = ObservedEvent(EventName("sync.transport_invalid_endpoint")),
                traceContext = traceContext,
                attributes = endpointAttributes(endpointConfig.messagesSyncUrl()),
            )
            return@withContext SyncTransportResult.PermanentFailure(code = ERROR_INVALID_ENDPOINT)
        }

        val connection = (endpoint.openConnection() as? HttpURLConnection)
            ?: return@withContext SyncTransportResult.PermanentFailure(code = ERROR_INVALID_ENDPOINT)

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = endpointConfig.connectTimeoutMillis
            connection.readTimeout = endpointConfig.readTimeoutMillis
            connection.doInput = true
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", JSON_CONTENT_TYPE)
            connection.setRequestProperty("Accept", JSON_CONTENT_TYPE)
            connection.setRequestProperty("X-Pulse-Message-Id", message.messageId)
            connection.setRequestProperty("X-Pulse-Correlation-Id", message.sync.dedupeKey)
            endpointConfig.apiKey?.takeIf { it.isNotBlank() }?.let { apiKey ->
                connection.setRequestProperty("Authorization", "Bearer $apiKey")
            }

            val payload = SyncPayloadJsonEncoder.encode(message)
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(payload)
            }

            val responseCode = connection.responseCode
            val result = responseCode.toTransportResult()
            observabilityProvider.logger(scope).log(
                level = result.toLogLevel(),
                event = ObservedEvent(EventName("sync.transport_response")),
                traceContext = traceContext,
                attributes = endpointAttributes(endpoint.toExternalForm()) + LogAttribute(
                    key = "status_code",
                    value = responseCode.toString(),
                ),
            )
            result
        } catch (_: IOException) {
            observabilityProvider.logger(scope).log(
                level = LogLevel.WARN,
                event = ObservedEvent(EventName("sync.transport_network_failure")),
                traceContext = traceContext,
                attributes = endpointAttributes(endpoint.toExternalForm()),
            )
            SyncTransportResult.RetryableFailure(code = ERROR_NETWORK_IO)
        } finally {
            connection.disconnect()
        }
    }

    private fun endpointAttributes(url: String): List<LogAttribute> = listOf(
        LogAttribute(
            key = "endpoint",
            value = url,
            classification = AttributeClassification.InternalOnly,
        )
    )

    private fun SyncTransportResult.toLogLevel(): LogLevel = when (this) {
        SyncTransportResult.Success -> LogLevel.INFO
        is SyncTransportResult.RetryableFailure -> LogLevel.WARN
        is SyncTransportResult.PermanentFailure -> LogLevel.ERROR
    }

    private companion object {
        const val JSON_CONTENT_TYPE = "application/json; charset=utf-8"
        const val ERROR_INVALID_ENDPOINT = "invalid_endpoint"
        const val ERROR_NETWORK_IO = "network_io"
        const val ERROR_UNCONFIGURED = "transport_unconfigured"
    }
}

internal fun Int.toTransportResult(): SyncTransportResult = when {
    this in 200..299 -> SyncTransportResult.Success
    this == 408 || this == 425 || this == 429 -> SyncTransportResult.RetryableFailure("http_$this")
    this in 500..599 -> SyncTransportResult.RetryableFailure("http_$this")
    else -> SyncTransportResult.PermanentFailure("http_$this")
}
