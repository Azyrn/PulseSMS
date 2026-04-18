package com.skeler.pulse.security.api

import com.skeler.pulse.contracts.ConversationId
import kotlinx.coroutines.flow.Flow
import java.time.Instant

data class BusinessComplianceStatus(
    val senderVerified: Boolean = true,
    val recipientVerified: Boolean = true,
    val identityVerified: Boolean = true,
    val tenDlcRegistered: Boolean = true,
    val updatedAt: Instant? = null,
)

interface BusinessComplianceProvider {
    fun observeStatus(conversationId: ConversationId): Flow<BusinessComplianceStatus>

    suspend fun currentStatus(conversationId: ConversationId): BusinessComplianceStatus
}
