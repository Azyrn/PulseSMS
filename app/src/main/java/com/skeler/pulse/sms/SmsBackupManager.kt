package com.skeler.pulse.sms

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.BaseColumns
import android.provider.Telephony
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import org.xmlpull.v1.XmlSerializer
import java.io.InputStream
import java.io.OutputStream

data class BackupMessage(
    val address: String,
    val body: String,
    val date: Long,
    val dateSent: Long?,
    val type: Int,
    val read: Int,
    val status: Int,
    val threadId: Long?,
)

class SmsBackupManager(
    private val context: Context,
    private val encryptionManager: SmsEncryptionManager,
) {

    private val contentResolver: ContentResolver get() = context.contentResolver

    fun exportSms(outputStream: OutputStream): Int {
        val serializer: XmlSerializer = XmlPullParserFactory.newInstance().newSerializer()
        serializer.setOutput(outputStream, "UTF-8")
        serializer.startDocument("UTF-8", true)

        var count = 0
        val messages = mutableListOf<CursorEntry>()
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.DATE_SENT,
            Telephony.Sms.TYPE,
            Telephony.Sms.READ,
            Telephony.Sms.STATUS,
            Telephony.Sms.THREAD_ID,
        )

        contentResolver.query(uri, projection, null, null, "${Telephony.Sms.DATE} ASC")?.use { cursor ->
            while (cursor.moveToNext()) {
                val address = cursor.getString(0) ?: ""
                val rawBody = cursor.getString(1) ?: ""
                val body = if (encryptionManager.isEncrypted(rawBody)) {
                    encryptionManager.decrypt(rawBody) ?: SmsEncryptionManager.KEY_LOST_PLACEHOLDER
                } else {
                    rawBody
                }
                val date = cursor.getLong(2)
                val dateSent = if (cursor.isNull(3)) null else cursor.getLong(3)
                val type = cursor.getInt(4)
                val read = cursor.getInt(5)
                val status = cursor.getInt(6)
                val threadId = if (cursor.isNull(7)) null else cursor.getLong(7)
                messages.add(CursorEntry(address, body, date, dateSent, type, read, status, threadId))
            }
        }

        serializer.startTag(null, "smses")
        serializer.attribute(null, "count", messages.size.toString())
        messages.forEach { msg ->
            serializer.startTag(null, "sms")
            serializer.attribute(null, "address", msg.address)
            serializer.attribute(null, "body", msg.body)
            serializer.attribute(null, "date", msg.date.toString())
            if (msg.dateSent != null) serializer.attribute(null, "date_sent", msg.dateSent.toString())
            serializer.attribute(null, "type", msg.type.toString())
            serializer.attribute(null, "read", msg.read.toString())
            serializer.attribute(null, "status", msg.status.toString())
            if (msg.threadId != null) serializer.attribute(null, "thread_id", msg.threadId.toString())
            serializer.endTag(null, "sms")
        }
        serializer.endTag(null, "smses")
        serializer.endDocument()
        return messages.size
    }

    private data class CursorEntry(
        val address: String,
        val body: String,
        val date: Long,
        val dateSent: Long?,
        val type: Int,
        val read: Int,
        val status: Int,
        val threadId: Long?,
    )

    fun importSms(inputStream: InputStream): Int {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(inputStream, "UTF-8")

        var count = 0
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "sms") {
                val address = parser.getAttributeValue(null, "address") ?: ""
                val body = parser.getAttributeValue(null, "body") ?: ""
                val date = parser.getAttributeValue(null, "date")?.toLongOrNull() ?: 0L
                val dateSent = parser.getAttributeValue(null, "date_sent")?.toLongOrNull()
                val type = parser.getAttributeValue(null, "type")?.toIntOrNull() ?: Telephony.Sms.MESSAGE_TYPE_INBOX
                val read = parser.getAttributeValue(null, "read")?.toIntOrNull() ?: 1
                val status = parser.getAttributeValue(null, "status")?.toIntOrNull() ?: Telephony.Sms.STATUS_NONE

                val values = ContentValues().apply {
                    put(Telephony.Sms.ADDRESS, address)
                    put(Telephony.Sms.BODY, body)
                    put(Telephony.Sms.DATE, date)
                    if (dateSent != null) put(Telephony.Sms.DATE_SENT, dateSent)
                    put(Telephony.Sms.TYPE, type)
                    put(Telephony.Sms.READ, read)
                    put(Telephony.Sms.STATUS, status)
                    put(Telephony.Sms.PROTOCOL, 0)
                    put(Telephony.Sms.SEEN, 1)
                }

                try {
                    contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
                    count++
                } catch (_: Exception) {
                }
            }
            eventType = parser.next()
        }
        return count
    }
}
