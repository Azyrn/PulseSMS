package com.skeler.pulse.security.data

import com.skeler.pulse.security.api.KeyMaterialStore
import com.skeler.pulse.security.api.MessageCipher
import com.skeler.pulse.security.model.EncryptedPayload
import java.time.Instant
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

class AesGcmMessageCipher(
    private val keyMaterialStore: KeyMaterialStore,
) : MessageCipher {

    override fun encrypt(alias: String, plaintext: ByteArray): EncryptedPayload {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keyMaterialStore.getOrCreateKey(alias))
        val ciphertext = cipher.doFinal(plaintext)
        return EncryptedPayload(
            keyAlias = alias,
            ciphertext = ciphertext,
            initializationVector = cipher.iv,
            encryptedAt = Instant.now(),
        )
    }

    override fun decrypt(payload: EncryptedPayload): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            keyMaterialStore.getOrCreateKey(payload.keyAlias),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, payload.initializationVector),
        )
        return cipher.doFinal(payload.ciphertext)
    }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH_BITS = 128
    }
}
