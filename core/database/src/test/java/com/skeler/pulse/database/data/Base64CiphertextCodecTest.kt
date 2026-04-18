package com.skeler.pulse.database.data

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class Base64CiphertextCodecTest {
    @Test
    fun `encodes and decodes ciphertext bytes losslessly`() {
        val codec = Base64CiphertextCodec()
        val bytes = byteArrayOf(1, 2, 3, 4, 5)

        val encoded = codec.encode(bytes)
        val decoded = codec.decode(encoded)

        assertArrayEquals(bytes, decoded)
    }
}
