package com.skeler.pulse.sms

import android.content.Context
import android.util.Base64
import com.skeler.pulse.security.data.AesGcmMessageCipher
import com.skeler.pulse.security.data.AndroidKeyMaterialStore
import javax.inject.Inject

class SmsEncryptionManager @Inject constructor(
    private val context: Context,
) {
    private val keyStore = AndroidKeyMaterialStore(context)
    private val cipher = AesGcmMessageCipher(keyStore)
    private val keyAlias = PULSE_SMS_KEY_ALIAS

    private val keyAliasCreated: Boolean by lazy {
        try {
            keyStore.getOrCreateKey(keyAlias)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun encrypt(plaintext: String): String? {
        if (!keyAliasCreated) return null
        return try {
            val encrypted = cipher.encrypt(keyAlias, plaintext.toByteArray(Charsets.UTF_8))
            val combined = encrypted.initializationVector + encrypted.ciphertext
            ENCRYPTED_PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }

    fun decrypt(ciphertext: String): String? {
        if (!ciphertext.startsWith(ENCRYPTED_PREFIX)) return null
        return try {
            val raw = Base64.decode(ciphertext.removePrefix(ENCRYPTED_PREFIX), Base64.NO_WRAP)
            val payload = com.skeler.pulse.security.model.EncryptedPayload(
                keyAlias = keyAlias,
                ciphertext = raw.copyOfRange(IV_LENGTH, raw.size),
                initializationVector = raw.copyOfRange(0, IV_LENGTH),
                encryptedAt = java.time.Instant.now(),
            )
            String(cipher.decrypt(payload), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    fun isEncrypted(body: String): Boolean = body.startsWith(ENCRYPTED_PREFIX)

    val isAvailable: Boolean get() = keyAliasCreated

    companion object {
        private const val ENCRYPTED_PREFIX = "~~pulse_e2e~~"
        private const val IV_LENGTH = 12
        private const val PULSE_SMS_KEY_ALIAS = "pulse_sms_e2e_key"
    }
}
