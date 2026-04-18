package com.skeler.pulse.core.common.ids

import java.util.UUID

fun interface IdGenerator {
    fun newId(): String
}

class UuidIdGenerator : IdGenerator {
    override fun newId(): String = UUID.randomUUID().toString()
}
