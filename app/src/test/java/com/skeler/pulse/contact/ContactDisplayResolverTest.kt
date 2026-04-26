package com.skeler.pulse.contact

import org.junit.Assert.assertEquals
import org.junit.Test

class ContactDisplayResolverTest {

    @Test
    fun `normalizes formatted phone numbers to the same key`() {
        assertEquals("+15551234567", "+1 (555) 123-4567".normalizeAddressForDisplay())
        assertEquals("5551234567", "555 123 4567".normalizeAddressForDisplay())
    }

    @Test
    fun `normalizes business senders case insensitively`() {
        assertEquals("amazon", "AMAZON".normalizeAddressForDisplay())
        assertEquals("uber", "Uber".normalizeAddressForDisplay())
    }

    @Test
    fun `returns blank for blank input`() {
        assertEquals("", "   ".normalizeAddressForDisplay())
    }
}
