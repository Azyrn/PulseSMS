package com.skeler.pulse.sync.data

import com.skeler.pulse.contracts.persistence.PayloadStoragePolicy
import com.skeler.pulse.contracts.persistence.PersistedMessageEnvelope
import com.skeler.pulse.contracts.persistence.PersistedSyncEnvelope
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncPayloadJsonEncoderTest {
    @Test
    fun `encodes envelope as transport json`() {
        val json = SyncPayloadJsonEncoder.encode(
            PersistedMessageEnvelope(
                schemaVersion = 1,
                messageId = "message-1",
                conversationId = "conversation-1",
                bodyCiphertext = "cipher\"value",
                bodyKeyAlias = "alias",
                bodyInitializationVector = "iv",
                bodyPreview = "visible\npreview",
                payloadStoragePolicy = PayloadStoragePolicy.CiphertextOnly,
                sentAtEpochMillis = 100L,
                receivedAtEpochMillis = null,
                sync = PersistedSyncEnvelope(
                    schemaVersion = 1,
                    queueKey = "queue-1",
                    dedupeKey = "dedupe-1",
                    attempt = 2,
                    maxAttempts = 5,
                    nextRetryAtEpochMillis = null,
                    lastFailureCode = "timeout",
                ),
            )
        )

        assertTrue(json.contains("\"messageId\": \"message-1\""))
        assertTrue(json.contains("\"bodyCiphertext\": \"cipher\\\"value\""))
        assertTrue(json.contains("\"bodyKeyAlias\": \"alias\""))
        assertTrue(json.contains("\"bodyInitializationVector\": \"iv\""))
        assertTrue(json.contains("\"bodyPreview\": \"visible\\npreview\""))
        assertTrue(json.contains("\"lastFailureCode\": \"timeout\""))
    }
}
