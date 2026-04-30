package com.skeler.pulse.sync.data

import com.skeler.pulse.contracts.persistence.PersistedMessageEnvelope

internal object SyncPayloadJsonEncoder {
    fun encode(
        message: PersistedMessageEnvelope,
    ): String = buildString {
        appendLine("{")
        appendIndented("\"schemaVersion\": ${message.schemaVersion},")
        appendIndented("\"messageId\": \"${message.messageId.escapeJson()}\",")
        appendIndented("\"conversationId\": \"${message.conversationId.escapeJson()}\",")
        appendIndented("\"bodyCiphertext\": ${message.bodyCiphertext.toJsonStringOrNull()},")
        appendIndented("\"bodyKeyAlias\": ${message.bodyKeyAlias.toJsonStringOrNull()},")
        appendIndented("\"bodyInitializationVector\": ${message.bodyInitializationVector.toJsonStringOrNull()},")
        appendIndented("\"bodyPreview\": \"${message.bodyPreview.escapeJson()}\",")
        appendIndented("\"payloadStoragePolicy\": \"${message.payloadStoragePolicy.name}\",")
        appendIndented("\"sentAtEpochMillis\": ${message.sentAtEpochMillis?.toString() ?: "null"},")
        appendIndented("\"receivedAtEpochMillis\": ${message.receivedAtEpochMillis?.toString() ?: "null"},")
        appendLine("  \"sync\": {")
        appendIndented("    \"schemaVersion\": ${message.sync.schemaVersion},")
        appendIndented("    \"queueKey\": \"${message.sync.queueKey.escapeJson()}\",")
        appendIndented("    \"dedupeKey\": \"${message.sync.dedupeKey.escapeJson()}\",")
        appendIndented("    \"attempt\": ${message.sync.attempt},")
        appendIndented("    \"maxAttempts\": ${message.sync.maxAttempts},")
        appendIndented("    \"nextRetryAtEpochMillis\": ${message.sync.nextRetryAtEpochMillis?.toString() ?: "null"},")
        appendIndented("    \"lastFailureCode\": ${message.sync.lastFailureCode.toJsonStringOrNull()}", indent = "")
        appendLine("  }")
        append('}')
    }

    private fun StringBuilder.appendIndented(
        value: String,
        indent: String = "  ",
    ) {
        append(indent)
        appendLine(value)
    }

    private fun String?.toJsonStringOrNull(): String = this?.let { "\"${it.escapeJson()}\"" } ?: "null"

    private fun String.escapeJson(): String = buildString(length + 8) {
        this@escapeJson.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\b' -> append("\\b")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(character)
            }
        }
    }
}
