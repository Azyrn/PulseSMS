package com.skeler.pulse.security.domain

import com.skeler.pulse.contracts.observability.AttributeClassification
import com.skeler.pulse.contracts.observability.EventName
import com.skeler.pulse.contracts.observability.LogAttribute
import com.skeler.pulse.contracts.observability.LogLevel
import com.skeler.pulse.contracts.observability.ObservabilityProvider
import com.skeler.pulse.contracts.observability.ObservabilityScope
import com.skeler.pulse.contracts.observability.ObservedEvent
import com.skeler.pulse.contracts.protocol.KeyProvenance
import com.skeler.pulse.contracts.protocol.ProtocolFallbackReason
import com.skeler.pulse.contracts.protocol.ProtocolMode
import com.skeler.pulse.contracts.protocol.PublicSecurityState
import com.skeler.pulse.contracts.protocol.SecureSessionNegotiator
import com.skeler.pulse.contracts.protocol.SessionNegotiationRequest
import com.skeler.pulse.contracts.protocol.SessionNegotiationResult
import com.skeler.pulse.contracts.security.KeyStoreCapability
import com.skeler.pulse.security.api.KeyExchangeProtocol
import com.skeler.pulse.security.api.KeyMaterialStore

class DefaultSecureSessionNegotiator(
    private val protocols: List<KeyExchangeProtocol>,
    private val keyMaterialStore: KeyMaterialStore,
    private val observabilityProvider: ObservabilityProvider,
) : SecureSessionNegotiator {

    private val scope = ObservabilityScope(
        feature = "security",
        component = "DefaultSecureSessionNegotiator",
        operation = "negotiate",
    )

    override suspend fun negotiate(request: SessionNegotiationRequest): SessionNegotiationResult {
        val capability = keyMaterialStore.getCapability()
        val traceContext = observabilityProvider.newTraceContext(
            correlationId = request.correlationId,
            conversationId = request.conversationId,
        )

        for ((index, protocol) in protocols.withIndex()) {
            if (!protocol.canHandle(request, capability)) {
                continue
            }
            val negotiated = protocol.negotiate(request, capability)
            val fallbackReason = if (index == 0) {
                null
            } else {
                resolveFallbackReason(capability)
            }

            observabilityProvider.logger(scope).log(
                level = LogLevel.INFO,
                event = ObservedEvent(EventName("security.session_negotiated")),
                traceContext = traceContext,
                attributes = negotiated.diagnostics + listOf(
                    LogAttribute("protocol_mode", protocol.mode.name, AttributeClassification.InternalOnly),
                ),
            )

            return SessionNegotiationResult(
                publicState = negotiated.publicState,
                selectedMode = protocol.mode,
                fallbackReason = fallbackReason,
                keyProvenance = negotiated.keyProvenance,
                diagnostics = negotiated.diagnostics,
            )
        }

        observabilityProvider.logger(scope).log(
            level = LogLevel.WARN,
            event = ObservedEvent(EventName("security.session_unavailable")),
            traceContext = traceContext,
            attributes = listOf(
                LogAttribute("capability", capability::class.simpleName.orEmpty(), AttributeClassification.InternalOnly),
            ),
        )

        return SessionNegotiationResult(
            publicState = PublicSecurityState.Unavailable,
            selectedMode = protocols.firstOrNull()?.mode ?: ProtocolMode.X3DH,
            fallbackReason = resolveFallbackReason(capability),
            keyProvenance = capability.toKeyProvenance(),
            diagnostics = listOf(
                LogAttribute("capability", capability::class.simpleName.orEmpty(), AttributeClassification.InternalOnly),
            ),
        )
    }

    private fun resolveFallbackReason(capability: KeyStoreCapability): ProtocolFallbackReason =
        when (capability) {
            is KeyStoreCapability.Available -> ProtocolFallbackReason.PolicyDowngradeApproved
            KeyStoreCapability.SoftwareOnly -> ProtocolFallbackReason.DeviceCapabilityUnavailable
            KeyStoreCapability.Unavailable -> ProtocolFallbackReason.KeyMaterialUnavailable
        }

    private fun KeyStoreCapability.toKeyProvenance(): KeyProvenance =
        when (this) {
            is KeyStoreCapability.Available ->
                if (hardwareBacked) {
                    KeyProvenance.HardwareBackedKeystore
                } else {
                    KeyProvenance.SoftwareBackedKeystore
                }
            KeyStoreCapability.SoftwareOnly -> KeyProvenance.SoftwareBackedKeystore
            KeyStoreCapability.Unavailable -> KeyProvenance.Unavailable
        }
}
