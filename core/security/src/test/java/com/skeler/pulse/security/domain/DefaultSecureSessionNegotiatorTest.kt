package com.skeler.pulse.security.domain

import com.skeler.pulse.contracts.protocol.ProtocolFallbackReason
import com.skeler.pulse.contracts.protocol.ProtocolMode
import com.skeler.pulse.contracts.protocol.PublicSecurityState
import com.skeler.pulse.contracts.protocol.SessionNegotiationRequest
import com.skeler.pulse.contracts.security.KeyManagementState
import com.skeler.pulse.contracts.security.KeyStoreCapability
import com.skeler.pulse.observability.PulseObservabilityProvider
import com.skeler.pulse.security.api.KeyMaterialStore
import com.skeler.pulse.security.data.PqxdhKeyExchangeProtocol
import com.skeler.pulse.security.data.X3dhKeyExchangeProtocol
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class DefaultSecureSessionNegotiatorTest {

    @Test
    fun `uses pqxdh when hardware backed capability is available`() = runBlocking {
        val negotiator = DefaultSecureSessionNegotiator(
            protocols = listOf(PqxdhKeyExchangeProtocol(), X3dhKeyExchangeProtocol()),
            keyMaterialStore = FakeKeyMaterialStore(KeyStoreCapability.Available(hardwareBacked = true)),
            observabilityProvider = PulseObservabilityProvider("test.security"),
        )

        val result = negotiator.negotiate(
            SessionNegotiationRequest(
                conversationId = "conv-1",
                correlationId = "corr-1",
                requiresPostQuantum = true,
            )
        )

        assertEquals(PublicSecurityState.Protected, result.publicState)
        assertEquals(ProtocolMode.PQXDH, result.selectedMode)
        assertEquals(null, result.fallbackReason)
    }

    @Test
    fun `falls back to x3dh when pqxdh is unavailable`() = runBlocking {
        val negotiator = DefaultSecureSessionNegotiator(
            protocols = listOf(PqxdhKeyExchangeProtocol(), X3dhKeyExchangeProtocol()),
            keyMaterialStore = FakeKeyMaterialStore(KeyStoreCapability.SoftwareOnly),
            observabilityProvider = PulseObservabilityProvider("test.security"),
        )

        val result = negotiator.negotiate(
            SessionNegotiationRequest(
                conversationId = "conv-1",
                correlationId = "corr-1",
                requiresPostQuantum = true,
            )
        )

        assertEquals(PublicSecurityState.Protected, result.publicState)
        assertEquals(ProtocolMode.X3DH, result.selectedMode)
        assertEquals(ProtocolFallbackReason.DeviceCapabilityUnavailable, result.fallbackReason)
    }

    private class FakeKeyMaterialStore(
        private val capability: KeyStoreCapability,
    ) : KeyMaterialStore {
        override fun getCapability(): KeyStoreCapability = capability

        override fun getKeyManagementState(alias: String): KeyManagementState = KeyManagementState.Ready

        override fun getOrCreateKey(alias: String): SecretKey {
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(256)
            return keyGenerator.generateKey()
        }
    }
}
