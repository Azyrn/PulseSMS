package com.skeler.pulse.database.`data`

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.EntityUpsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.getTotalChangedRows
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class EncryptedMessageDao_Impl(
  __db: RoomDatabase,
) : EncryptedMessageDao {
  private val __db: RoomDatabase

  private val __upsertAdapterOfEncryptedMessageEntity: EntityUpsertAdapter<EncryptedMessageEntity>
  init {
    this.__db = __db
    this.__upsertAdapterOfEncryptedMessageEntity =
        EntityUpsertAdapter<EncryptedMessageEntity>(object :
        EntityInsertAdapter<EncryptedMessageEntity>() {
      protected override fun createQuery(): String =
          "INSERT INTO `encrypted_messages` (`messageId`,`schemaVersion`,`conversationId`,`bodyCiphertext`,`bodyPreview`,`payloadStoragePolicy`,`sentAtEpochMillis`,`receivedAtEpochMillis`,`queueKey`,`dedupeKey`,`attempt`,`maxAttempts`,`nextRetryAtEpochMillis`,`lastFailureCode`,`syncCompletedAtEpochMillis`,`messageCorrelationId`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: EncryptedMessageEntity) {
        statement.bindText(1, entity.messageId)
        statement.bindLong(2, entity.schemaVersion.toLong())
        statement.bindText(3, entity.conversationId)
        statement.bindText(4, entity.bodyCiphertext)
        statement.bindText(5, entity.bodyPreview)
        statement.bindText(6, entity.payloadStoragePolicy)
        val _tmpSentAtEpochMillis: Long? = entity.sentAtEpochMillis
        if (_tmpSentAtEpochMillis == null) {
          statement.bindNull(7)
        } else {
          statement.bindLong(7, _tmpSentAtEpochMillis)
        }
        val _tmpReceivedAtEpochMillis: Long? = entity.receivedAtEpochMillis
        if (_tmpReceivedAtEpochMillis == null) {
          statement.bindNull(8)
        } else {
          statement.bindLong(8, _tmpReceivedAtEpochMillis)
        }
        statement.bindText(9, entity.queueKey)
        statement.bindText(10, entity.dedupeKey)
        statement.bindLong(11, entity.attempt.toLong())
        statement.bindLong(12, entity.maxAttempts.toLong())
        val _tmpNextRetryAtEpochMillis: Long? = entity.nextRetryAtEpochMillis
        if (_tmpNextRetryAtEpochMillis == null) {
          statement.bindNull(13)
        } else {
          statement.bindLong(13, _tmpNextRetryAtEpochMillis)
        }
        val _tmpLastFailureCode: String? = entity.lastFailureCode
        if (_tmpLastFailureCode == null) {
          statement.bindNull(14)
        } else {
          statement.bindText(14, _tmpLastFailureCode)
        }
        val _tmpSyncCompletedAtEpochMillis: Long? = entity.syncCompletedAtEpochMillis
        if (_tmpSyncCompletedAtEpochMillis == null) {
          statement.bindNull(15)
        } else {
          statement.bindLong(15, _tmpSyncCompletedAtEpochMillis)
        }
        statement.bindText(16, entity.messageCorrelationId)
      }
    }, object : EntityDeleteOrUpdateAdapter<EncryptedMessageEntity>() {
      protected override fun createQuery(): String =
          "UPDATE `encrypted_messages` SET `messageId` = ?,`schemaVersion` = ?,`conversationId` = ?,`bodyCiphertext` = ?,`bodyPreview` = ?,`payloadStoragePolicy` = ?,`sentAtEpochMillis` = ?,`receivedAtEpochMillis` = ?,`queueKey` = ?,`dedupeKey` = ?,`attempt` = ?,`maxAttempts` = ?,`nextRetryAtEpochMillis` = ?,`lastFailureCode` = ?,`syncCompletedAtEpochMillis` = ?,`messageCorrelationId` = ? WHERE `messageId` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: EncryptedMessageEntity) {
        statement.bindText(1, entity.messageId)
        statement.bindLong(2, entity.schemaVersion.toLong())
        statement.bindText(3, entity.conversationId)
        statement.bindText(4, entity.bodyCiphertext)
        statement.bindText(5, entity.bodyPreview)
        statement.bindText(6, entity.payloadStoragePolicy)
        val _tmpSentAtEpochMillis: Long? = entity.sentAtEpochMillis
        if (_tmpSentAtEpochMillis == null) {
          statement.bindNull(7)
        } else {
          statement.bindLong(7, _tmpSentAtEpochMillis)
        }
        val _tmpReceivedAtEpochMillis: Long? = entity.receivedAtEpochMillis
        if (_tmpReceivedAtEpochMillis == null) {
          statement.bindNull(8)
        } else {
          statement.bindLong(8, _tmpReceivedAtEpochMillis)
        }
        statement.bindText(9, entity.queueKey)
        statement.bindText(10, entity.dedupeKey)
        statement.bindLong(11, entity.attempt.toLong())
        statement.bindLong(12, entity.maxAttempts.toLong())
        val _tmpNextRetryAtEpochMillis: Long? = entity.nextRetryAtEpochMillis
        if (_tmpNextRetryAtEpochMillis == null) {
          statement.bindNull(13)
        } else {
          statement.bindLong(13, _tmpNextRetryAtEpochMillis)
        }
        val _tmpLastFailureCode: String? = entity.lastFailureCode
        if (_tmpLastFailureCode == null) {
          statement.bindNull(14)
        } else {
          statement.bindText(14, _tmpLastFailureCode)
        }
        val _tmpSyncCompletedAtEpochMillis: Long? = entity.syncCompletedAtEpochMillis
        if (_tmpSyncCompletedAtEpochMillis == null) {
          statement.bindNull(15)
        } else {
          statement.bindLong(15, _tmpSyncCompletedAtEpochMillis)
        }
        statement.bindText(16, entity.messageCorrelationId)
        statement.bindText(17, entity.messageId)
      }
    })
  }

  public override suspend fun upsert(entity: EncryptedMessageEntity): Unit = performSuspending(__db,
      false, true) { _connection ->
    __upsertAdapterOfEncryptedMessageEntity.upsert(_connection, entity)
  }

  public override fun observeConversation(conversationId: String):
      Flow<List<EncryptedMessageEntity>> {
    val _sql: String = """
        |
        |        SELECT * FROM encrypted_messages
        |        WHERE conversationId = ?
        |        ORDER BY COALESCE(sentAtEpochMillis, receivedAtEpochMillis, 0) ASC, messageId ASC
        |        
        """.trimMargin()
    return createFlow(__db, false, arrayOf("encrypted_messages")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, conversationId)
        val _columnIndexOfMessageId: Int = getColumnIndexOrThrow(_stmt, "messageId")
        val _columnIndexOfSchemaVersion: Int = getColumnIndexOrThrow(_stmt, "schemaVersion")
        val _columnIndexOfConversationId: Int = getColumnIndexOrThrow(_stmt, "conversationId")
        val _columnIndexOfBodyCiphertext: Int = getColumnIndexOrThrow(_stmt, "bodyCiphertext")
        val _columnIndexOfBodyPreview: Int = getColumnIndexOrThrow(_stmt, "bodyPreview")
        val _columnIndexOfPayloadStoragePolicy: Int = getColumnIndexOrThrow(_stmt,
            "payloadStoragePolicy")
        val _columnIndexOfSentAtEpochMillis: Int = getColumnIndexOrThrow(_stmt, "sentAtEpochMillis")
        val _columnIndexOfReceivedAtEpochMillis: Int = getColumnIndexOrThrow(_stmt,
            "receivedAtEpochMillis")
        val _columnIndexOfQueueKey: Int = getColumnIndexOrThrow(_stmt, "queueKey")
        val _columnIndexOfDedupeKey: Int = getColumnIndexOrThrow(_stmt, "dedupeKey")
        val _columnIndexOfAttempt: Int = getColumnIndexOrThrow(_stmt, "attempt")
        val _columnIndexOfMaxAttempts: Int = getColumnIndexOrThrow(_stmt, "maxAttempts")
        val _columnIndexOfNextRetryAtEpochMillis: Int = getColumnIndexOrThrow(_stmt,
            "nextRetryAtEpochMillis")
        val _columnIndexOfLastFailureCode: Int = getColumnIndexOrThrow(_stmt, "lastFailureCode")
        val _columnIndexOfSyncCompletedAtEpochMillis: Int = getColumnIndexOrThrow(_stmt,
            "syncCompletedAtEpochMillis")
        val _columnIndexOfMessageCorrelationId: Int = getColumnIndexOrThrow(_stmt,
            "messageCorrelationId")
        val _result: MutableList<EncryptedMessageEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: EncryptedMessageEntity
          val _tmpMessageId: String
          _tmpMessageId = _stmt.getText(_columnIndexOfMessageId)
          val _tmpSchemaVersion: Int
          _tmpSchemaVersion = _stmt.getLong(_columnIndexOfSchemaVersion).toInt()
          val _tmpConversationId: String
          _tmpConversationId = _stmt.getText(_columnIndexOfConversationId)
          val _tmpBodyCiphertext: String
          _tmpBodyCiphertext = _stmt.getText(_columnIndexOfBodyCiphertext)
          val _tmpBodyPreview: String
          _tmpBodyPreview = _stmt.getText(_columnIndexOfBodyPreview)
          val _tmpPayloadStoragePolicy: String
          _tmpPayloadStoragePolicy = _stmt.getText(_columnIndexOfPayloadStoragePolicy)
          val _tmpSentAtEpochMillis: Long?
          if (_stmt.isNull(_columnIndexOfSentAtEpochMillis)) {
            _tmpSentAtEpochMillis = null
          } else {
            _tmpSentAtEpochMillis = _stmt.getLong(_columnIndexOfSentAtEpochMillis)
          }
          val _tmpReceivedAtEpochMillis: Long?
          if (_stmt.isNull(_columnIndexOfReceivedAtEpochMillis)) {
            _tmpReceivedAtEpochMillis = null
          } else {
            _tmpReceivedAtEpochMillis = _stmt.getLong(_columnIndexOfReceivedAtEpochMillis)
          }
          val _tmpQueueKey: String
          _tmpQueueKey = _stmt.getText(_columnIndexOfQueueKey)
          val _tmpDedupeKey: String
          _tmpDedupeKey = _stmt.getText(_columnIndexOfDedupeKey)
          val _tmpAttempt: Int
          _tmpAttempt = _stmt.getLong(_columnIndexOfAttempt).toInt()
          val _tmpMaxAttempts: Int
          _tmpMaxAttempts = _stmt.getLong(_columnIndexOfMaxAttempts).toInt()
          val _tmpNextRetryAtEpochMillis: Long?
          if (_stmt.isNull(_columnIndexOfNextRetryAtEpochMillis)) {
            _tmpNextRetryAtEpochMillis = null
          } else {
            _tmpNextRetryAtEpochMillis = _stmt.getLong(_columnIndexOfNextRetryAtEpochMillis)
          }
          val _tmpLastFailureCode: String?
          if (_stmt.isNull(_columnIndexOfLastFailureCode)) {
            _tmpLastFailureCode = null
          } else {
            _tmpLastFailureCode = _stmt.getText(_columnIndexOfLastFailureCode)
          }
          val _tmpSyncCompletedAtEpochMillis: Long?
          if (_stmt.isNull(_columnIndexOfSyncCompletedAtEpochMillis)) {
            _tmpSyncCompletedAtEpochMillis = null
          } else {
            _tmpSyncCompletedAtEpochMillis = _stmt.getLong(_columnIndexOfSyncCompletedAtEpochMillis)
          }
          val _tmpMessageCorrelationId: String
          _tmpMessageCorrelationId = _stmt.getText(_columnIndexOfMessageCorrelationId)
          _item =
              EncryptedMessageEntity(_tmpMessageId,_tmpSchemaVersion,_tmpConversationId,_tmpBodyCiphertext,_tmpBodyPreview,_tmpPayloadStoragePolicy,_tmpSentAtEpochMillis,_tmpReceivedAtEpochMillis,_tmpQueueKey,_tmpDedupeKey,_tmpAttempt,_tmpMaxAttempts,_tmpNextRetryAtEpochMillis,_tmpLastFailureCode,_tmpSyncCompletedAtEpochMillis,_tmpMessageCorrelationId)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeAllMessages(): Flow<List<EncryptedMessageEntity>> {
    val _sql: String = """
        |
        |        SELECT * FROM encrypted_messages
        |        ORDER BY COALESCE(sentAtEpochMillis, receivedAtEpochMillis, 0) DESC, messageId DESC
        |        
        """.trimMargin()
    return createFlow(__db, false, arrayOf("encrypted_messages")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfMessageId: Int = getColumnIndexOrThrow(_stmt, "messageId")
        val _columnIndexOfSchemaVersion: Int = getColumnIndexOrThrow(_stmt, "schemaVersion")
        val _columnIndexOfConversationId: Int = getColumnIndexOrThrow(_stmt, "conversationId")
        val _columnIndexOfBodyCiphertext: Int = getColumnIndexOrThrow(_stmt, "bodyCiphertext")
        val _columnIndexOfBodyPreview: Int = getColumnIndexOrThrow(_stmt, "bodyPreview")
        val _columnIndexOfPayloadStoragePolicy: Int = getColumnIndexOrThrow(_stmt,
            "payloadStoragePolicy")
        val _columnIndexOfSentAtEpochMillis: Int = getColumnIndexOrThrow(_stmt, "sentAtEpochMillis")
        val _columnIndexOfReceivedAtEpochMillis: Int = getColumnIndexOrThrow(_stmt,
            "receivedAtEpochMillis")
        val _columnIndexOfQueueKey: Int = getColumnIndexOrThrow(_stmt, "queueKey")
        val _columnIndexOfDedupeKey: Int = getColumnIndexOrThrow(_stmt, "dedupeKey")
        val _columnIndexOfAttempt: Int = getColumnIndexOrThrow(_stmt, "attempt")
        val _columnIndexOfMaxAttempts: Int = getColumnIndexOrThrow(_stmt, "maxAttempts")
        val _columnIndexOfNextRetryAtEpochMillis: Int = getColumnIndexOrThrow(_stmt,
            "nextRetryAtEpochMillis")
        val _columnIndexOfLastFailureCode: Int = getColumnIndexOrThrow(_stmt, "lastFailureCode")
        val _columnIndexOfSyncCompletedAtEpochMillis: Int = getColumnIndexOrThrow(_stmt,
            "syncCompletedAtEpochMillis")
        val _columnIndexOfMessageCorrelationId: Int = getColumnIndexOrThrow(_stmt,
            "messageCorrelationId")
        val _result: MutableList<EncryptedMessageEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: EncryptedMessageEntity
          val _tmpMessageId: String
          _tmpMessageId = _stmt.getText(_columnIndexOfMessageId)
          val _tmpSchemaVersion: Int
          _tmpSchemaVersion = _stmt.getLong(_columnIndexOfSchemaVersion).toInt()
          val _tmpConversationId: String
          _tmpConversationId = _stmt.getText(_columnIndexOfConversationId)
          val _tmpBodyCiphertext: String
          _tmpBodyCiphertext = _stmt.getText(_columnIndexOfBodyCiphertext)
          val _tmpBodyPreview: String
          _tmpBodyPreview = _stmt.getText(_columnIndexOfBodyPreview)
          val _tmpPayloadStoragePolicy: String
          _tmpPayloadStoragePolicy = _stmt.getText(_columnIndexOfPayloadStoragePolicy)
          val _tmpSentAtEpochMillis: Long?
          if (_stmt.isNull(_columnIndexOfSentAtEpochMillis)) {
            _tmpSentAtEpochMillis = null
          } else {
            _tmpSentAtEpochMillis = _stmt.getLong(_columnIndexOfSentAtEpochMillis)
          }
          val _tmpReceivedAtEpochMillis: Long?
          if (_stmt.isNull(_columnIndexOfReceivedAtEpochMillis)) {
            _tmpReceivedAtEpochMillis = null
          } else {
            _tmpReceivedAtEpochMillis = _stmt.getLong(_columnIndexOfReceivedAtEpochMillis)
          }
          val _tmpQueueKey: String
          _tmpQueueKey = _stmt.getText(_columnIndexOfQueueKey)
          val _tmpDedupeKey: String
          _tmpDedupeKey = _stmt.getText(_columnIndexOfDedupeKey)
          val _tmpAttempt: Int
          _tmpAttempt = _stmt.getLong(_columnIndexOfAttempt).toInt()
          val _tmpMaxAttempts: Int
          _tmpMaxAttempts = _stmt.getLong(_columnIndexOfMaxAttempts).toInt()
          val _tmpNextRetryAtEpochMillis: Long?
          if (_stmt.isNull(_columnIndexOfNextRetryAtEpochMillis)) {
            _tmpNextRetryAtEpochMillis = null
          } else {
            _tmpNextRetryAtEpochMillis = _stmt.getLong(_columnIndexOfNextRetryAtEpochMillis)
          }
          val _tmpLastFailureCode: String?
          if (_stmt.isNull(_columnIndexOfLastFailureCode)) {
            _tmpLastFailureCode = null
          } else {
            _tmpLastFailureCode = _stmt.getText(_columnIndexOfLastFailureCode)
          }
          val _tmpSyncCompletedAtEpochMillis: Long?
          if (_stmt.isNull(_columnIndexOfSyncCompletedAtEpochMillis)) {
            _tmpSyncCompletedAtEpochMillis = null
          } else {
            _tmpSyncCompletedAtEpochMillis = _stmt.getLong(_columnIndexOfSyncCompletedAtEpochMillis)
          }
          val _tmpMessageCorrelationId: String
          _tmpMessageCorrelationId = _stmt.getText(_columnIndexOfMessageCorrelationId)
          _item =
              EncryptedMessageEntity(_tmpMessageId,_tmpSchemaVersion,_tmpConversationId,_tmpBodyCiphertext,_tmpBodyPreview,_tmpPayloadStoragePolicy,_tmpSentAtEpochMillis,_tmpReceivedAtEpochMillis,_tmpQueueKey,_tmpDedupeKey,_tmpAttempt,_tmpMaxAttempts,_tmpNextRetryAtEpochMillis,_tmpLastFailureCode,_tmpSyncCompletedAtEpochMillis,_tmpMessageCorrelationId)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun pendingSync(limit: Int): List<EncryptedMessageEntity> {
    val _sql: String = """
        |
        |        SELECT * FROM encrypted_messages
        |        WHERE syncCompletedAtEpochMillis IS NULL
        |          AND (lastFailureCode IS NULL OR nextRetryAtEpochMillis IS NOT NULL)
        |        ORDER BY
        |            CASE WHEN nextRetryAtEpochMillis IS NULL THEN 0 ELSE 1 END ASC,
        |            COALESCE(nextRetryAtEpochMillis, sentAtEpochMillis, receivedAtEpochMillis, 0) ASC,
        |            messageId ASC
        |        LIMIT ?
        |        
        """.trimMargin()
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfMessageId: Int = getColumnIndexOrThrow(_stmt, "messageId")
        val _columnIndexOfSchemaVersion: Int = getColumnIndexOrThrow(_stmt, "schemaVersion")
        val _columnIndexOfConversationId: Int = getColumnIndexOrThrow(_stmt, "conversationId")
        val _columnIndexOfBodyCiphertext: Int = getColumnIndexOrThrow(_stmt, "bodyCiphertext")
        val _columnIndexOfBodyPreview: Int = getColumnIndexOrThrow(_stmt, "bodyPreview")
        val _columnIndexOfPayloadStoragePolicy: Int = getColumnIndexOrThrow(_stmt,
            "payloadStoragePolicy")
        val _columnIndexOfSentAtEpochMillis: Int = getColumnIndexOrThrow(_stmt, "sentAtEpochMillis")
        val _columnIndexOfReceivedAtEpochMillis: Int = getColumnIndexOrThrow(_stmt,
            "receivedAtEpochMillis")
        val _columnIndexOfQueueKey: Int = getColumnIndexOrThrow(_stmt, "queueKey")
        val _columnIndexOfDedupeKey: Int = getColumnIndexOrThrow(_stmt, "dedupeKey")
        val _columnIndexOfAttempt: Int = getColumnIndexOrThrow(_stmt, "attempt")
        val _columnIndexOfMaxAttempts: Int = getColumnIndexOrThrow(_stmt, "maxAttempts")
        val _columnIndexOfNextRetryAtEpochMillis: Int = getColumnIndexOrThrow(_stmt,
            "nextRetryAtEpochMillis")
        val _columnIndexOfLastFailureCode: Int = getColumnIndexOrThrow(_stmt, "lastFailureCode")
        val _columnIndexOfSyncCompletedAtEpochMillis: Int = getColumnIndexOrThrow(_stmt,
            "syncCompletedAtEpochMillis")
        val _columnIndexOfMessageCorrelationId: Int = getColumnIndexOrThrow(_stmt,
            "messageCorrelationId")
        val _result: MutableList<EncryptedMessageEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: EncryptedMessageEntity
          val _tmpMessageId: String
          _tmpMessageId = _stmt.getText(_columnIndexOfMessageId)
          val _tmpSchemaVersion: Int
          _tmpSchemaVersion = _stmt.getLong(_columnIndexOfSchemaVersion).toInt()
          val _tmpConversationId: String
          _tmpConversationId = _stmt.getText(_columnIndexOfConversationId)
          val _tmpBodyCiphertext: String
          _tmpBodyCiphertext = _stmt.getText(_columnIndexOfBodyCiphertext)
          val _tmpBodyPreview: String
          _tmpBodyPreview = _stmt.getText(_columnIndexOfBodyPreview)
          val _tmpPayloadStoragePolicy: String
          _tmpPayloadStoragePolicy = _stmt.getText(_columnIndexOfPayloadStoragePolicy)
          val _tmpSentAtEpochMillis: Long?
          if (_stmt.isNull(_columnIndexOfSentAtEpochMillis)) {
            _tmpSentAtEpochMillis = null
          } else {
            _tmpSentAtEpochMillis = _stmt.getLong(_columnIndexOfSentAtEpochMillis)
          }
          val _tmpReceivedAtEpochMillis: Long?
          if (_stmt.isNull(_columnIndexOfReceivedAtEpochMillis)) {
            _tmpReceivedAtEpochMillis = null
          } else {
            _tmpReceivedAtEpochMillis = _stmt.getLong(_columnIndexOfReceivedAtEpochMillis)
          }
          val _tmpQueueKey: String
          _tmpQueueKey = _stmt.getText(_columnIndexOfQueueKey)
          val _tmpDedupeKey: String
          _tmpDedupeKey = _stmt.getText(_columnIndexOfDedupeKey)
          val _tmpAttempt: Int
          _tmpAttempt = _stmt.getLong(_columnIndexOfAttempt).toInt()
          val _tmpMaxAttempts: Int
          _tmpMaxAttempts = _stmt.getLong(_columnIndexOfMaxAttempts).toInt()
          val _tmpNextRetryAtEpochMillis: Long?
          if (_stmt.isNull(_columnIndexOfNextRetryAtEpochMillis)) {
            _tmpNextRetryAtEpochMillis = null
          } else {
            _tmpNextRetryAtEpochMillis = _stmt.getLong(_columnIndexOfNextRetryAtEpochMillis)
          }
          val _tmpLastFailureCode: String?
          if (_stmt.isNull(_columnIndexOfLastFailureCode)) {
            _tmpLastFailureCode = null
          } else {
            _tmpLastFailureCode = _stmt.getText(_columnIndexOfLastFailureCode)
          }
          val _tmpSyncCompletedAtEpochMillis: Long?
          if (_stmt.isNull(_columnIndexOfSyncCompletedAtEpochMillis)) {
            _tmpSyncCompletedAtEpochMillis = null
          } else {
            _tmpSyncCompletedAtEpochMillis = _stmt.getLong(_columnIndexOfSyncCompletedAtEpochMillis)
          }
          val _tmpMessageCorrelationId: String
          _tmpMessageCorrelationId = _stmt.getText(_columnIndexOfMessageCorrelationId)
          _item =
              EncryptedMessageEntity(_tmpMessageId,_tmpSchemaVersion,_tmpConversationId,_tmpBodyCiphertext,_tmpBodyPreview,_tmpPayloadStoragePolicy,_tmpSentAtEpochMillis,_tmpReceivedAtEpochMillis,_tmpQueueKey,_tmpDedupeKey,_tmpAttempt,_tmpMaxAttempts,_tmpNextRetryAtEpochMillis,_tmpLastFailureCode,_tmpSyncCompletedAtEpochMillis,_tmpMessageCorrelationId)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun findByMessageId(messageId: String): EncryptedMessageEntity? {
    val _sql: String = """
        |
        |        SELECT * FROM encrypted_messages
        |        WHERE messageId = ?
        |        LIMIT 1
        |        
        """.trimMargin()
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, messageId)
        val _columnIndexOfMessageId: Int = getColumnIndexOrThrow(_stmt, "messageId")
        val _columnIndexOfSchemaVersion: Int = getColumnIndexOrThrow(_stmt, "schemaVersion")
        val _columnIndexOfConversationId: Int = getColumnIndexOrThrow(_stmt, "conversationId")
        val _columnIndexOfBodyCiphertext: Int = getColumnIndexOrThrow(_stmt, "bodyCiphertext")
        val _columnIndexOfBodyPreview: Int = getColumnIndexOrThrow(_stmt, "bodyPreview")
        val _columnIndexOfPayloadStoragePolicy: Int = getColumnIndexOrThrow(_stmt,
            "payloadStoragePolicy")
        val _columnIndexOfSentAtEpochMillis: Int = getColumnIndexOrThrow(_stmt, "sentAtEpochMillis")
        val _columnIndexOfReceivedAtEpochMillis: Int = getColumnIndexOrThrow(_stmt,
            "receivedAtEpochMillis")
        val _columnIndexOfQueueKey: Int = getColumnIndexOrThrow(_stmt, "queueKey")
        val _columnIndexOfDedupeKey: Int = getColumnIndexOrThrow(_stmt, "dedupeKey")
        val _columnIndexOfAttempt: Int = getColumnIndexOrThrow(_stmt, "attempt")
        val _columnIndexOfMaxAttempts: Int = getColumnIndexOrThrow(_stmt, "maxAttempts")
        val _columnIndexOfNextRetryAtEpochMillis: Int = getColumnIndexOrThrow(_stmt,
            "nextRetryAtEpochMillis")
        val _columnIndexOfLastFailureCode: Int = getColumnIndexOrThrow(_stmt, "lastFailureCode")
        val _columnIndexOfSyncCompletedAtEpochMillis: Int = getColumnIndexOrThrow(_stmt,
            "syncCompletedAtEpochMillis")
        val _columnIndexOfMessageCorrelationId: Int = getColumnIndexOrThrow(_stmt,
            "messageCorrelationId")
        val _result: EncryptedMessageEntity?
        if (_stmt.step()) {
          val _tmpMessageId: String
          _tmpMessageId = _stmt.getText(_columnIndexOfMessageId)
          val _tmpSchemaVersion: Int
          _tmpSchemaVersion = _stmt.getLong(_columnIndexOfSchemaVersion).toInt()
          val _tmpConversationId: String
          _tmpConversationId = _stmt.getText(_columnIndexOfConversationId)
          val _tmpBodyCiphertext: String
          _tmpBodyCiphertext = _stmt.getText(_columnIndexOfBodyCiphertext)
          val _tmpBodyPreview: String
          _tmpBodyPreview = _stmt.getText(_columnIndexOfBodyPreview)
          val _tmpPayloadStoragePolicy: String
          _tmpPayloadStoragePolicy = _stmt.getText(_columnIndexOfPayloadStoragePolicy)
          val _tmpSentAtEpochMillis: Long?
          if (_stmt.isNull(_columnIndexOfSentAtEpochMillis)) {
            _tmpSentAtEpochMillis = null
          } else {
            _tmpSentAtEpochMillis = _stmt.getLong(_columnIndexOfSentAtEpochMillis)
          }
          val _tmpReceivedAtEpochMillis: Long?
          if (_stmt.isNull(_columnIndexOfReceivedAtEpochMillis)) {
            _tmpReceivedAtEpochMillis = null
          } else {
            _tmpReceivedAtEpochMillis = _stmt.getLong(_columnIndexOfReceivedAtEpochMillis)
          }
          val _tmpQueueKey: String
          _tmpQueueKey = _stmt.getText(_columnIndexOfQueueKey)
          val _tmpDedupeKey: String
          _tmpDedupeKey = _stmt.getText(_columnIndexOfDedupeKey)
          val _tmpAttempt: Int
          _tmpAttempt = _stmt.getLong(_columnIndexOfAttempt).toInt()
          val _tmpMaxAttempts: Int
          _tmpMaxAttempts = _stmt.getLong(_columnIndexOfMaxAttempts).toInt()
          val _tmpNextRetryAtEpochMillis: Long?
          if (_stmt.isNull(_columnIndexOfNextRetryAtEpochMillis)) {
            _tmpNextRetryAtEpochMillis = null
          } else {
            _tmpNextRetryAtEpochMillis = _stmt.getLong(_columnIndexOfNextRetryAtEpochMillis)
          }
          val _tmpLastFailureCode: String?
          if (_stmt.isNull(_columnIndexOfLastFailureCode)) {
            _tmpLastFailureCode = null
          } else {
            _tmpLastFailureCode = _stmt.getText(_columnIndexOfLastFailureCode)
          }
          val _tmpSyncCompletedAtEpochMillis: Long?
          if (_stmt.isNull(_columnIndexOfSyncCompletedAtEpochMillis)) {
            _tmpSyncCompletedAtEpochMillis = null
          } else {
            _tmpSyncCompletedAtEpochMillis = _stmt.getLong(_columnIndexOfSyncCompletedAtEpochMillis)
          }
          val _tmpMessageCorrelationId: String
          _tmpMessageCorrelationId = _stmt.getText(_columnIndexOfMessageCorrelationId)
          _result =
              EncryptedMessageEntity(_tmpMessageId,_tmpSchemaVersion,_tmpConversationId,_tmpBodyCiphertext,_tmpBodyPreview,_tmpPayloadStoragePolicy,_tmpSentAtEpochMillis,_tmpReceivedAtEpochMillis,_tmpQueueKey,_tmpDedupeKey,_tmpAttempt,_tmpMaxAttempts,_tmpNextRetryAtEpochMillis,_tmpLastFailureCode,_tmpSyncCompletedAtEpochMillis,_tmpMessageCorrelationId)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateSync(
    messageId: String,
    queueKey: String,
    dedupeKey: String,
    attempt: Int,
    maxAttempts: Int,
    nextRetryAtEpochMillis: Long?,
    lastFailureCode: String?,
    completedAtEpochMillis: Long?,
  ): Int {
    val _sql: String = """
        |
        |        UPDATE encrypted_messages
        |        SET queueKey = ?,
        |            dedupeKey = ?,
        |            attempt = ?,
        |            maxAttempts = ?,
        |            nextRetryAtEpochMillis = ?,
        |            lastFailureCode = ?,
        |            syncCompletedAtEpochMillis = ?
        |        WHERE messageId = ?
        |        
        """.trimMargin()
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, queueKey)
        _argIndex = 2
        _stmt.bindText(_argIndex, dedupeKey)
        _argIndex = 3
        _stmt.bindLong(_argIndex, attempt.toLong())
        _argIndex = 4
        _stmt.bindLong(_argIndex, maxAttempts.toLong())
        _argIndex = 5
        if (nextRetryAtEpochMillis == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindLong(_argIndex, nextRetryAtEpochMillis)
        }
        _argIndex = 6
        if (lastFailureCode == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, lastFailureCode)
        }
        _argIndex = 7
        if (completedAtEpochMillis == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindLong(_argIndex, completedAtEpochMillis)
        }
        _argIndex = 8
        _stmt.bindText(_argIndex, messageId)
        _stmt.step()
        getTotalChangedRows(_connection)
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
