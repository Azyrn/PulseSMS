package com.skeler.pulse.database.data

import java.util.Base64

interface CiphertextCodec {
    fun encode(bytes: ByteArray): String

    fun decode(value: String): ByteArray
}

class Base64CiphertextCodec : CiphertextCodec {
    override fun encode(bytes: ByteArray): String = Base64.getEncoder().withoutPadding().encodeToString(bytes)

    override fun decode(value: String): ByteArray = Base64.getDecoder().decode(value)
}
