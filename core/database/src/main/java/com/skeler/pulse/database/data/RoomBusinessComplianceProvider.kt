package com.skeler.pulse.database.data

import com.skeler.pulse.contracts.ConversationId
import com.skeler.pulse.database.api.BusinessComplianceStatusStore
import com.skeler.pulse.database.api.UpsertBusinessComplianceStatusRequest
import com.skeler.pulse.security.api.BusinessComplianceProvider
import com.skeler.pulse.security.api.BusinessComplianceStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class RoomBusinessComplianceProvider(
    private val businessComplianceDao: BusinessComplianceDao,
    private val defaultStatus: BusinessComplianceStatus = BusinessComplianceStatus(),
) : BusinessComplianceProvider, BusinessComplianceStatusStore {

    override fun observeStatus(conversationId: ConversationId): Flow<BusinessComplianceStatus> =
        businessComplianceDao.observeStatus(conversationId).map { entity ->
            entity?.toStatus() ?: defaultStatus
        }

    override suspend fun currentStatus(conversationId: ConversationId): BusinessComplianceStatus =
        businessComplianceDao.findByConversationId(conversationId)?.toStatus() ?: defaultStatus

    override suspend fun upsertStatus(request: UpsertBusinessComplianceStatusRequest) {
        businessComplianceDao.upsert(
            BusinessComplianceEntity(
                conversationId = request.conversationId,
                schemaVersion = request.schemaVersion,
                senderVerified = request.status.senderVerified,
                recipientVerified = request.status.recipientVerified,
                identityVerified = request.status.identityVerified,
                tenDlcRegistered = request.status.tenDlcRegistered,
                updatedAtEpochMillis = request.updatedAt.toEpochMilli(),
            )
        )
    }

    private fun BusinessComplianceEntity.toStatus(): BusinessComplianceStatus =
        BusinessComplianceStatus(
            senderVerified = senderVerified,
            recipientVerified = recipientVerified,
            identityVerified = identityVerified,
            tenDlcRegistered = tenDlcRegistered,
            updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
        )
}
