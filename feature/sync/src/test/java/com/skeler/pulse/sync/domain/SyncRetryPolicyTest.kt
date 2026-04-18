package com.skeler.pulse.sync.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.random.Random

class SyncRetryPolicyTest {
    @Test
    fun `next retry grows exponentially`() {
        val policy = SyncRetryPolicy(
            baseDelayMillis = 1_000L,
            maxDelayMillis = 60_000L,
            jitterRatio = 0.0,
            random = Random(0),
        )
        val start = Instant.parse("2026-04-17T00:00:00Z")

        val first = policy.nextRetryAt(1, start)
        val second = policy.nextRetryAt(2, start)

        assertEquals(start.plusMillis(1_000L), first)
        assertEquals(start.plusMillis(2_000L), second)
    }
}
