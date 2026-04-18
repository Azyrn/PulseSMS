package com.skeler.pulse.sync.data

import com.skeler.pulse.sync.api.SyncTransportResult
import org.junit.Assert.assertEquals
import org.junit.Test

class HttpSyncTransportTest {
    @Test
    fun `maps retryable status codes`() {
        assertEquals(SyncTransportResult.RetryableFailure("http_429"), 429.toTransportResult())
        assertEquals(SyncTransportResult.RetryableFailure("http_503"), 503.toTransportResult())
    }

    @Test
    fun `maps permanent status codes`() {
        assertEquals(SyncTransportResult.PermanentFailure("http_401"), 401.toTransportResult())
        assertEquals(SyncTransportResult.PermanentFailure("http_422"), 422.toTransportResult())
    }

    @Test
    fun `maps success status codes`() {
        assertEquals(SyncTransportResult.Success, 202.toTransportResult())
    }
}
