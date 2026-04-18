package com.skeler.pulse.sync.domain

import java.time.Instant
import kotlin.math.min
import kotlin.random.Random

class SyncRetryPolicy(
    private val baseDelayMillis: Long = 1_000L,
    private val maxDelayMillis: Long = 5 * 60_000L,
    private val jitterRatio: Double = 0.2,
    private val random: Random = Random.Default,
) {
    fun nextRetryAt(
        attempt: Int,
        from: Instant,
    ): Instant {
        val exponent = (attempt - 1).coerceAtLeast(0)
        val exponentialDelay = min(maxDelayMillis, baseDelayMillis * (1L shl exponent.coerceAtMost(20)))
        val jitterWindow = (exponentialDelay * jitterRatio).toLong().coerceAtLeast(0L)
        val jitter = if (jitterWindow == 0L) 0L else random.nextLong(until = jitterWindow + 1L)
        return from.plusMillis(exponentialDelay + jitter)
    }

    fun jitterWindowMillis(
        attempt: Int,
    ): Long {
        val exponent = (attempt - 1).coerceAtLeast(0)
        val exponentialDelay = min(maxDelayMillis, baseDelayMillis * (1L shl exponent.coerceAtMost(20)))
        return (exponentialDelay * jitterRatio).toLong().coerceAtLeast(0L)
    }
}
