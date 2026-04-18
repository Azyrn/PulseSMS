package com.skeler.pulse.database.api

import android.content.Context
import androidx.room.Room
import com.skeler.pulse.database.data.PulseDatabase
import com.skeler.pulse.database.data.PulseDatabaseMigrations

object PulseDatabaseFactory {
    fun create(context: Context): PulseDatabase =
        Room.databaseBuilder(
            context,
            PulseDatabase::class.java,
            PulseDatabase.NAME,
        )
            .addMigrations(*PulseDatabaseMigrations.ALL)
            .build()
}
