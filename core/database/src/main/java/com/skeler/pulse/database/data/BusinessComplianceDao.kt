package com.skeler.pulse.database.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessComplianceDao {
    @Upsert
    suspend fun upsert(entity: BusinessComplianceEntity)

    @Query(
        """
        SELECT * FROM business_compliance
        WHERE conversationId = :conversationId
        LIMIT 1
        """
    )
    fun observeStatus(conversationId: String): Flow<BusinessComplianceEntity?>

    @Query(
        """
        SELECT * FROM business_compliance
        WHERE conversationId = :conversationId
        LIMIT 1
        """
    )
    suspend fun findByConversationId(conversationId: String): BusinessComplianceEntity?
}
