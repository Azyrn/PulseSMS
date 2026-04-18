package com.skeler.pulse.security.api

import com.skeler.pulse.contracts.observability.LogAttribute
import com.skeler.pulse.contracts.protocol.KeyProvenance
import com.skeler.pulse.contracts.protocol.ProtocolMode
import com.skeler.pulse.contracts.protocol.PublicSecurityState
import com.skeler.pulse.contracts.protocol.SessionNegotiationRequest
import com.skeler.pulse.contracts.security.KeyStoreCapability

interface KeyExchangeProtocol {
    val mode: ProtocolMode

    suspend fun canHandle(
        request: SessionNegotiationRequest,
        capability: KeyStoreCapability,
    ): Boolean

    suspend fun negotiate(
        request: SessionNegotiationRequest,
        capability: KeyStoreCapability,
    ): NegotiatedSession
}

data class NegotiatedSession(
    val publicState: PublicSecurityState,
    val keyProvenance: KeyProvenance,
    val diagnostics: List<LogAttribute> = emptyList(),
)
