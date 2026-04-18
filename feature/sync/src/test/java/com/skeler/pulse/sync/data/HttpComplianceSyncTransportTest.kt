package com.skeler.pulse.sync.data

import com.skeler.pulse.sync.api.ComplianceSyncResult
import org.junit.Assert.assertEquals
import org.junit.Test

class HttpComplianceSyncTransportTest {
    @Test
    fun `maps retryable compliance status codes`() {
        assertEquals(ComplianceSyncResult.RetryableFailure("http_429"), 429.toComplianceSyncResult())
        assertEquals(ComplianceSyncResult.RetryableFailure("http_503"), 503.toComplianceSyncResult())
    }

    @Test
    fun `maps permanent compliance status codes`() {
        assertEquals(ComplianceSyncResult.PermanentFailure("http_401"), 401.toComplianceSyncResult())
        assertEquals(ComplianceSyncResult.PermanentFailure("http_422"), 422.toComplianceSyncResult())
    }
}
