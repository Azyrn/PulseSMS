package com.skeler.pulse.sync.data

import com.skeler.pulse.contracts.ConversationId
import com.skeler.pulse.contracts.observability.AttributeClassification
import com.skeler.pulse.contracts.observability.EventName
import com.skeler.pulse.contracts.observability.LogAttribute
import com.skeler.pulse.contracts.observability.LogLevel
import com.skeler.pulse.contracts.observability.ObservabilityProvider
import com.skeler.pulse.contracts.observability.ObservabilityScope
import com.skeler.pulse.contracts.observability.ObservedEvent
import com.skeler.pulse.sync.api.ComplianceSyncResult
import com.skeler.pulse.sync.api.ComplianceSyncTransport
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class HttpComplianceSyncTransport(
    private val endpointConfig: SyncEndpointConfig,
    private val observabilityProvider: ObservabilityProvider,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ComplianceSyncTransport {

    private val scope = ObservabilityScope(
        feature = "sync",
        component = "HttpComplianceSyncTransport",
        operation = "fetch",
    )

    override suspend fun fetchStatus(
        conversationId: ConversationId,
    ): ComplianceSyncResult = withContext(ioDispatcher) {
        if (!endpointConfig.isConfigured) {
            return@withContext ComplianceSyncResult.PermanentFailure(code = ERROR_UNCONFIGURED)
        }

        val traceContext = observabilityProvider.newTraceContext(
            correlationId = "compliance:$conversationId",
            conversationId = conversationId,
        )
        val endpoint = runCatching { URL(endpointConfig.complianceStatusUrl(conversationId)) }.getOrElse {
            observabilityProvider.logger(scope).log(
                level = LogLevel.ERROR,
                event = ObservedEvent(EventName("sync.compliance_invalid_endpoint")),
                traceContext = traceContext,
                attributes = endpointAttributes(endpointConfig.complianceStatusUrl(conversationId)),
            )
            return@withContext ComplianceSyncResult.PermanentFailure(code = ERROR_INVALID_ENDPOINT)
        }

        val connection = (endpoint.openConnection() as? HttpURLConnection)
            ?: return@withContext ComplianceSyncResult.PermanentFailure(code = ERROR_INVALID_ENDPOINT)

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = endpointConfig.connectTimeoutMillis
            connection.readTimeout = endpointConfig.readTimeoutMillis
            connection.doInput = true
            connection.setRequestProperty("Accept", JSON_CONTENT_TYPE)
            endpointConfig.apiKey?.takeIf { it.isNotBlank() }?.let { apiKey ->
                connection.setRequestProperty("Authorization", "Bearer $apiKey")
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val payload = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                return@withContext runCatching {
                    ComplianceSyncResult.Success(ComplianceStatusJsonDecoder.decode(payload))
                }.getOrElse {
                    observabilityProvider.logger(scope).log(
                        level = LogLevel.ERROR,
                        event = ObservedEvent(EventName("sync.compliance_payload_invalid")),
                        traceContext = traceContext,
                        attributes = endpointAttributes(endpoint.toExternalForm()),
                    )
                    ComplianceSyncResult.PermanentFailure(code = ERROR_INVALID_PAYLOAD)
                }
            }

            val result = responseCode.toComplianceSyncResult()
            observabilityProvider.logger(scope).log(
                level = result.toLogLevel(),
                event = ObservedEvent(EventName("sync.compliance_response")),
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
                event = ObservedEvent(EventName("sync.compliance_network_failure")),
                traceContext = traceContext,
                attributes = endpointAttributes(endpoint.toExternalForm()),
            )
            ComplianceSyncResult.RetryableFailure(code = ERROR_NETWORK_IO)
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

    private fun ComplianceSyncResult.toLogLevel(): LogLevel = when (this) {
        is ComplianceSyncResult.Success -> LogLevel.INFO
        is ComplianceSyncResult.RetryableFailure -> LogLevel.WARN
        is ComplianceSyncResult.PermanentFailure -> LogLevel.ERROR
    }

    private companion object {
        const val JSON_CONTENT_TYPE = "application/json; charset=utf-8"
        const val ERROR_INVALID_ENDPOINT = "invalid_endpoint"
        const val ERROR_INVALID_PAYLOAD = "compliance_payload_invalid"
        const val ERROR_NETWORK_IO = "network_io"
        const val ERROR_UNCONFIGURED = "transport_unconfigured"
    }
}

internal fun Int.toComplianceSyncResult(): ComplianceSyncResult = when {
    this == 408 || this == 425 || this == 429 -> ComplianceSyncResult.RetryableFailure("http_$this")
    this in 500..599 -> ComplianceSyncResult.RetryableFailure("http_$this")
    else -> ComplianceSyncResult.PermanentFailure("http_$this")
}
