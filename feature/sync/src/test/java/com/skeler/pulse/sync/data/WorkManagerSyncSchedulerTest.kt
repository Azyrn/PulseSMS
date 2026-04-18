package com.skeler.pulse.sync.data

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkManagerSyncSchedulerTest {
    @Test
    fun `unique work name is stable per conversation`() {
        assertEquals("pulse_sync_conv-1", WorkManagerSyncScheduler.uniqueWorkName("conv-1"))
    }
}
