package com.skeler.pulse.database.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface EncryptedMessageDao {
    @Upsert
    suspend fun upsert(entity: EncryptedMessageEntity)

    @Query(
        """
        SELECT * FROM encrypted_messages
        WHERE conversationId = :conversationId
        ORDER BY COALESCE(sentAtEpochMillis, receivedAtEpochMillis, 0) ASC, messageId ASC
        """
    )
    fun observeConversation(conversationId: String): Flow<List<EncryptedMessageEntity>>

    @Query(
        """
        SELECT * FROM encrypted_messages
        ORDER BY COALESCE(sentAtEpochMillis, receivedAtEpochMillis, 0) DESC, messageId DESC
        """
    )
    fun observeAllMessages(): Flow<List<EncryptedMessageEntity>>

    @Query(
        """
        SELECT * FROM encrypted_messages
        WHERE syncCompletedAtEpochMillis IS NULL
          AND (lastFailureCode IS NULL OR nextRetryAtEpochMillis IS NOT NULL)
        ORDER BY
            CASE WHEN nextRetryAtEpochMillis IS NULL THEN 0 ELSE 1 END ASC,
            COALESCE(nextRetryAtEpochMillis, sentAtEpochMillis, receivedAtEpochMillis, 0) ASC,
            messageId ASC
        LIMIT :limit
        """
    )
    suspend fun pendingSync(limit: Int): List<EncryptedMessageEntity>

    @Query(
        """
        SELECT * FROM encrypted_messages
        WHERE messageId = :messageId
        LIMIT 1
        """
    )
    suspend fun findByMessageId(messageId: String): EncryptedMessageEntity?

    @Query(
        """
        UPDATE encrypted_messages
        SET queueKey = :queueKey,
            dedupeKey = :dedupeKey,
            attempt = :attempt,
            maxAttempts = :maxAttempts,
            nextRetryAtEpochMillis = :nextRetryAtEpochMillis,
            lastFailureCode = :lastFailureCode,
            syncCompletedAtEpochMillis = :completedAtEpochMillis
        WHERE messageId = :messageId
        """
    )
    suspend fun updateSync(
        messageId: String,
        queueKey: String,
        dedupeKey: String,
        attempt: Int,
        maxAttempts: Int,
        nextRetryAtEpochMillis: Long?,
        lastFailureCode: String?,
        completedAtEpochMillis: Long?,
    ): Int
}
