package com.skeler.pulse.security.model

import java.time.Instant

data class EncryptedPayload(
    val keyAlias: String,
    val ciphertext: ByteArray,
    val initializationVector: ByteArray,
    val encryptedAt: Instant,
)
