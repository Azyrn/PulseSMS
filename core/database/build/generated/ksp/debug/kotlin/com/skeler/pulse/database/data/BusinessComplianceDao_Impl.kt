package com.skeler.pulse.database.`data`

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.EntityUpsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class BusinessComplianceDao_Impl(
  __db: RoomDatabase,
) : BusinessComplianceDao {
  private val __db: RoomDatabase

  private val __upsertAdapterOfBusinessComplianceEntity:
      EntityUpsertAdapter<BusinessComplianceEntity>
  init {
    this.__db = __db
    this.__upsertAdapterOfBusinessComplianceEntity =
        EntityUpsertAdapter<BusinessComplianceEntity>(object :
        EntityInsertAdapter<BusinessComplianceEntity>() {
      protected override fun createQuery(): String =
          "INSERT INTO `business_compliance` (`conversationId`,`schemaVersion`,`senderVerified`,`recipientVerified`,`identityVerified`,`tenDlcRegistered`,`updatedAtEpochMillis`) VALUES (?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: BusinessComplianceEntity) {
        statement.bindText(1, entity.conversationId)
        statement.bindLong(2, entity.schemaVersion.toLong())
        val _tmp: Int = if (entity.senderVerified) 1 else 0
        statement.bindLong(3, _tmp.toLong())
        val _tmp_1: Int = if (entity.recipientVerified) 1 else 0
        statement.bindLong(4, _tmp_1.toLong())
        val _tmp_2: Int = if (entity.identityVerified) 1 else 0
        statement.bindLong(5, _tmp_2.toLong())
        val _tmp_3: Int = if (entity.tenDlcRegistered) 1 else 0
        statement.bindLong(6, _tmp_3.toLong())
        statement.bindLong(7, entity.updatedAtEpochMillis)
      }
    }, object : EntityDeleteOrUpdateAdapter<BusinessComplianceEntity>() {
      protected override fun createQuery(): String =
          "UPDATE `business_compliance` SET `conversationId` = ?,`schemaVersion` = ?,`senderVerified` = ?,`recipientVerified` = ?,`identityVerified` = ?,`tenDlcRegistered` = ?,`updatedAtEpochMillis` = ? WHERE `conversationId` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: BusinessComplianceEntity) {
        statement.bindText(1, entity.conversationId)
        statement.bindLong(2, entity.schemaVersion.toLong())
        val _tmp: Int = if (entity.senderVerified) 1 else 0
        statement.bindLong(3, _tmp.toLong())
        val _tmp_1: Int = if (entity.recipientVerified) 1 else 0
        statement.bindLong(4, _tmp_1.toLong())
        val _tmp_2: Int = if (entity.identityVerified) 1 else 0
        statement.bindLong(5, _tmp_2.toLong())
        val _tmp_3: Int = if (entity.tenDlcRegistered) 1 else 0
        statement.bindLong(6, _tmp_3.toLong())
        statement.bindLong(7, entity.updatedAtEpochMillis)
        statement.bindText(8, entity.conversationId)
      }
    })
  }

  public override suspend fun upsert(entity: BusinessComplianceEntity): Unit =
      performSuspending(__db, false, true) { _connection ->
    __upsertAdapterOfBusinessComplianceEntity.upsert(_connection, entity)
  }

  public override fun observeStatus(conversationId: String): Flow<BusinessComplianceEntity?> {
    val _sql: String = """
        |
        |        SELECT * FROM business_compliance
        |        WHERE conversationId = ?
        |        LIMIT 1
        |        
        """.trimMargin()
    return createFlow(__db, false, arrayOf("business_compliance")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, conversationId)
        val _columnIndexOfConversationId: Int = getColumnIndexOrThrow(_stmt, "conversationId")
        val _columnIndexOfSchemaVersion: Int = getColumnIndexOrThrow(_stmt, "schemaVersion")
        val _columnIndexOfSenderVerified: Int = getColumnIndexOrThrow(_stmt, "senderVerified")
        val _columnIndexOfRecipientVerified: Int = getColumnIndexOrThrow(_stmt, "recipientVerified")
        val _columnIndexOfIdentityVerified: Int = getColumnIndexOrThrow(_stmt, "identityVerified")
        val _columnIndexOfTenDlcRegistered: Int = getColumnIndexOrThrow(_stmt, "tenDlcRegistered")
        val _columnIndexOfUpdatedAtEpochMillis: Int = getColumnIndexOrThrow(_stmt,
            "updatedAtEpochMillis")
        val _result: BusinessComplianceEntity?
        if (_stmt.step()) {
          val _tmpConversationId: String
          _tmpConversationId = _stmt.getText(_columnIndexOfConversationId)
          val _tmpSchemaVersion: Int
          _tmpSchemaVersion = _stmt.getLong(_columnIndexOfSchemaVersion).toInt()
          val _tmpSenderVerified: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfSenderVerified).toInt()
          _tmpSenderVerified = _tmp != 0
          val _tmpRecipientVerified: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfRecipientVerified).toInt()
          _tmpRecipientVerified = _tmp_1 != 0
          val _tmpIdentityVerified: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfIdentityVerified).toInt()
          _tmpIdentityVerified = _tmp_2 != 0
          val _tmpTenDlcRegistered: Boolean
          val _tmp_3: Int
          _tmp_3 = _stmt.getLong(_columnIndexOfTenDlcRegistered).toInt()
          _tmpTenDlcRegistered = _tmp_3 != 0
          val _tmpUpdatedAtEpochMillis: Long
          _tmpUpdatedAtEpochMillis = _stmt.getLong(_columnIndexOfUpdatedAtEpochMillis)
          _result =
              BusinessComplianceEntity(_tmpConversationId,_tmpSchemaVersion,_tmpSenderVerified,_tmpRecipientVerified,_tmpIdentityVerified,_tmpTenDlcRegistered,_tmpUpdatedAtEpochMillis)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun findByConversationId(conversationId: String):
      BusinessComplianceEntity? {
    val _sql: String = """
        |
        |        SELECT * FROM business_compliance
        |        WHERE conversationId = ?
        |        LIMIT 1
        |        
        """.trimMargin()
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, conversationId)
        val _columnIndexOfConversationId: Int = getColumnIndexOrThrow(_stmt, "conversationId")
        val _columnIndexOfSchemaVersion: Int = getColumnIndexOrThrow(_stmt, "schemaVersion")
        val _columnIndexOfSenderVerified: Int = getColumnIndexOrThrow(_stmt, "senderVerified")
        val _columnIndexOfRecipientVerified: Int = getColumnIndexOrThrow(_stmt, "recipientVerified")
        val _columnIndexOfIdentityVerified: Int = getColumnIndexOrThrow(_stmt, "identityVerified")
        val _columnIndexOfTenDlcRegistered: Int = getColumnIndexOrThrow(_stmt, "tenDlcRegistered")
        val _columnIndexOfUpdatedAtEpochMillis: Int = getColumnIndexOrThrow(_stmt,
            "updatedAtEpochMillis")
        val _result: BusinessComplianceEntity?
        if (_stmt.step()) {
          val _tmpConversationId: String
          _tmpConversationId = _stmt.getText(_columnIndexOfConversationId)
          val _tmpSchemaVersion: Int
          _tmpSchemaVersion = _stmt.getLong(_columnIndexOfSchemaVersion).toInt()
          val _tmpSenderVerified: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfSenderVerified).toInt()
          _tmpSenderVerified = _tmp != 0
          val _tmpRecipientVerified: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfRecipientVerified).toInt()
          _tmpRecipientVerified = _tmp_1 != 0
          val _tmpIdentityVerified: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfIdentityVerified).toInt()
          _tmpIdentityVerified = _tmp_2 != 0
          val _tmpTenDlcRegistered: Boolean
          val _tmp_3: Int
          _tmp_3 = _stmt.getLong(_columnIndexOfTenDlcRegistered).toInt()
          _tmpTenDlcRegistered = _tmp_3 != 0
          val _tmpUpdatedAtEpochMillis: Long
          _tmpUpdatedAtEpochMillis = _stmt.getLong(_columnIndexOfUpdatedAtEpochMillis)
          _result =
              BusinessComplianceEntity(_tmpConversationId,_tmpSchemaVersion,_tmpSenderVerified,_tmpRecipientVerified,_tmpIdentityVerified,_tmpTenDlcRegistered,_tmpUpdatedAtEpochMillis)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
