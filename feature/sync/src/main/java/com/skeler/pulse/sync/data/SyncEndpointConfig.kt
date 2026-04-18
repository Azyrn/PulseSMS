package com.skeler.pulse.sync.data

import com.skeler.pulse.contracts.ConversationId
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class SyncEndpointConfig(
    val baseUrl: String,
    val apiKey: String? = null,
    val connectTimeoutMillis: Int = DEFAULT_CONNECT_TIMEOUT_MILLIS,
    val readTimeoutMillis: Int = DEFAULT_READ_TIMEOUT_MILLIS,
) {
    val isConfigured: Boolean
        get() = baseUrl.isNotBlank()

    fun messagesSyncUrl(): String = "${baseUrl.trimEnd('/')}/messages/sync"

    fun complianceStatusUrl(conversationId: ConversationId): String {
        val encodedConversationId = URLEncoder.encode(conversationId, StandardCharsets.UTF_8.toString())
        return "${baseUrl.trimEnd('/')}/conversations/$encodedConversationId/compliance"
    }

    private companion object {
        const val DEFAULT_CONNECT_TIMEOUT_MILLIS = 5_000
        const val DEFAULT_READ_TIMEOUT_MILLIS = 5_000
    }
}
