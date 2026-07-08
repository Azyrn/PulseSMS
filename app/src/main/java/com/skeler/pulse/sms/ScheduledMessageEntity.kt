package com.skeler.pulse.sms

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scheduled_messages")
data class ScheduledMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val address: String,
    val body: String,
    @ColumnInfo(name = "scheduled_at_millis")
    val scheduledAtMillis: Long,
    @ColumnInfo(name = "is_sent")
    val isSent: Boolean = false,
    @ColumnInfo(name = "is_cancelled")
    val isCancelled: Boolean = false,
    @ColumnInfo(name = "subscription_id")
    val subscriptionId: Int? = null,
    @ColumnInfo(name = "created_at_millis")
    val createdAtMillis: Long = System.currentTimeMillis(),
)
