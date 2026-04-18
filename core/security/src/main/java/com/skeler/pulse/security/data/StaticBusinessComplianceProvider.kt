package com.skeler.pulse.security.data

import com.skeler.pulse.contracts.ConversationId
import com.skeler.pulse.security.api.BusinessComplianceProvider
import com.skeler.pulse.security.api.BusinessComplianceStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class StaticBusinessComplianceProvider(
    private val statuses: Map<ConversationId, BusinessComplianceStatus>,
    private val defaultStatus: BusinessComplianceStatus = BusinessComplianceStatus(),
) : BusinessComplianceProvider {

    override fun observeStatus(conversationId: ConversationId): Flow<BusinessComplianceStatus> =
        flowOf(statuses[conversationId] ?: defaultStatus)

    override suspend fun currentStatus(conversationId: ConversationId): BusinessComplianceStatus =
        statuses[conversationId] ?: defaultStatus
}
