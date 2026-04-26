package com.skeler.pulse.ui

import com.skeler.pulse.InboxAccessState
import org.junit.Assert.assertEquals
import org.junit.Test

class RealSmsViewModelTest {

    @Test
    fun `inbox access state reflects denied permissions`() {
        val state = RealInboxState().copy(
            permissionDenied = InboxAccessState(permissionDenied = true, isDefaultSmsApp = true).permissionDenied,
            isDefaultSmsApp = InboxAccessState(permissionDenied = true, isDefaultSmsApp = true).isDefaultSmsApp,
            loading = false,
        )

        assertEquals(true, state.permissionDenied)
        assertEquals(true, state.isDefaultSmsApp)
        assertEquals(false, state.loading)
    }
}
