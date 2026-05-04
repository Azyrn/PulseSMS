package com.skeler.pulse

import android.content.res.Configuration
import com.skeler.pulse.design.theme.SerafinaThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun `resolves persisted dark theme before first compose frame`() {
        assertTrue(resolveDarkTheme(SerafinaThemeMode.Dark, systemDarkTheme = false))
        assertFalse(resolveDarkTheme(SerafinaThemeMode.Light, systemDarkTheme = true))
        assertTrue(resolveDarkTheme(SerafinaThemeMode.System, systemDarkTheme = true))
        assertFalse(resolveDarkTheme(SerafinaThemeMode.System, systemDarkTheme = false))
    }

    @Test
    fun `detects system night mode from configuration flags`() {
        assertTrue(isSystemNightMode(Configuration.UI_MODE_NIGHT_YES))
        assertFalse(isSystemNightMode(Configuration.UI_MODE_NIGHT_NO))
    }
}
