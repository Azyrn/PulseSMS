package com.skeler.pulse.sync.api

import com.skeler.pulse.contracts.ConversationId
import com.skeler.pulse.security.api.BusinessComplianceStatus

interface ComplianceSyncTransport {
    suspend fun fetchStatus(
        conversationId: ConversationId,
    ): ComplianceSyncResult
}

sealed interface ComplianceSyncResult {
    data class Success(
        val status: BusinessComplianceStatus,
    ) : ComplianceSyncResult

    data class RetryableFailure(
        val code: String,
    ) : ComplianceSyncResult

    data class PermanentFailure(
        val code: String,
    ) : ComplianceSyncResult
}
