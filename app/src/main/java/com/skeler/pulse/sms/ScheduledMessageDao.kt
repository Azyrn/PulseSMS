package com.skeler.pulse.sms

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledMessageDao {

    @Insert
    suspend fun insert(message: ScheduledMessageEntity): Long

    @Update
    suspend fun update(message: ScheduledMessageEntity)

    @Query("SELECT * FROM scheduled_messages WHERE id = :id")
    suspend fun findById(id: Long): ScheduledMessageEntity?

    @Query("SELECT * FROM scheduled_messages WHERE is_sent = 0 AND is_cancelled = 0 AND scheduled_at_millis <= :nowMillis")
    suspend fun pendingMessages(nowMillis: Long): List<ScheduledMessageEntity>

    @Query("SELECT * FROM scheduled_messages WHERE is_sent = 0 AND is_cancelled = 0 ORDER BY scheduled_at_millis ASC")
    fun observePending(): Flow<List<ScheduledMessageEntity>>

    @Query("SELECT * FROM scheduled_messages WHERE address = :address AND is_sent = 0 AND is_cancelled = 0 ORDER BY scheduled_at_millis ASC")
    fun observePendingForAddress(address: String): Flow<List<ScheduledMessageEntity>>

    @Query("UPDATE scheduled_messages SET is_sent = 1 WHERE id = :id")
    suspend fun markSent(id: Long)

    @Query("UPDATE scheduled_messages SET is_cancelled = 1 WHERE id = :id")
    suspend fun cancel(id: Long)

    @Query("SELECT * FROM scheduled_messages WHERE is_sent = 0 AND is_cancelled = 0 ORDER BY scheduled_at_millis ASC")
    suspend fun allPending(): List<ScheduledMessageEntity>

    @Query("DELETE FROM scheduled_messages WHERE is_sent = 1 AND created_at_millis < :beforeMillis")
    suspend fun clearSent(beforeMillis: Long)
}
