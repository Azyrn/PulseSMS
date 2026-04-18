package com.skeler.pulse.database.data

import com.skeler.pulse.database.api.UpsertBusinessComplianceStatusRequest
import com.skeler.pulse.security.api.BusinessComplianceStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class RoomBusinessComplianceProviderTest {

    @Test
    fun `returns default status when no persisted compliance exists`() = runBlocking {
        val provider = RoomBusinessComplianceProvider(
            businessComplianceDao = FakeBusinessComplianceDao(),
            defaultStatus = BusinessComplianceStatus(
                senderVerified = false,
                tenDlcRegistered = false,
            ),
        )

        val status = provider.currentStatus("conv-unknown")

        assertFalse(status.senderVerified)
        assertFalse(status.tenDlcRegistered)
    }

    @Test
    fun `observes persisted compliance updates`() = runBlocking {
        val dao = FakeBusinessComplianceDao()
        val provider = RoomBusinessComplianceProvider(
            businessComplianceDao = dao,
        )

        provider.upsertStatus(
            UpsertBusinessComplianceStatusRequest(
                conversationId = "business-10dlc-pending",
                schemaVersion = 1,
                status = BusinessComplianceStatus(
                    tenDlcRegistered = false,
                ),
                updatedAt = Instant.parse("2026-04-17T00:00:00Z"),
            )
        )

        val status = provider.observeStatus("business-10dlc-pending").first()

        assertTrue(status.senderVerified)
        assertFalse(status.tenDlcRegistered)
        assertEquals(Instant.parse("2026-04-17T00:00:00Z"), status.updatedAt)
    }

    private class FakeBusinessComplianceDao : BusinessComplianceDao {
        private val state = MutableStateFlow<Map<String, BusinessComplianceEntity>>(emptyMap())

        override suspend fun upsert(entity: BusinessComplianceEntity) {
            state.value = state.value + (entity.conversationId to entity)
        }

        override fun observeStatus(conversationId: String): Flow<BusinessComplianceEntity?> =
            state.map { entities -> entities[conversationId] }

        override suspend fun findByConversationId(conversationId: String): BusinessComplianceEntity? =
            state.value[conversationId]
    }
}
