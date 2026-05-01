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
            when (character.code) {
                0x5C -> append("\\\\")  // backslash
                0x22 -> append("\\\"")  // double quote
                0x08 -> append("\\b")   // backspace
                0x0C -> append("\\f")   // form feed
                0x0A -> append("\\n")   // newline
                0x0D -> append("\\r")   // carriage return
                0x09 -> append("\\t")   // tab
                in 0x00..0x1F -> append(String.format("\\u%04X", character.code))
                else -> append(character)
            }
        }
    }
}