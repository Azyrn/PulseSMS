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

class PqxdhKeyExchangeProtocol : KeyExchangeProtocol {
    override val mode: ProtocolMode = ProtocolMode.PQXDH

    override suspend fun canHandle(
        request: SessionNegotiationRequest,
        capability: KeyStoreCapability,
    ): Boolean = capability is KeyStoreCapability.Available && capability.hardwareBacked

    override suspend fun negotiate(
        request: SessionNegotiationRequest,
        capability: KeyStoreCapability,
    ): NegotiatedSession = NegotiatedSession(
        publicState = PublicSecurityState.Protected,
        keyProvenance = KeyProvenance.HardwareBackedKeystore,
        diagnostics = listOf(
            LogAttribute("protocol_family", "pqxdh", AttributeClassification.InternalOnly),
        ),
    )
}
