package com.skeler.pulse.database.api

import com.skeler.pulse.contracts.ConversationId
import com.skeler.pulse.security.api.BusinessComplianceStatus
import java.time.Instant

data class UpsertBusinessComplianceStatusRequest(
    val conversationId: ConversationId,
    val schemaVersion: Int,
    val status: BusinessComplianceStatus,
    val updatedAt: Instant,
)

interface BusinessComplianceStatusStore {
    suspend fun upsertStatus(request: UpsertBusinessComplianceStatusRequest)
}
