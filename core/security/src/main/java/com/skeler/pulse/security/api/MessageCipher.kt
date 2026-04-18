package com.skeler.pulse.security.api

import com.skeler.pulse.security.model.EncryptedPayload

interface MessageCipher {
    fun encrypt(alias: String, plaintext: ByteArray): EncryptedPayload

    fun decrypt(payload: EncryptedPayload): ByteArray
}
