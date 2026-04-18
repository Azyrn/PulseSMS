package com.skeler.pulse.database.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "encrypted_messages")
data class EncryptedMessageEntity(
    @PrimaryKey
    val messageId: String,
    val schemaVersion: Int,
    val conversationId: String,
    val bodyCiphertext: String,
    val bodyPreview: String,
    val payloadStoragePolicy: String,
    val sentAtEpochMillis: Long?,
    val receivedAtEpochMillis: Long?,
    val queueKey: String,
    val dedupeKey: String,
    val attempt: Int,
    val maxAttempts: Int,
    val nextRetryAtEpochMillis: Long?,
    val lastFailureCode: String?,
    val syncCompletedAtEpochMillis: Long?,
    val messageCorrelationId: String,
)
