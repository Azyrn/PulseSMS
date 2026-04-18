package com.skeler.pulse.database.`data`

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class PulseDatabase_Impl : PulseDatabase() {
  private val _encryptedMessageDao: Lazy<EncryptedMessageDao> = lazy {
    EncryptedMessageDao_Impl(this)
  }

  private val _businessComplianceDao: Lazy<BusinessComplianceDao> = lazy {
    BusinessComplianceDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(4,
        "ca4013b6fda801a9a12848e5c4af029b", "6725e48e4b7b69a49b37942553ef3991") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `encrypted_messages` (`messageId` TEXT NOT NULL, `schemaVersion` INTEGER NOT NULL, `conversationId` TEXT NOT NULL, `bodyCiphertext` TEXT NOT NULL, `bodyPreview` TEXT NOT NULL, `payloadStoragePolicy` TEXT NOT NULL, `sentAtEpochMillis` INTEGER, `receivedAtEpochMillis` INTEGER, `queueKey` TEXT NOT NULL, `dedupeKey` TEXT NOT NULL, `attempt` INTEGER NOT NULL, `maxAttempts` INTEGER NOT NULL, `nextRetryAtEpochMillis` INTEGER, `lastFailureCode` TEXT, `syncCompletedAtEpochMillis` INTEGER, `messageCorrelationId` TEXT NOT NULL, PRIMARY KEY(`messageId`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `business_compliance` (`conversationId` TEXT NOT NULL, `schemaVersion` INTEGER NOT NULL, `senderVerified` INTEGER NOT NULL, `recipientVerified` INTEGER NOT NULL, `identityVerified` INTEGER NOT NULL, `tenDlcRegistered` INTEGER NOT NULL, `updatedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`conversationId`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'ca4013b6fda801a9a12848e5c4af029b')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `encrypted_messages`")
        connection.execSQL("DROP TABLE IF EXISTS `business_compliance`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection):
          RoomOpenDelegate.ValidationResult {
        val _columnsEncryptedMessages: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsEncryptedMessages.put("messageId", TableInfo.Column("messageId", "TEXT", true, 1,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsEncryptedMessages.put("schemaVersion", TableInfo.Column("schemaVersion", "INTEGER",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsEncryptedMessages.put("conversationId", TableInfo.Column("conversationId", "TEXT",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsEncryptedMessages.put("bodyCiphertext", TableInfo.Column("bodyCiphertext", "TEXT",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsEncryptedMessages.put("bodyPreview", TableInfo.Column("bodyPreview", "TEXT", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsEncryptedMessages.put("payloadStoragePolicy",
            TableInfo.Column("payloadStoragePolicy", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsEncryptedMessages.put("sentAtEpochMillis", TableInfo.Column("sentAtEpochMillis",
            "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsEncryptedMessages.put("receivedAtEpochMillis",
            TableInfo.Column("receivedAtEpochMillis", "INTEGER", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsEncryptedMessages.put("queueKey", TableInfo.Column("queueKey", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsEncryptedMessages.put("dedupeKey", TableInfo.Column("dedupeKey", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsEncryptedMessages.put("attempt", TableInfo.Column("attempt", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsEncryptedMessages.put("maxAttempts", TableInfo.Column("maxAttempts", "INTEGER",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsEncryptedMessages.put("nextRetryAtEpochMillis",
            TableInfo.Column("nextRetryAtEpochMillis", "INTEGER", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsEncryptedMessages.put("lastFailureCode", TableInfo.Column("lastFailureCode", "TEXT",
            false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsEncryptedMessages.put("syncCompletedAtEpochMillis",
            TableInfo.Column("syncCompletedAtEpochMillis", "INTEGER", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsEncryptedMessages.put("messageCorrelationId",
            TableInfo.Column("messageCorrelationId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysEncryptedMessages: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesEncryptedMessages: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoEncryptedMessages: TableInfo = TableInfo("encrypted_messages",
            _columnsEncryptedMessages, _foreignKeysEncryptedMessages, _indicesEncryptedMessages)
        val _existingEncryptedMessages: TableInfo = read(connection, "encrypted_messages")
        if (!_infoEncryptedMessages.equals(_existingEncryptedMessages)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |encrypted_messages(com.skeler.pulse.database.data.EncryptedMessageEntity).
              | Expected:
              |""".trimMargin() + _infoEncryptedMessages + """
              |
              | Found:
              |""".trimMargin() + _existingEncryptedMessages)
        }
        val _columnsBusinessCompliance: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsBusinessCompliance.put("conversationId", TableInfo.Column("conversationId", "TEXT",
            true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBusinessCompliance.put("schemaVersion", TableInfo.Column("schemaVersion", "INTEGER",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBusinessCompliance.put("senderVerified", TableInfo.Column("senderVerified",
            "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBusinessCompliance.put("recipientVerified", TableInfo.Column("recipientVerified",
            "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBusinessCompliance.put("identityVerified", TableInfo.Column("identityVerified",
            "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBusinessCompliance.put("tenDlcRegistered", TableInfo.Column("tenDlcRegistered",
            "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBusinessCompliance.put("updatedAtEpochMillis",
            TableInfo.Column("updatedAtEpochMillis", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysBusinessCompliance: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesBusinessCompliance: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoBusinessCompliance: TableInfo = TableInfo("business_compliance",
            _columnsBusinessCompliance, _foreignKeysBusinessCompliance, _indicesBusinessCompliance)
        val _existingBusinessCompliance: TableInfo = read(connection, "business_compliance")
        if (!_infoBusinessCompliance.equals(_existingBusinessCompliance)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |business_compliance(com.skeler.pulse.database.data.BusinessComplianceEntity).
              | Expected:
              |""".trimMargin() + _infoBusinessCompliance + """
              |
              | Found:
              |""".trimMargin() + _existingBusinessCompliance)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "encrypted_messages",
        "business_compliance")
  }

  public override fun clearAllTables() {
    super.performClear(false, "encrypted_messages", "business_compliance")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(EncryptedMessageDao::class,
        EncryptedMessageDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(BusinessComplianceDao::class,
        BusinessComplianceDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override
      fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun encryptedMessageDao(): EncryptedMessageDao = _encryptedMessageDao.value

  public override fun businessComplianceDao(): BusinessComplianceDao = _businessComplianceDao.value
}
