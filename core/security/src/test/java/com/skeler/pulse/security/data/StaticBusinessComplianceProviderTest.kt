package com.skeler.pulse.security.data

import com.skeler.pulse.security.api.BusinessComplianceStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StaticBusinessComplianceProviderTest {
    @Test
    fun `returns configured profile for conversation`() = runBlocking {
        val provider = StaticBusinessComplianceProvider(
            statuses = mapOf(
                "business-10dlc-pending" to BusinessComplianceStatus(
                    tenDlcRegistered = false,
                )
            )
        )

        val status = provider.observeStatus("business-10dlc-pending").first()

        assertFalse(status.tenDlcRegistered)
        assertTrue(status.senderVerified)
    }

    @Test
    fun `falls back to default profile`() = runBlocking {
        val provider = StaticBusinessComplianceProvider(
            statuses = emptyMap(),
            defaultStatus = BusinessComplianceStatus(
                senderVerified = false,
                identityVerified = false,
            ),
        )

        val status = provider.currentStatus("unknown-conversation")

        assertFalse(status.senderVerified)
        assertFalse(status.identityVerified)
        assertEquals(true, status.recipientVerified)
    }
}
