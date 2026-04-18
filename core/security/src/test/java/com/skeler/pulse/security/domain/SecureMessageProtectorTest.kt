package com.skeler.pulse.security.domain

import com.skeler.pulse.contracts.errors.ProtocolError
import com.skeler.pulse.contracts.protocol.ProtocolMode
import com.skeler.pulse.contracts.protocol.PublicSecurityState
import com.skeler.pulse.contracts.protocol.SessionNegotiationRequest
import com.skeler.pulse.contracts.protocol.SessionNegotiationResult
import com.skeler.pulse.contracts.security.KeyManagementState
import com.skeler.pulse.contracts.security.KeyStoreCapability
import com.skeler.pulse.observability.PulseObservabilityProvider
import com.skeler.pulse.security.api.KeyMaterialStore
import com.skeler.pulse.security.api.MessageProtector
import com.skeler.pulse.security.api.ProtectMessageRequest
import com.skeler.pulse.security.api.ProtectionResult
import com.skeler.pulse.security.data.AesGcmMessageCipher
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class SecureMessageProtectorTest {

    @Test
    fun `encrypts payload after successful negotiation`() = runBlocking {
        val keyMaterialStore = FakeKeyMaterialStore(
            capability = KeyStoreCapability.Available(hardwareBacked = true),
            keyManagementState = KeyManagementState.Ready,
        )
        val protector: MessageProtector = SecureMessageProtector(
            sessionNegotiator = FakeSecureSessionNegotiator(
                SessionNegotiationResult(
                    publicState = PublicSecurityState.Protected,
                    selectedMode = ProtocolMode.PQXDH,
                    fallbackReason = null,
                    keyProvenance = com.skeler.pulse.contracts.protocol.KeyProvenance.HardwareBackedKeystore,
                )
            ),
            keyMaterialStore = keyMaterialStore,
            messageCipher = AesGcmMessageCipher(keyMaterialStore),
            observabilityProvider = PulseObservabilityProvider("test.security"),
        )
        val plaintext = "hello".toByteArray()

        val result = protector.protect(
            ProtectMessageRequest(
                conversationId = "conv-1",
                correlationId = "corr-1",
                plaintext = plaintext,
            )
        )

        assertTrue(result is ProtectionResult.Success)
        assertArrayEquals(ByteArray(5), plaintext)
    }

    @Test
    fun `fails when keystore is unavailable`() = runBlocking {
        val keyMaterialStore = FakeKeyMaterialStore(
            capability = KeyStoreCapability.Unavailable,
            keyManagementState = KeyManagementState.Unrecoverable,
        )
        val protector: MessageProtector = SecureMessageProtector(
            sessionNegotiator = FakeSecureSessionNegotiator(
                SessionNegotiationResult(
                    publicState = PublicSecurityState.Unavailable,
                    selectedMode = ProtocolMode.X3DH,
                    fallbackReason = null,
                    keyProvenance = com.skeler.pulse.contracts.protocol.KeyProvenance.Unavailable,
                )
            ),
            keyMaterialStore = keyMaterialStore,
            messageCipher = AesGcmMessageCipher(keyMaterialStore),
            observabilityProvider = PulseObservabilityProvider("test.security"),
        )
        val plaintext = "hello".toByteArray()

        val result = protector.protect(
            ProtectMessageRequest(
                conversationId = "conv-1",
                correlationId = "corr-1",
                plaintext = plaintext,
            )
        )

        assertTrue(result is ProtectionResult.Failure)
        assertTrue((result as ProtectionResult.Failure).error is ProtocolError.KeyStoreUnavailable)
        assertArrayEquals(ByteArray(5), plaintext)
    }

    private class FakeSecureSessionNegotiator(
        private val result: SessionNegotiationResult,
    ) : com.skeler.pulse.contracts.protocol.SecureSessionNegotiator {
        override suspend fun negotiate(request: SessionNegotiationRequest): SessionNegotiationResult = result
    }

    private class FakeKeyMaterialStore(
        private val capability: KeyStoreCapability,
        private val keyManagementState: KeyManagementState,
    ) : KeyMaterialStore {
        private val secretKey: SecretKey by lazy {
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(256)
            keyGenerator.generateKey()
        }

        override fun getCapability(): KeyStoreCapability = capability

        override fun getKeyManagementState(alias: String): KeyManagementState = keyManagementState

        override fun getOrCreateKey(alias: String): SecretKey = secretKey
    }
}
