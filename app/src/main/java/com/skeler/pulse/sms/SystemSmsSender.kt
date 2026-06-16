package com.skeler.pulse.sms

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.klinker.android.send_message.Transaction
import com.google.android.mms.MMSPart
import com.skeler.pulse.R
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URL
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class SystemSmsSender(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher,
) {
    private val contentResolver: ContentResolver get() = context.contentResolver

    @Suppress("DEPRECATION")
    suspend fun sendSms(address: String, body: String, subscriptionId: Int? = null, waitForDelivery: Boolean = true) = withContext(ioDispatcher) {
        val smsManager = if (subscriptionId != null) {
            SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
        } else {
            SmsManager.getDefault()
        }
        val parts = smsManager.divideMessage(body)
        val messageUri = insertOutgoingMessage(address, body, Telephony.Sms.MESSAGE_TYPE_QUEUED)
        val token = deliveryCallbackToken(address, messageUri)
        try {
            val deliveryIntents = buildDeliveryIntents(parts, messageUri, token)
            awaitSentCallbacks(parts, messageUri, token) { sentIntents ->
                smsManager.sendMultipartTextMessage(address, null, parts, sentIntents, deliveryIntents)
            }
            updateOutgoingMessage(messageUri, Telephony.Sms.MESSAGE_TYPE_SENT)
            if (waitForDelivery) {
                awaitDeliveryCallbacks(parts, messageUri, token)
            }
        } catch (exception: Exception) {
            updateOutgoingMessage(messageUri, Telephony.Sms.MESSAGE_TYPE_FAILED)
            throw exception
        }
    }

    private fun insertOutgoingMessage(address: String, body: String, messageType: Int): Uri? {
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, address)
            put(Telephony.Sms.BODY, body)
            put(Telephony.Sms.DATE, System.currentTimeMillis())
            put(Telephony.Sms.READ, 1)
            put(Telephony.Sms.SEEN, 1)
            put(Telephony.Sms.TYPE, messageType)
            put(Telephony.Sms.STATUS, Telephony.Sms.STATUS_PENDING)
            put(Telephony.Sms.THREAD_ID, Telephony.Threads.getOrCreateThreadId(context, address))
        }
        return contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
    }

    private fun updateOutgoingMessage(messageUri: Uri?, messageType: Int) {
        if (messageUri == null) return
        val values = ContentValues().apply {
            put(Telephony.Sms.TYPE, messageType)
            when (messageType) {
                Telephony.Sms.MESSAGE_TYPE_SENT -> put(Telephony.Sms.STATUS, Telephony.Sms.STATUS_COMPLETE)
                Telephony.Sms.MESSAGE_TYPE_FAILED -> put(Telephony.Sms.STATUS, Telephony.Sms.STATUS_FAILED)
            }
        }
        contentResolver.update(messageUri, values, null, null)
    }

    private fun updateOutgoingStatus(messageUri: Uri?, status: Int) {
        if (messageUri == null) return
        val values = ContentValues().apply {
            put(Telephony.Sms.STATUS, status)
        }
        contentResolver.update(messageUri, values, null, null)
    }

    private suspend fun awaitSentCallbacks(
        parts: ArrayList<String>,
        messageUri: Uri?,
        token: String,
        send: (ArrayList<PendingIntent>) -> Unit,
    ) = withTimeout(SEND_CALLBACK_TIMEOUT_MILLIS) {
        suspendCancellableCoroutine { continuation ->
            val action = "$ACTION_SMS_SENT.$token"
            val remainingParts = AtomicInteger(parts.size)
            val completed = AtomicBoolean(false)
            val failures = Collections.synchronizedList(mutableListOf<Int>())
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action != action) return
                    if (resultCode == Activity.RESULT_OK) {
                        if (completed.compareAndSet(false, true)) {
                            context.unregisterReceiver(this)
                            continuation.resume(Unit)
                        }
                        return
                    }
                    failures.add(resultCode)
                    if (remainingParts.decrementAndGet() == 0 && completed.compareAndSet(false, true)) {
                        context.unregisterReceiver(this)
                        continuation.resumeWithException(
                            SmsSendException("SMS send failed with result ${failures.first()}")
                        )
                    }
                }
            }
            ContextCompat.registerReceiver(
                context,
                receiver,
                IntentFilter(action),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            continuation.invokeOnCancellation {
                if (completed.compareAndSet(false, true)) {
                    runCatching { context.unregisterReceiver(receiver) }
                }
            }
            val sentIntents = buildCallbackIntents(
                action = action,
                token = token,
                parts = parts,
                messageUri = messageUri,
            )
            try {
                send(sentIntents)
            } catch (exception: Exception) {
                if (completed.compareAndSet(false, true)) {
                    runCatching { context.unregisterReceiver(receiver) }
                    continuation.resumeWithException(exception)
                }
            }
        }
    }

    private fun buildDeliveryIntents(
        parts: ArrayList<String>,
        messageUri: Uri?,
        token: String,
    ): ArrayList<PendingIntent> {
        val action = "$ACTION_SMS_DELIVERED.$token"
        return buildCallbackIntents(
            action = action,
            token = token,
            parts = parts,
            messageUri = messageUri,
        )
    }

    private suspend fun awaitDeliveryCallbacks(
        parts: ArrayList<String>,
        messageUri: Uri?,
        token: String,
    ) = runCatching {
        withTimeout(DELIVERY_CALLBACK_TIMEOUT_MILLIS) {
            suspendCancellableCoroutine { continuation ->
                val action = "$ACTION_SMS_DELIVERED.$token"
                val remainingParts = AtomicInteger(parts.size)
                val completed = AtomicBoolean(false)
                val failures = Collections.synchronizedList(mutableListOf<Int>())
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        if (intent.action != action) return
                        if (resultCode != Activity.RESULT_OK) {
                            failures.add(resultCode)
                        }
                        if (remainingParts.decrementAndGet() == 0 && completed.compareAndSet(false, true)) {
                            context.unregisterReceiver(this)
                            if (failures.isEmpty()) {
                                updateOutgoingStatus(messageUri, Telephony.Sms.STATUS_COMPLETE)
                            } else {
                                updateOutgoingStatus(messageUri, Telephony.Sms.STATUS_FAILED)
                            }
                            continuation.resume(Unit)
                        }
                    }
                }
                ContextCompat.registerReceiver(
                    context,
                    receiver,
                    IntentFilter(action),
                    ContextCompat.RECEIVER_NOT_EXPORTED,
                )
                continuation.invokeOnCancellation {
                    if (completed.compareAndSet(false, true)) {
                        runCatching { context.unregisterReceiver(receiver) }
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    suspend fun sendSmsFireAndForget(address: String, body: String) = withContext(ioDispatcher) {
        val smsManager = SmsManager.getDefault()
        val messageUri = insertOutgoingMessage(address, body, Telephony.Sms.MESSAGE_TYPE_SENT)
        if (messageUri != null) {
            val values = ContentValues().apply {
                put(Telephony.Sms.STATUS, Telephony.Sms.STATUS_COMPLETE)
            }
            contentResolver.update(messageUri, values, null, null)
        }
        try {
            val parts = smsManager.divideMessage(body)
            smsManager.sendMultipartTextMessage(address, null, parts, null, null)
        } catch (e: Exception) {
            if (messageUri != null) {
                updateOutgoingMessage(messageUri, Telephony.Sms.MESSAGE_TYPE_FAILED)
            }
            throw e
        }
    }

    suspend fun sendMms(address: String, text: String, imageUris: List<Uri> = emptyList()) = withContext(ioDispatcher) {
        try {
            val maxSizeKb = MmsPreferences(context).getMaxImageSizeKb()
            sendMmsInternal(address, text, imageUris, maxSizeKb)
        } catch (e: Exception) {
            Log.e("SystemSmsSender", "sendMms failed", e)
            throw e
        }
    }

    suspend fun sendVoiceMms(address: String, text: String, audioUri: Uri) = withContext(ioDispatcher) {
        try {
            val audioBytes = context.contentResolver.openInputStream(audioUri)?.use { it.readBytes() }
                ?: throw RuntimeException("Cannot read audio file")
            if (audioBytes.isEmpty()) throw RuntimeException("Empty audio recording")
            sendVoiceMmsInternal(address, text, audioBytes)
        } catch (e: Exception) {
            Log.e("SystemSmsSender", "sendVoiceMms failed", e)
            throw e
        }
    }

    private suspend fun sendVoiceMmsInternal(address: String, text: String, audioBytes: ByteArray) {
        val threadId = Telephony.Threads.getOrCreateThreadId(context, address)
        val now = System.currentTimeMillis()

        val parts = mutableListOf<MMSPart>()
        if (text.isNotBlank()) {
            parts.add(MMSPart().apply {
                MimeType = "text/plain"
                Name = "text.txt"
                Data = text.toByteArray()
            })
        }
        parts.add(MMSPart().apply {
            MimeType = "audio/amr"
            Name = "voice_${now}.amr"
            Data = audioBytes
        })

        val myNumber = MyPhoneNumberProvider.detect(context)
            ?: throw RuntimeException("Cannot detect own phone number. MMS requires a valid SIM.")
        val messageInfo = Transaction.getBytes(
            context,
            false,
            myNumber,
            arrayOf(address),
            parts.toTypedArray(),
            text.take(40).ifBlank { null },
        )
        val pduBytes = messageInfo.bytes ?: throw RuntimeException("PDU generation failed — audio may be too large")

        val messageUri = insertVoiceMmsRecord(threadId, address, text, audioBytes.size, pduBytes.size, now, myNumber, audioBytes)

        suspendCancellableCoroutine<Unit> { cont ->
            com.klinker.android.send_message.ApnUtils.initDefaultApns(context) { cont.resume(Unit) }
        }

        runCatching {
            val sp = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
            val spMmsc = sp.getString("mmsc_url", "")
            val spProxy = sp.getString("mms_proxy", "")
            val spPort = sp.getString("mms_port", "")
            if (!spMmsc.isNullOrBlank() || !spProxy.isNullOrBlank()) {
                MmsPreferences(context).setMmsProxy(spProxy, spPort, spMmsc)
            }
        }

        val mmsPrefs = MmsPreferences(context)
        var mmsc = mmsPrefs.getMmscUrl()
        var mmsProxy = mmsPrefs.getMmsProxy()
        var mmsPort = mmsPrefs.getMmsPort()

        if (mmsc.isNullOrBlank()) {
            Log.w("SystemSmsSender", "ApnUtils gave no MMSC, querying system APN provider")
            val subId = try { android.telephony.SubscriptionManager.getDefaultSubscriptionId() } catch (_: Exception) { -1 }
            if (subId >= 0) {
                val apnUri = Telephony.Carriers.CONTENT_URI.buildUpon()
                    .appendPath("subId").appendPath(subId.toString()).build()
                val cursor = try {
                    contentResolver.query(apnUri, null, "type LIKE '%mms%'", null, null)
                } catch (_: Exception) { null }
                cursor?.use { c ->
                    while (c.moveToNext()) {
                        val url = c.getString(c.getColumnIndexOrThrow("mmsc"))
                        if (!url.isNullOrBlank()) {
                            mmsc = url
                            mmsProxy = c.getString(c.getColumnIndexOrThrow("mmsproxy"))
                            mmsPort = c.getString(c.getColumnIndexOrThrow("mmsport"))
                            runCatching { mmsPrefs.setMmsProxy(mmsProxy, mmsPort, mmsc) }
                            break
                        }
                    }
                }
            }
        }

        if (mmsc.isNullOrBlank()) {
            Log.e("SystemSmsSender", "No MMSC found, cannot send MMS")
            throw RuntimeException("No MMSC configured")
        }

        sendPduToMmsc(pduBytes, mmsc, if (mmsProxy.isNullOrBlank()) null else mmsProxy, mmsPort?.toIntOrNull() ?: 80)

        if (messageUri != null) {
            contentResolver.update(messageUri, ContentValues().apply {
                put("st", 128)
            }, null, null)
        }
        context.contentResolver.notifyChange(Telephony.Mms.CONTENT_URI, null)
    }

    private suspend fun sendMmsInternal(address: String, text: String, imageUris: List<Uri>, maxImageSizeKb: Int) {
        val threadId = Telephony.Threads.getOrCreateThreadId(context, address)
        val maxSizeBytes = if (maxImageSizeKb <= 0) -1 else maxImageSizeKb * 1024
        val imageBytesList = imageUris.mapNotNull { uri ->
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@mapNotNull null
            compressImageToMaxSize(bytes, maxSizeBytes)
        }
        if (imageBytesList.isEmpty() && text.isBlank()) throw RuntimeException("No content to send")
        val now = System.currentTimeMillis()

        val parts = mutableListOf<MMSPart>()
        if (text.isNotBlank()) {
            parts.add(MMSPart().apply {
                MimeType = "text/plain"
                Name = "text.txt"
                Data = text.toByteArray()
            })
        }
        imageBytesList.forEach { bytes ->
            parts.add(MMSPart().apply {
                MimeType = "image/jpeg"
                Name = "image_${System.currentTimeMillis()}.jpg"
                Data = bytes
            })
        }

        val myNumber = MyPhoneNumberProvider.detect(context) ?: throw RuntimeException("Cannot detect own phone number. MMS requires a valid SIM.")
        val messageInfo = Transaction.getBytes(
            context,
            false,
            myNumber,
            arrayOf(address),
            parts.toTypedArray(),
            text.take(40).ifBlank { null },
        )
        val pduBytes = messageInfo.bytes ?: throw RuntimeException("PDU generation failed — image may be too large")

        // Insert our own MMS record with correct thread_id and addresses
        val messageUri = insertMmsRecord(threadId, address, text, imageBytesList, pduBytes.size, now, myNumber)

        // Load APN settings from klinker's bundled carrier database
        suspendCancellableCoroutine<Unit> { cont ->
            com.klinker.android.send_message.ApnUtils.initDefaultApns(context) { cont.resume(Unit) }
        }

        // Copy klinker's SharedPreferences values to DataStore for consistency
        runCatching {
            val sp = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
            val spMmsc = sp.getString("mmsc_url", "")
            val spProxy = sp.getString("mms_proxy", "")
            val spPort = sp.getString("mms_port", "")
            if (!spMmsc.isNullOrBlank() || !spProxy.isNullOrBlank()) {
                MmsPreferences(context).setMmsProxy(spProxy, spPort, spMmsc)
            }
        }

        val mmsPrefs = MmsPreferences(context)
        var mmsc = mmsPrefs.getMmscUrl()
        var mmsProxy = mmsPrefs.getMmsProxy()
        var mmsPort = mmsPrefs.getMmsPort()

        if (mmsc.isNullOrBlank()) {
            Log.w("SystemSmsSender", "ApnUtils gave no MMSC, querying system APN provider")
            val subId = try { android.telephony.SubscriptionManager.getDefaultSubscriptionId() } catch (_: Exception) { -1 }
            if (subId >= 0) {
                val apnUri = Telephony.Carriers.CONTENT_URI.buildUpon()
                    .appendPath("subId").appendPath(subId.toString()).build()
                val cursor = try {
                    contentResolver.query(apnUri, null, "type LIKE '%mms%'", null, null)
                } catch (_: Exception) { null }
                cursor?.use { c ->
                    while (c.moveToNext()) {
                        val url = c.getString(c.getColumnIndexOrThrow("mmsc"))
                        if (!url.isNullOrBlank()) {
                            mmsc = url
                            mmsProxy = c.getString(c.getColumnIndexOrThrow("mmsproxy"))
                            mmsPort = c.getString(c.getColumnIndexOrThrow("mmsport"))
                            // Save system APN values to DataStore for future use
                            runCatching { mmsPrefs.setMmsProxy(mmsProxy, mmsPort, mmsc) }
                            break
                        }
                    }
                }
            }
        }

        if (mmsc.isNullOrBlank()) {
            Log.e("SystemSmsSender", "No MMSC found, cannot send MMS")
            throw RuntimeException("No MMSC configured")
        }
        Log.i("SystemSmsSender", "MMSC=$mmsc proxy=$mmsProxy port=$mmsPort")

        sendPduToMmsc(pduBytes, mmsc, if (mmsProxy.isNullOrBlank()) null else mmsProxy, mmsPort?.toIntOrNull() ?: 80)

        if (messageUri != null) {
            contentResolver.update(messageUri, ContentValues().apply {
                put("st", 128) // STATUS_COMPLETE
            }, null, null)
        }
        context.contentResolver.notifyChange(Telephony.Mms.CONTENT_URI, null)
    }

    private fun sendPduToMmsc(pduBytes: ByteArray, mmsc: String, proxy: String?, port: Int) {
        val url = URL(mmsc)
        val connection = if (proxy != null) {
            url.openConnection(Proxy(Proxy.Type.HTTP, java.net.InetSocketAddress(proxy, port)))
        } else {
            url.openConnection()
        } as HttpURLConnection

        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/vnd.wap.mms-message")
        connection.doOutput = true
        connection.connectTimeout = 30_000
        connection.readTimeout = 60_000
        connection.outputStream.use { it.write(pduBytes) }
        val code = connection.responseCode
        Log.i("SystemSmsSender", "MMS HTTP response code=$code")
        if (code !in 200..299) {
            throw RuntimeException("MMS server returned $code")
        }
        connection.disconnect()
    }

    private fun insertMmsRecord(
        threadId: Long, address: String, text: String, imageBytesList: List<ByteArray>, pduSize: Int, now: Long, myNumber: String,
    ): Uri? {
        val mmsValues = ContentValues().apply {
            put("thread_id", threadId)
            put("date", now / 1000L)
            put("msg_box", Telephony.Mms.MESSAGE_BOX_OUTBOX)
            put("read", 1)
            put("sub", text.take(40).ifBlank { null })
            put("sub_cs", 106)
            put("ct_t", "application/vnd.wap.multipart.related")
            put("exp", pduSize)
            put("m_cls", "personal")
            put("m_type", 128)
            put("v", 18)
            put("pri", 129)
            put("tr_id", "T${now.toString(16)}")
            put("resp_st", 128)
        }
        val mmsUri = contentResolver.insert(Telephony.Mms.CONTENT_URI, mmsValues) ?: return null
        val mmsId = mmsUri.lastPathSegment ?: return null

        if (text.isNotBlank()) {
            contentResolver.insert(Uri.parse("content://mms/$mmsId/part"), ContentValues().apply {
                put("mid", mmsId)
                put("ct", "text/plain")
                put("text", text)
            })
        }
        imageBytesList.forEach { bytes ->
            val partUri = Uri.parse("content://mms/$mmsId/part")
            val insertedPart = contentResolver.insert(partUri, ContentValues().apply {
                put("mid", mmsId)
                put("ct", "image/jpeg")
                put("cid", "<${System.currentTimeMillis()}>")
                put("fn", "image_${System.currentTimeMillis()}.jpg")
            })
            if (insertedPart != null) {
                contentResolver.openOutputStream(insertedPart)?.use { it.write(bytes) }
            }
        }
        contentResolver.insert(Uri.parse("content://mms/$mmsId/addr"), ContentValues().apply {
            put("address", myNumber)
            put("charset", 106)
            put("type", 137)
        })
        contentResolver.insert(Uri.parse("content://mms/$mmsId/addr"), ContentValues().apply {
            put("address", address)
            put("charset", 106)
            put("type", 151)
        })
        return mmsUri
    }

    private fun deliveryCallbackToken(address: String, messageUri: Uri?): String =
        "${java.util.UUID.randomUUID()}_${messageUri?.lastPathSegment.orEmpty()}_${address.hashCode()}"

    private fun insertVoiceMmsRecord(
        threadId: Long, address: String, text: String, audioSize: Int, pduSize: Int, now: Long, myNumber: String, audioBytes: ByteArray? = null,
    ): Uri? {
        val mmsValues = ContentValues().apply {
            put("thread_id", threadId)
            put("date", now / 1000L)
            put("msg_box", Telephony.Mms.MESSAGE_BOX_OUTBOX)
            put("read", 1)
            put("sub", text.take(40).ifBlank { null })
            put("sub_cs", 106)
            put("ct_t", "application/vnd.wap.multipart.related")
            put("exp", pduSize)
            put("m_cls", "personal")
            put("m_type", 128)
            put("v", 18)
            put("pri", 129)
            put("tr_id", "T${now.toString(16)}")
            put("resp_st", 128)
        }
        val mmsUri = contentResolver.insert(Telephony.Mms.CONTENT_URI, mmsValues) ?: return null
        val mmsId = mmsUri.lastPathSegment ?: return null

        if (text.isNotBlank()) {
            contentResolver.insert(Uri.parse("content://mms/$mmsId/part"), ContentValues().apply {
                put("mid", mmsId)
                put("ct", "text/plain")
                put("text", text)
            })
        }
        if (audioBytes != null) {
            val partUri = Uri.parse("content://mms/$mmsId/part")
            val insertedPart = contentResolver.insert(partUri, ContentValues().apply {
                put("mid", mmsId)
                put("ct", "audio/amr")
                put("cid", "<${System.currentTimeMillis()}>")
                put("fn", "voice_${System.currentTimeMillis()}.amr")
            })
            if (insertedPart != null) {
                contentResolver.openOutputStream(insertedPart)?.use { it.write(audioBytes) }
            }
        }
        contentResolver.insert(Uri.parse("content://mms/$mmsId/addr"), ContentValues().apply {
            put("address", myNumber)
            put("charset", 106)
            put("type", 137)
        })
        contentResolver.insert(Uri.parse("content://mms/$mmsId/addr"), ContentValues().apply {
            put("address", address)
            put("charset", 106)
            put("type", 151)
        })
        return mmsUri
    }

    private fun compressImageToMaxSize(bytes: ByteArray, maxSizeBytes: Int): ByteArray {
        if (maxSizeBytes <= 0 || bytes.size <= maxSizeBytes) return bytes

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return bytes

        var sampleSize = maxOf(bounds.outWidth / 2048, bounds.outHeight / 2048, 1)
        Log.d("SystemSmsSender", "compress: ${bounds.outWidth}x${bounds.outHeight}, ${bytes.size}B target=${maxSizeBytes}B sample=$sampleSize")

        while (sampleSize <= 8) {
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
                BitmapFactory.Options().apply { inSampleSize = sampleSize }
            ) ?: return bytes

            val out = java.io.ByteArrayOutputStream()
            var quality = 85
            while (quality >= 10) {
                out.reset()
                if (bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out) && out.size() <= maxSizeBytes) {
                    bitmap.recycle()
                    Log.d("SystemSmsSender", "compressed: ${bytes.size} -> ${out.size()}B (sample=$sampleSize q=$quality)")
                    return out.toByteArray()
                }
                quality -= 15
            }
            bitmap.recycle()
            sampleSize *= 2
        }

        Log.w("SystemSmsSender", "compress: could not fit in $maxSizeBytes B")
        throw RuntimeException(context.getString(R.string.mms_image_too_large, maxSizeBytes / 1024))
    }

    private fun buildCallbackIntents(
        action: String,
        token: String,
        parts: ArrayList<String>,
        messageUri: Uri?,
    ): ArrayList<PendingIntent> {
        val intents = ArrayList<PendingIntent>(parts.size)
        repeat(parts.size) { index ->
            val intent = Intent(action)
                .setPackage(context.packageName)
                .putExtra(EXTRA_MESSAGE_URI, messageUri?.toString())
                .putExtra(EXTRA_PART_INDEX, index)
                .putExtra(EXTRA_PART_COUNT, parts.size)
            intents.add(
                PendingIntent.getBroadcast(
                    context,
                    callbackRequestCode(token, index),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            )
        }
        return intents
    }

    private companion object {
        private const val ACTION_SMS_SENT = "com.skeler.pulse.sms.SMS_SENT"
        private const val ACTION_SMS_DELIVERED = "com.skeler.pulse.sms.SMS_DELIVERED"
        private const val EXTRA_MESSAGE_URI = "message_uri"
        private const val EXTRA_PART_INDEX = "part_index"
        private const val EXTRA_PART_COUNT = "part_count"
        private const val SEND_CALLBACK_TIMEOUT_MILLIS = 60_000L
        private const val DELIVERY_CALLBACK_TIMEOUT_MILLIS = 180_000L
    }
}
