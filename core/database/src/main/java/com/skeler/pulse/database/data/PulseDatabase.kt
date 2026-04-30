package com.skeler.pulse.database.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [EncryptedMessageEntity::class, BusinessComplianceEntity::class],
    version = PulseDatabase.VERSION,
    exportSchema = false,
)
abstract class PulseDatabase : RoomDatabase() {
    abstract fun encryptedMessageDao(): EncryptedMessageDao
    abstract fun businessComplianceDao(): BusinessComplianceDao

    companion object {
        const val NAME: String = "pulse.db"
        const val VERSION: Int = 5
    }
}
