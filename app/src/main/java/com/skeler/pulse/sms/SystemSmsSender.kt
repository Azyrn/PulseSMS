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
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.klinker.android.send_message.Transaction
import com.google.android.mms.MMSPart
import com.google.android.mms.pdu_alt.PduParser
import com.google.android.mms.pdu_alt.SendConf
import com.skeler.pulse.R
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URL
import java.nio.ByteBuffer
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
    suspend fun sendSms(address: String, body: String, subscriptionId: Int? = null, waitForDelivery: Boolean = true, encryptedBody: String? = null) = withContext(ioDispatcher) {
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
            if (encryptedBody != null) {
                encryptStoredMessage(messageUri, encryptedBody)
            }
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

    private fun encryptStoredMessage(messageUri: Uri?, encryptedBody: String) {
        if (messageUri == null) return
        val values = ContentValues().apply {
            put(Telephony.Sms.BODY, encryptedBody)
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
            val maxSizeKb = MmsPreferences(context).getMaxImageSizeKb()
            val audioBytes = context.contentResolver.openInputStream(audioUri)?.use { it.readBytes() }
                ?: throw RuntimeException("Cannot read audio file")
            if (audioBytes.isEmpty()) throw RuntimeException("Empty audio recording")
            val compressed = compressVoiceToMaxSize(audioBytes, maxSizeKb)
            sendVoiceMmsInternal(address, text, compressed)
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

        withTimeout(15_000L) {
            suspendCancellableCoroutine<Unit> { cont ->
                com.klinker.android.send_message.ApnUtils.initDefaultApns(context) { cont.resume(Unit) }
            }
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

        try {
            val body = sendPduToMmsc(pduBytes, mmsc, if (mmsProxy.isNullOrBlank()) null else mmsProxy, mmsPort?.toIntOrNull() ?: 80)
            if (messageUri != null) {
                val sendConf = if (body != null) PduParser(body).parse() as? SendConf else null
                val responseStatus = sendConf?.responseStatus ?: 128
                if (responseStatus != 128) {
                    throw RuntimeException("MMS rejected by MMSC: responseStatus=$responseStatus")
                }
                contentResolver.update(messageUri, ContentValues().apply {
                    put("msg_box", Telephony.Mms.MESSAGE_BOX_SENT)
                    put("st", 128)
                    sendConf?.messageId?.let { put("m_id", String(it)) }
                }, null, null)
            }
        } catch (e: Exception) {
            if (messageUri != null) {
                contentResolver.update(messageUri, ContentValues().apply {
                    put("msg_box", Telephony.Mms.MESSAGE_BOX_FAILED)
                    put("st", 129)
                }, null, null)
            }
            throw e
        } finally {
            context.contentResolver.notifyChange(Telephony.Mms.CONTENT_URI, null)
        }
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
        withTimeout(15_000L) {
            suspendCancellableCoroutine<Unit> { cont ->
                com.klinker.android.send_message.ApnUtils.initDefaultApns(context) { cont.resume(Unit) }
            }
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

        try {
            val body = sendPduToMmsc(pduBytes, mmsc, if (mmsProxy.isNullOrBlank()) null else mmsProxy, mmsPort?.toIntOrNull() ?: 80)
            if (messageUri != null) {
                val sendConf = if (body != null) PduParser(body).parse() as? SendConf else null
                val responseStatus = sendConf?.responseStatus ?: 128
                if (responseStatus != 128) {
                    throw RuntimeException("MMS rejected by MMSC: responseStatus=$responseStatus")
                }
                contentResolver.update(messageUri, ContentValues().apply {
                    put("msg_box", Telephony.Mms.MESSAGE_BOX_SENT)
                    put("st", 128)
                    sendConf?.messageId?.let { put("m_id", String(it)) }
                }, null, null)
            }
        } catch (e: Exception) {
            if (messageUri != null) {
                contentResolver.update(messageUri, ContentValues().apply {
                    put("msg_box", Telephony.Mms.MESSAGE_BOX_FAILED)
                    put("st", 129)
                }, null, null)
            }
            throw e
        } finally {
            context.contentResolver.notifyChange(Telephony.Mms.CONTENT_URI, null)
        }
    }

    private suspend fun sendPduToMmsc(pduBytes: ByteArray, mmsc: String, proxy: String?, port: Int): ByteArray? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val mmsNetwork = cm.awaitNetwork(
            NetworkCapabilities.TRANSPORT_CELLULAR,
            NetworkCapabilities.NET_CAPABILITY_MMS,
            timeoutMs = 20_000,
        ) ?: cm.awaitNetwork(
            NetworkCapabilities.TRANSPORT_CELLULAR,
            NetworkCapabilities.NET_CAPABILITY_INTERNET,
            timeoutMs = 10_000,
        ) ?: cm.awaitNetwork(
            NetworkCapabilities.TRANSPORT_WIFI,
            NetworkCapabilities.NET_CAPABILITY_INTERNET,
            timeoutMs = 5_000,
        )
        val previousNetwork = if (Build.VERSION.SDK_INT >= 23) {
            cm.getBoundNetworkForProcess()
        } else null

        if (mmsNetwork != null) {
            if (Build.VERSION.SDK_INT >= 23) {
                cm.bindProcessToNetwork(mmsNetwork)
            } else {
                @Suppress("DEPRECATION")
                ConnectivityManager.setProcessDefaultNetwork(mmsNetwork)
            }
        }

        return try {
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
            try {
                connection.inputStream?.use { it.readBytes() }
            } catch (_: Exception) {
                null
            }.also { connection.disconnect() }
        } finally {
            if (Build.VERSION.SDK_INT >= 23) {
                cm.bindProcessToNetwork(previousNetwork)
            } else {
                @Suppress("DEPRECATION")
                ConnectivityManager.setProcessDefaultNetwork(previousNetwork)
            }
        }
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

    private fun compressVoiceToMaxSize(audioBytes: ByteArray, maxSizeKb: Int): ByteArray {
        val maxSizeBytes = if (maxSizeKb <= 0) -1 else maxSizeKb * 1024
        if (maxSizeBytes <= 0 || audioBytes.size <= maxSizeBytes) return audioBytes

        val tempDir = java.io.File(context.cacheDir, "voice_compressed")
        tempDir.mkdirs()
        // Clean stale temp files from previous runs
        tempDir.listFiles()?.filter { it.extension == "amr" }?.forEach { it.delete() }

        val amrBitrates = listOf(7950, 5900, 4750)

        for (targetBitrate in amrBitrates) {
            val outFile = java.io.File(tempDir, "compressed_${targetBitrate}.amr")
            try {
                if (transcodeAmr(audioBytes, outFile.absolutePath, targetBitrate)) {
                    val result = outFile.readBytes()
                    if (result.size <= maxSizeBytes) {
                        outFile.delete()
                        Log.i("SystemSmsSender", "Voice compressed: ${audioBytes.size} -> ${result.size}B (bitrate=$targetBitrate)")
                        return result
                    }
                    outFile.delete()
                }
            } catch (e: Exception) {
                Log.w("SystemSmsSender", "AMR transcode failed at ${targetBitrate}bps", e)
            }
        }

        Log.w("SystemSmsSender", "Voice compression could not fit in $maxSizeBytes")
        throw RuntimeException("Compressed voice message exceeds ${maxSizeBytes / 1024} KB")
    }

    private fun transcodeAmr(inputBytes: ByteArray, outputPath: String, targetBitrate: Int): Boolean {
        val inputFile = java.io.File(context.cacheDir, "voice_transcode_input.amr")
        inputFile.writeBytes(inputBytes)

        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(inputFile.absolutePath)
        } catch (e: Exception) {
            return false
        }

        var audioTrackIndex = -1
        var sourceFormat: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val fmt = extractor.getTrackFormat(i)
            if (fmt.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                audioTrackIndex = i
                sourceFormat = fmt
                break
            }
        }
        if (audioTrackIndex == -1) { extractor.release(); return false }

        val mime = sourceFormat!!.getString(MediaFormat.KEY_MIME) ?: return false
        val sampleRate = sourceFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE, 8000)
        val channelCount = sourceFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT, 1)

        extractor.selectTrack(audioTrackIndex)
        val decoder = MediaCodec.createDecoderByType(mime)
        decoder.configure(sourceFormat, null, null, 0)
        decoder.start()

        val encoder = MediaCodec.createEncoderByType("audio/3gpp")
        val outputFormat = MediaFormat.createAudioFormat("audio/3gpp", sampleRate, channelCount).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, targetBitrate)
            setInteger(MediaFormat.KEY_AAC_PROFILE, 0) // no profile for AMR
        }
        encoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()

        val muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_3GPP)
        var muxerStarted = false
        var encoderTrackIndex = -1

        val bufferInfo = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        val timeoutUs = 5000L
        var sawInputEos = false

        val allDecodedFrames = mutableListOf<Pair<ByteBuffer, MediaCodec.BufferInfo>>()

        // Phase 1: decode all input
        while (!sawInputEos) {
            val inputIndex = decoder.dequeueInputBuffer(timeoutUs)
            if (inputIndex >= 0) {
                val buf = decoder.getInputBuffer(inputIndex) ?: continue
                val sampleSize = extractor.readSampleData(buf, 0)
                if (sampleSize < 0) {
                    decoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    sawInputEos = true
                } else {
                    decoder.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                    extractor.advance()
                }
            }

            var decodeDone = false
            while (!decodeDone) {
                val outputIndex = decoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
                when {
                    outputIndex >= 0 -> {
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            decodeDone = true
                        }
                        if (bufferInfo.size > 0) {
                            val outBuf = decoder.getOutputBuffer(outputIndex) ?: continue
                            val copy = ByteBuffer.allocate(bufferInfo.size)
                            outBuf.position(bufferInfo.offset)
                            outBuf.limit(bufferInfo.offset + bufferInfo.size)
                            copy.put(outBuf)
                            copy.flip()
                            allDecodedFrames.add(copy to MediaCodec.BufferInfo().apply {
                                size = copy.remaining()
                                offset = 0
                                presentationTimeUs = bufferInfo.presentationTimeUs
                                flags = bufferInfo.flags
                            })
                        }
                        decoder.releaseOutputBuffer(outputIndex, false)
                    }
                    outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> { /* continue */ }
                    outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        if (sawInputEos && bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            decodeDone = true
                        } else {
                            break
                        }
                    }
                }
            }
        }

        decoder.stop()
        decoder.release()
        extractor.release()

        if (allDecodedFrames.isEmpty()) return false

        // Phase 2: encode all decoded frames
        var frameIndex = 0
        val inputFlags = IntArray(allDecodedFrames.size + 1) { 0 }
        val inputTimes = LongArray(allDecodedFrames.size) { allDecodedFrames[it].second.presentationTimeUs }
        var encodedOutputDone = false

        while (!encodedOutputDone) {
            // Feed input
            if (!inputDone && frameIndex < allDecodedFrames.size) {
                val inputIdx = encoder.dequeueInputBuffer(timeoutUs)
                if (inputIdx >= 0) {
                    val (buf, info) = allDecodedFrames[frameIndex]
                    val encBuf = encoder.getInputBuffer(inputIdx) ?: continue
                    encBuf.clear()
                    encBuf.put(buf)
                    buf.rewind()
                    val flags = if (frameIndex == allDecodedFrames.size - 1) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                    encoder.queueInputBuffer(inputIdx, 0, info.size, info.presentationTimeUs, flags)
                    frameIndex++
                }
            } else {
                inputDone = true
            }

            if (inputDone && frameIndex >= allDecodedFrames.size) {
                val inputIdx = encoder.dequeueInputBuffer(timeoutUs)
                if (inputIdx >= 0) {
                    encoder.queueInputBuffer(inputIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                }
            }

            // Collect output
            val outputIdx = encoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
            when {
                outputIdx >= 0 -> {
                    val eos = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                    if (eos) encodedOutputDone = true

                    if (bufferInfo.size > 0) {
                        val encBuf = encoder.getOutputBuffer(outputIdx) ?: continue
                        if (muxerStarted) {
                            muxer.writeSampleData(encoderTrackIndex, encBuf, bufferInfo)
                            encoder.releaseOutputBuffer(outputIdx, false)
                        } else {
                            // Save data before releasing; buffer is invalid after releaseOutputBuffer
                            val savedBuf = ByteBuffer.allocate(bufferInfo.size)
                            encBuf.position(bufferInfo.offset)
                            savedBuf.put(encBuf)
                            savedBuf.flip()
                            encoder.releaseOutputBuffer(outputIdx, false)

                            val newFormat = encoder.outputFormat
                            encoderTrackIndex = muxer.addTrack(newFormat)
                            muxer.start()
                            muxerStarted = true
                            muxer.writeSampleData(encoderTrackIndex, savedBuf, bufferInfo)
                        }
                    } else {
                        encoder.releaseOutputBuffer(outputIdx, false)
                    }
                }
                outputIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    if (!muxerStarted) {
                        val newFormat = encoder.outputFormat
                        encoderTrackIndex = muxer.addTrack(newFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                }
                outputIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> { /* wait */ }
            }
        }

        encoder.stop()
        encoder.release()

        if (muxerStarted) {
            muxer.stop()
        }
        muxer.release()

        inputFile.delete()
        return muxerStarted
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
