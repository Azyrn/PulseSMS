package com.skeler.pulse.security.api

import com.skeler.pulse.contracts.ConversationId
import com.skeler.pulse.contracts.CorrelationId
import com.skeler.pulse.contracts.protocol.PublicSecurityState
import com.skeler.pulse.contracts.errors.ProtocolError
import com.skeler.pulse.security.model.EncryptedPayload

data class ProtectMessageRequest(
    val conversationId: ConversationId,
    val correlationId: CorrelationId,
    val plaintext: ByteArray,
    val requiresPostQuantum: Boolean = true,
)

sealed interface ProtectionResult {
    data class Success(
        val payload: EncryptedPayload,
        val securityState: PublicSecurityState,
    ) : ProtectionResult

    data class Failure(
        val error: ProtocolError,
    ) : ProtectionResult
}

interface MessageProtector {
    suspend fun protect(request: ProtectMessageRequest): ProtectionResult
}
