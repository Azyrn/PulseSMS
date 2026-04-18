package com.skeler.pulse.contracts.protocol

import com.skeler.pulse.contracts.ConversationId
import com.skeler.pulse.contracts.CorrelationId
import com.skeler.pulse.contracts.observability.LogAttribute

sealed interface PublicSecurityState {
    data object Protected : PublicSecurityState
    data object Recovering : PublicSecurityState
    data object Unavailable : PublicSecurityState
}

enum class ProtocolMode {
    PQXDH,
    X3DH,
}

enum class ProtocolFallbackReason {
    DeviceCapabilityUnavailable,
    KeyMaterialUnavailable,
    PolicyDowngradeApproved,
}

enum class KeyProvenance {
    HardwareBackedKeystore,
    SoftwareBackedKeystore,
    MemoryOnly,
    Unavailable,
}

data class SessionNegotiationRequest(
    val conversationId: ConversationId,
    val correlationId: CorrelationId,
    val requiresPostQuantum: Boolean,
)

data class SessionNegotiationResult(
    val publicState: PublicSecurityState,
    val selectedMode: ProtocolMode,
    val fallbackReason: ProtocolFallbackReason?,
    val keyProvenance: KeyProvenance,
    val diagnostics: List<LogAttribute> = emptyList(),
)

interface SecureSessionNegotiator {
    suspend fun negotiate(request: SessionNegotiationRequest): SessionNegotiationResult
}
