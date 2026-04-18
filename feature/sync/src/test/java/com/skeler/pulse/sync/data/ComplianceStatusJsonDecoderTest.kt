package com.skeler.pulse.sync.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ComplianceStatusJsonDecoderTest {
    @Test
    fun `decodes business compliance booleans`() {
        val decoded = ComplianceStatusJsonDecoder.decode(
            """
            {
              "senderVerified": true,
              "recipientVerified": false,
              "identityVerified": true,
              "tenDlcRegistered": false,
              "updatedAt": "2026-04-17T10:15:00Z"
            }
            """.trimIndent()
        )

        assertTrue(decoded.senderVerified)
        assertFalse(decoded.recipientVerified)
        assertTrue(decoded.identityVerified)
        assertFalse(decoded.tenDlcRegistered)
        assertEquals(Instant.parse("2026-04-17T10:15:00Z"), decoded.updatedAt)
    }
}
