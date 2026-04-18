package com.skeler.pulse.database.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "business_compliance")
data class BusinessComplianceEntity(
    @PrimaryKey
    val conversationId: String,
    val schemaVersion: Int,
    val senderVerified: Boolean,
    val recipientVerified: Boolean,
    val identityVerified: Boolean,
    val tenDlcRegistered: Boolean,
    val updatedAtEpochMillis: Long,
)
