package com.skeler.pulse.sms

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ScheduledMessageEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class ScheduledMessageDatabase : RoomDatabase() {
    abstract fun scheduledMessageDao(): ScheduledMessageDao

    companion object {
        @Volatile
        private var INSTANCE: ScheduledMessageDatabase? = null

        fun getInstance(context: Context): ScheduledMessageDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ScheduledMessageDatabase::class.java,
                    "pulse_scheduled_messages.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
