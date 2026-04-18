package com.skeler.pulse.security.domain

import com.skeler.pulse.contracts.errors.ProtocolError
import com.skeler.pulse.contracts.observability.EventName
import com.skeler.pulse.contracts.observability.LogLevel
import com.skeler.pulse.contracts.observability.ObservabilityProvider
import com.skeler.pulse.contracts.observability.ObservabilityScope
import com.skeler.pulse.contracts.observability.ObservedEvent
import com.skeler.pulse.contracts.protocol.PublicSecurityState
import com.skeler.pulse.contracts.protocol.SecureSessionNegotiator
import com.skeler.pulse.contracts.protocol.SessionNegotiationRequest
import com.skeler.pulse.contracts.security.KeyManagementState
import com.skeler.pulse.security.api.KeyMaterialStore
import com.skeler.pulse.security.api.MessageCipher
import com.skeler.pulse.security.api.MessageProtector
import com.skeler.pulse.security.api.ProtectMessageRequest
import com.skeler.pulse.security.api.ProtectionResult
class SecureMessageProtector(
    private val sessionNegotiator: SecureSessionNegotiator,
    private val keyMaterialStore: KeyMaterialStore,
    private val messageCipher: MessageCipher,
    private val observabilityProvider: ObservabilityProvider,
) : MessageProtector {

    private val scope = ObservabilityScope(
        feature = "security",
        component = "SecureMessageProtector",
        operation = "protect",
    )

    override suspend fun protect(request: ProtectMessageRequest): ProtectionResult {
        val traceContext = observabilityProvider.newTraceContext(
            correlationId = request.correlationId,
            conversationId = request.conversationId,
        )
        val session = sessionNegotiator.negotiate(
            SessionNegotiationRequest(
                conversationId = request.conversationId,
                correlationId = request.correlationId,
                requiresPostQuantum = request.requiresPostQuantum,
            )
        )

        if (session.publicState != PublicSecurityState.Protected) {
            val error = when (keyMaterialStore.getCapability()) {
                com.skeler.pulse.contracts.security.KeyStoreCapability.Unavailable -> ProtocolError.KeyStoreUnavailable()
                else -> ProtocolError.SecureChannelUnavailable()
            }
            observabilityProvider.logger(scope).log(
                level = LogLevel.WARN,
                event = ObservedEvent(EventName("security.protection_failed")),
                traceContext = traceContext,
            )
            request.plaintext.fill(0)
            return ProtectionResult.Failure(error)
        }

        val keyAlias = buildKeyAlias(request.conversationId)
        val keyState = keyMaterialStore.getKeyManagementState(keyAlias)
        if (keyState == KeyManagementState.RotationRequired) {
            request.plaintext.fill(0)
            return ProtectionResult.Failure(ProtocolError.KeyRotationRequired())
        }
        if (keyState == KeyManagementState.Corrupted || keyState == KeyManagementState.Unrecoverable) {
            request.plaintext.fill(0)
            return ProtectionResult.Failure(ProtocolError.KeyStoreUnavailable())
        }

        val encryptedPayload = try {
            messageCipher.encrypt(keyAlias, request.plaintext)
        } finally {
            request.plaintext.fill(0)
        }

        observabilityProvider.logger(scope).log(
            level = LogLevel.INFO,
            event = ObservedEvent(EventName("security.payload_encrypted")),
            traceContext = traceContext,
        )

        return ProtectionResult.Success(
            payload = encryptedPayload,
            securityState = session.publicState,
        )
    }

    private fun buildKeyAlias(conversationId: String): String =
        "pulse_message_${conversationId.lowercase()}"
}
