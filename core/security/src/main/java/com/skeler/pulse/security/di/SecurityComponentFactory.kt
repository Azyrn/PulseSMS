package com.skeler.pulse.security.di

import android.content.Context
import com.skeler.pulse.contracts.observability.ObservabilityProvider
import com.skeler.pulse.observability.PulseObservabilityProvider
import com.skeler.pulse.security.api.BusinessComplianceProvider
import com.skeler.pulse.security.api.KeyExchangeProtocol
import com.skeler.pulse.security.api.KeyMaterialStore
import com.skeler.pulse.security.api.MessageCipher
import com.skeler.pulse.security.api.MessageProtector
import com.skeler.pulse.security.data.AesGcmMessageCipher
import com.skeler.pulse.security.data.AndroidKeyMaterialStore
import com.skeler.pulse.security.data.PqxdhKeyExchangeProtocol
import com.skeler.pulse.security.data.StaticBusinessComplianceProvider
import com.skeler.pulse.security.data.X3dhKeyExchangeProtocol
import com.skeler.pulse.security.domain.DefaultSecureSessionNegotiator
import com.skeler.pulse.security.domain.SecureMessageProtector

data class SecurityComponent(
    val businessComplianceProvider: BusinessComplianceProvider,
    val keyMaterialStore: KeyMaterialStore,
    val protocols: List<KeyExchangeProtocol>,
    val messageCipher: MessageCipher,
    val messageProtector: MessageProtector,
)

object SecurityComponentFactory {
    fun create(
        context: Context,
        observabilityProvider: ObservabilityProvider = PulseObservabilityProvider("core.security"),
        businessComplianceProvider: BusinessComplianceProvider = defaultBusinessComplianceProvider(),
    ): SecurityComponent {
        val keyMaterialStore = AndroidKeyMaterialStore(context)
        val protocols = listOf(
            PqxdhKeyExchangeProtocol(),
            X3dhKeyExchangeProtocol(),
        )
        val messageCipher = AesGcmMessageCipher(keyMaterialStore)
        val sessionNegotiator = DefaultSecureSessionNegotiator(
            protocols = protocols,
            keyMaterialStore = keyMaterialStore,
            observabilityProvider = observabilityProvider,
        )
        return SecurityComponent(
            businessComplianceProvider = businessComplianceProvider,
            keyMaterialStore = keyMaterialStore,
            protocols = protocols,
            messageCipher = messageCipher,
            messageProtector = SecureMessageProtector(
                sessionNegotiator = sessionNegotiator,
                keyMaterialStore = keyMaterialStore,
                messageCipher = messageCipher,
                observabilityProvider = observabilityProvider,
            ),
        )
    }

    private fun defaultBusinessComplianceProvider(): BusinessComplianceProvider =
        StaticBusinessComplianceProvider(
            statuses = mapOf(
                "business-primary" to com.skeler.pulse.security.api.BusinessComplianceStatus(),
                "business-verification-pending" to com.skeler.pulse.security.api.BusinessComplianceStatus(
                    senderVerified = false,
                ),
                "business-recipient-pending" to com.skeler.pulse.security.api.BusinessComplianceStatus(
                    recipientVerified = false,
                ),
                "business-identity-missing" to com.skeler.pulse.security.api.BusinessComplianceStatus(
                    identityVerified = false,
                ),
                "business-10dlc-pending" to com.skeler.pulse.security.api.BusinessComplianceStatus(
                    tenDlcRegistered = false,
                ),
            ),
        )
}
