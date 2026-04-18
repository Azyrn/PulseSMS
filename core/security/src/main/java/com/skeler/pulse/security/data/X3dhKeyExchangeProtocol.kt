package com.skeler.pulse.security.data

import com.skeler.pulse.contracts.observability.AttributeClassification
import com.skeler.pulse.contracts.observability.LogAttribute
import com.skeler.pulse.contracts.protocol.KeyProvenance
import com.skeler.pulse.contracts.protocol.ProtocolMode
import com.skeler.pulse.contracts.protocol.PublicSecurityState
import com.skeler.pulse.contracts.protocol.SessionNegotiationRequest
import com.skeler.pulse.contracts.security.KeyStoreCapability
import com.skeler.pulse.security.api.KeyExchangeProtocol
import com.skeler.pulse.security.api.NegotiatedSession

class X3dhKeyExchangeProtocol : KeyExchangeProtocol {
    override val mode: ProtocolMode = ProtocolMode.X3DH

    override suspend fun canHandle(
        request: SessionNegotiationRequest,
        capability: KeyStoreCapability,
    ): Boolean = capability != KeyStoreCapability.Unavailable

    override suspend fun negotiate(
        request: SessionNegotiationRequest,
        capability: KeyStoreCapability,
    ): NegotiatedSession = NegotiatedSession(
        publicState = PublicSecurityState.Protected,
        keyProvenance = when (capability) {
            is KeyStoreCapability.Available ->
                if (capability.hardwareBacked) {
                    KeyProvenance.HardwareBackedKeystore
                } else {
                    KeyProvenance.SoftwareBackedKeystore
                }
            KeyStoreCapability.SoftwareOnly -> KeyProvenance.SoftwareBackedKeystore
            KeyStoreCapability.Unavailable -> KeyProvenance.Unavailable
        },
        diagnostics = listOf(
            LogAttribute("protocol_family", "x3dh", AttributeClassification.InternalOnly),
        ),
    )
}
