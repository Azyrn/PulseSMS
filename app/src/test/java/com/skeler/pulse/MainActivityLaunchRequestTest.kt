package com.skeler.pulse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MainActivityLaunchRequestTest {

    @Test
    fun `returns null when no launch values are present`() {
        assertNull(buildPulseLaunchRequestOrNull(conversationAddress = null, draftBody = null))
    }

    @Test
    fun `maps conversation address and draft body from raw launch values`() {
        val request = buildPulseLaunchRequestOrNull(
            conversationAddress = "+15551234567",
            draftBody = "See you soon",
        )

        assertEquals(
            PulseLaunchRequest(
                conversationAddress = "+15551234567",
                conversationTitle = "",
                draftBody = "See you soon",
            ),
            request,
        )
    }

    @Test
    fun `trims blank values and returns null when inputs collapse to empty`() {
        val request = buildPulseLaunchRequestOrNull(
            conversationAddress = "   ",
            draftBody = "\n",
        )

        assertNull(request)
    }
}
