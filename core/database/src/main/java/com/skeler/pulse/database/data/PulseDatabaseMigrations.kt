package com.skeler.pulse.database.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object PulseDatabaseMigrations {
    val MIGRATION_1_2: Migration =
        object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE encrypted_messages
                    ADD COLUMN messageCorrelationId TEXT NOT NULL DEFAULT ''
                    """.trimIndent()
                )
            }
        }

    val MIGRATION_2_3: Migration =
        object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS business_compliance (
                        conversationId TEXT NOT NULL PRIMARY KEY,
                        schemaVersion INTEGER NOT NULL,
                        senderVerified INTEGER NOT NULL,
                        recipientVerified INTEGER NOT NULL,
                        identityVerified INTEGER NOT NULL,
                        tenDlcRegistered INTEGER NOT NULL,
                        updatedAtEpochMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

    val MIGRATION_3_4: Migration =
        object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE encrypted_messages
                    ADD COLUMN syncCompletedAtEpochMillis INTEGER
                    """.trimIndent()
                )
            }
        }

    val MIGRATION_4_5: Migration =
        object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE encrypted_messages
                    ADD COLUMN bodyKeyAlias TEXT NOT NULL DEFAULT ''
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    ALTER TABLE encrypted_messages
                    ADD COLUMN bodyInitializationVector TEXT NOT NULL DEFAULT ''
                    """.trimIndent()
                )
            }
        }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
}
