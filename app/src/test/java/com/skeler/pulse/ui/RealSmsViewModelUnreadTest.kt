package com.skeler.pulse.ui

import android.provider.Telephony
import com.skeler.pulse.sms.SystemSms
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RealSmsViewModelUnreadTest {

    @Test
    fun `detects unread inbound messages`() {
        val messages = listOf(
            SystemSms(
                id = 1L,
                address = "665",
                body = "hello",
                date = 1L,
                type = Telephony.Sms.MESSAGE_TYPE_INBOX,
                read = false,
                threadId = 10L,
            ),
        )

        assertTrue(messages.hasUnreadInboundMessages())
    }

    @Test
    fun `ignores read and outbound messages when checking unread inbound state`() {
        val messages = listOf(
            SystemSms(
                id = 1L,
                address = "665",
                body = "read inbound",
                date = 1L,
                type = Telephony.Sms.MESSAGE_TYPE_INBOX,
                read = true,
                threadId = 10L,
            ),
            SystemSms(
                id = 2L,
                address = "665",
                body = "sent",
                date = 2L,
                type = Telephony.Sms.MESSAGE_TYPE_SENT,
                read = false,
                threadId = 10L,
            ),
        )

        assertFalse(messages.hasUnreadInboundMessages())
    }
}
