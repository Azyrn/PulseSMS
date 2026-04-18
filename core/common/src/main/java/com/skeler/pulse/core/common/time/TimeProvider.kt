package com.skeler.pulse.core.common.time

import java.time.Instant

fun interface TimeProvider {
    fun now(): Instant
}

class SystemTimeProvider : TimeProvider {
    override fun now(): Instant = Instant.now()
}
