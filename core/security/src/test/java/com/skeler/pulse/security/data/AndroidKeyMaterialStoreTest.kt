package com.skeler.pulse.security.data

import com.skeler.pulse.contracts.security.KeyManagementState
import javax.crypto.SecretKey
import org.junit.Assert.assertEquals
import org.junit.Test

class AndroidKeyMaterialStoreTest {

    @Test
    fun `missing alias is ready for lazy key creation`() {
        assertEquals(
            KeyManagementState.Ready,
            resolveKeyManagementState(aliasExists = false, key = null),
        )
    }

    @Test
    fun `existing secret key is ready`() {
        val key = object : SecretKey {
            override fun getAlgorithm(): String = "AES"
            override fun getFormat(): String = "RAW"
            override fun getEncoded(): ByteArray = byteArrayOf(1)
        }

        assertEquals(
            KeyManagementState.Ready,
            resolveKeyManagementState(aliasExists = true, key = key),
        )
    }

    @Test
    fun `existing alias without a secret key is corrupted`() {
        assertEquals(
            KeyManagementState.Corrupted,
            resolveKeyManagementState(aliasExists = true, key = null),
        )
    }
}
