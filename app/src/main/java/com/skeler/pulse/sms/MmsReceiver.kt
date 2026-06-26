package com.skeler.pulse.sms

import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import android.util.Log
import com.google.android.mms.pdu_alt.DeliveryInd
import com.google.android.mms.pdu_alt.NotificationInd
import com.google.android.mms.pdu_alt.PduBody
import com.google.android.mms.pdu_alt.PduHeaders
import com.google.android.mms.pdu_alt.PduParser
import com.google.android.mms.pdu_alt.RetrieveConf
import com.skeler.pulse.R
import com.skeler.pulse.contact.normalizeAddressForDisplay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL

class MmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION) return

        val pendingResult = goAsync()
        val pduData = intent.getByteArrayExtra("data")
        if (pduData == null) {
            Log.e(TAG, "No PDU data in intent")
            pendingResult.finish()
            return
        }
        Log.i(TAG, "WAP_PUSH received, PDU size: ${pduData.size} bytes")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                handleWapPush(context, pduData)
            } catch (e: Exception) {
                Log.e(TAG, "MMS handling failed", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleWapPush(context: Context, pduData: ByteArray) {
        val pdu = PduParser(pduData).parse()
        when (pdu) {
            is NotificationInd -> handleNotification(context, pdu)
            is DeliveryInd -> handleDeliveryInd(context, pdu)
            else -> Log.w(TAG, "Unexpected PDU type: ${pdu?.javaClass?.simpleName}")
        }
    }

    private suspend fun handleNotification(context: Context, notification: NotificationInd) {
        val locationUrl = String(notification.contentLocation ?: ByteArray(0))
        val transactionId = String(notification.transactionId ?: ByteArray(0))
        val from = notification.from?.string.orEmpty()

        Log.i(TAG, "MMS Notification: location=$locationUrl transactionId=$transactionId from=$from")

        if (locationUrl.isBlank()) {
            Log.e(TAG, "No content location")
            return
        }

        val mmsData = downloadFromLocation(context, locationUrl)
        if (mmsData == null) {
            Log.e(TAG, "Download failed")
            return
        }
        Log.i(TAG, "MMS downloaded: ${mmsData.size} bytes")

        val retrieveConf = PduParser(mmsData).parse() as? RetrieveConf
        if (retrieveConf == null) {
            Log.e(TAG, "Not a valid RetrieveConf")
            return
        }

        storeMms(context, retrieveConf, from)
    }

    private suspend fun handleDeliveryInd(context: Context, deliveryInd: DeliveryInd) {
        val messageId = deliveryInd.messageId?.let { String(it) } ?: return
        val status = deliveryInd.status
        Log.i(TAG, "Delivery report: m_id=$messageId status=$status")

        val cursor = context.contentResolver.query(
            Telephony.Mms.CONTENT_URI,
            arrayOf("_id"),
            "m_id = ?",
            arrayOf(messageId),
            null,
        )

        cursor?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val mmsUri = ContentUris.withAppendedId(Telephony.Mms.CONTENT_URI, id)

                val respSt = when (status) {
                    PduHeaders.STATUS_RETRIEVED -> PduHeaders.RESPONSE_STATUS_OK
                    PduHeaders.STATUS_REJECTED -> PduHeaders.RESPONSE_STATUS_ERROR_PERMANENT_CONTENT_NOT_ACCEPTED
                    PduHeaders.STATUS_EXPIRED -> PduHeaders.RESPONSE_STATUS_ERROR_PERMANENT_MESSAGE_NOT_FOUND
                    PduHeaders.STATUS_DEFERRED -> PduHeaders.RESPONSE_STATUS_ERROR_TRANSIENT_FAILURE
                    else -> PduHeaders.RESPONSE_STATUS_ERROR_UNSPECIFIED
                }

                context.contentResolver.update(mmsUri, ContentValues().apply {
                    put("resp_st", respSt)
                }, null, null)

                Log.i(TAG, "Delivery report updated: mmsId=$id resp_st=$respSt")
            }
        }

        context.contentResolver.notifyChange(Telephony.Mms.CONTENT_URI, null)
    }

    private suspend fun downloadFromLocation(context: Context, locationUrl: String): ByteArray? {
        return try {
            val url = URL(locationUrl)
            val mmsPrefs = MmsPreferences(context)
            val dsHost = mmsPrefs.getMmsProxy()
            val dsPort = mmsPrefs.getMmsPort()
            var proxyHost: String
            var proxyPort: Int
            if (!dsHost.isNullOrBlank()) {
                proxyHost = dsHost
                proxyPort = dsPort?.toIntOrNull() ?: 80
            } else {
                // Fallback to SharedPreferences (klinker ApnUtils writes there)
                val sp = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
                proxyHost = sp.getString("mms_proxy", "") ?: ""
                proxyPort = sp.getString("mms_port", "80")?.toIntOrNull() ?: 80
                if (proxyHost.isBlank()) {
                    // Query system APN provider directly
                    val subId = try { android.telephony.SubscriptionManager.getDefaultSubscriptionId() } catch (_: Exception) { -1 }
                    if (subId >= 0) {
                        val apnUri = Telephony.Carriers.CONTENT_URI.buildUpon()
                            .appendPath("subId").appendPath(subId.toString()).build()
                        val cursor = try {
                            context.contentResolver.query(apnUri, null, "type LIKE '%mms%'", null, null)
                        } catch (_: Exception) { null }
                        cursor?.use { c ->
                            while (c.moveToNext()) {
                                val mmsProxy = c.getString(c.getColumnIndexOrThrow("mmsproxy")) ?: ""
                                val mmsPort = c.getString(c.getColumnIndexOrThrow("mmsport")) ?: ""
                                if (mmsProxy.isNotBlank()) {
                                    proxyHost = mmsProxy
                                    proxyPort = mmsPort.toIntOrNull() ?: 80
                                    runCatching {
                                        mmsPrefs.setMmsProxy(proxyHost, mmsPort, c.getString(c.getColumnIndexOrThrow("mmsc")))
                                    }
                                }
                                break
                            }
                        }
                    }
                }
            }

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

            try {
                val connection = if (proxyHost.isNotBlank()) {
                    url.openConnection(Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyHost, proxyPort)))
                } else {
                    url.openConnection()
                } as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 30_000
                connection.readTimeout = 30_000
                connection.doInput = true

                val code = connection.responseCode
                Log.i(TAG, "MMSC response: $code")

                if (code != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "MMSC error: $code")
                    connection.disconnect()
                    return null
                }

                connection.inputStream.use { it.readBytes() }.also { connection.disconnect() }
            } finally {
                if (Build.VERSION.SDK_INT >= 23) {
                    cm.bindProcessToNetwork(previousNetwork)
                } else {
                    @Suppress("DEPRECATION")
                    ConnectivityManager.setProcessDefaultNetwork(previousNetwork)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "HTTP download failed", e)
            null
        }
    }

    private suspend fun storeMms(context: Context, conf: RetrieveConf, fromFallback: String) {
        val fromRaw = conf.from?.string ?: fromFallback
        val fromDisplay = fromRaw
            .normalizeAddressForDisplay()
            .ifBlank { context.getString(R.string.mms_sender_label) }

        // Skip delivery notifications FROM our own number (they create confusing threads)
        if (MyPhoneNumberProvider.isMyNumber(context, fromRaw)) {
            Log.i(TAG, "Skipping MMS from self (delivery notification)")
            return
        }

        val threadId = try {
            Telephony.Threads.getOrCreateThreadId(context, fromRaw)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve thread_id", e)
            return
        }

        // 1. Insert MMS header manually
        val now = System.currentTimeMillis() / 1000L
        val mmsValues = ContentValues().apply {
            put("date", now)
            put("msg_box", Telephony.Mms.MESSAGE_BOX_INBOX)
            put("read", 0)
            put("seen", 0)
            put("m_type", conf.messageType)
            put("sub", conf.subject?.string.orEmpty())
            put("sub_cs", 106)
            put("text_only", if (conf.body?.partsNum == 0) 1 else 0)
            put("thread_id", threadId)
        }
        val mmsUri = context.contentResolver.insert(
            Uri.parse("content://mms/inbox"),
            mmsValues,
        )
        if (mmsUri == null) {
            Log.e(TAG, "Provider insert failed")
            return
        }
        val mmsId = mmsUri.lastPathSegment?.toLongOrNull() ?: 0L

        // 2. Insert from address
        val addrValues = ContentValues().apply {
            put("address", fromRaw)
            put("charset", 106)
            put("type", 137) // PduHeaders.FROM
        }
        context.contentResolver.insert(Uri.withAppendedPath(mmsUri, "addr"), addrValues)

        // 3. Persist parts — write binary parts to our cache dir so _data is set and Coil can read them
        val body = conf.body
        if (body != null && body.partsNum > 0) {
            persistParts(context, mmsUri, mmsId, body)
        }

        Log.i(TAG, "MMS persisted: $mmsUri id=$mmsId threadId=$threadId")

        val textBody = extractTextBody(conf).ifEmpty { context.getString(R.string.mms_body_placeholder) }

        val imageUri = MmsPartResolver.resolveFirstAttachmentUri(context, mmsId)

        // Only set conversation address if we have a real number (not the fallback label)
        val senderForNotification = fromDisplay.takeUnless { it == context.getString(R.string.mms_sender_label) }.orEmpty()

        val quickReplyEnabled = try {
            NotificationPreferences(context).isQuickReplyEnabled()
        } catch (e: Exception) {
            true
        }
        SmsNotificationHelper.notifyIncomingSms(
            context = context,
            sender = senderForNotification,
            body = textBody,
            messageId = mmsId,
            imageUri = imageUri,
            quickReplyEnabled = quickReplyEnabled,
            isMms = true,
        )
        Log.i(TAG, "MMS notified for id=$mmsId sender=$senderForNotification")

        // Force the UI to refresh (system provider may not auto-notify on MIUI)
        context.contentResolver.notifyChange(Telephony.Mms.CONTENT_URI, null)
    }

    private fun persistParts(context: Context, mmsUri: Uri, mmsId: Long, body: PduBody) {
        for (i in 0 until body.partsNum) {
            val part = body.getPart(i)
            val ct = String(part.contentType ?: ByteArray(0))
            val partData = part.data

            val partValues = ContentValues().apply {
                put("mid", mmsId)
                put("ct", ct)
                put("fn", part.filename?.let { String(it) }.orEmpty())
                put("name", part.name?.let { String(it) }.orEmpty())
                put("chset", part.charset)
                put("cl", part.contentLocation?.let { String(it) }.orEmpty())
                if (partData != null && (ct == "text/plain" || ct == "text/html")) {
                    put("text", String(partData))
                }
            }

            val partUri = context.contentResolver.insert(
                Uri.withAppendedPath(mmsUri, "part"),
                partValues,
            )
            if (partUri == null) {
                Log.e(TAG, "Failed to insert part $i for ct=$ct")
                continue
            }

            val partId = partUri.lastPathSegment?.toLongOrNull() ?: continue
            Log.i(TAG, "Inserted part $i: $partUri ct=$ct partId=$partId")

            // Write binary parts to our cache dir (accessible to our FileProvider)
            if (partData != null && ct != "text/plain" && ct != "text/html" && ct != "application/smil") {
                val file = java.io.File(context.cacheDir, "mms_parts/$partId")
                file.parentFile?.mkdirs()
                file.writeBytes(partData)
                Log.i(TAG, "Saved image to ${file.absolutePath} (${partData.size} bytes)")
            }
        }
    }

    private fun extractTextBody(conf: RetrieveConf): String {
        val body = conf.body ?: return ""
        for (i in 0 until body.partsNum) {
            val part = body.getPart(i)
            val ct = String(part.contentType ?: ByteArray(0))
            if (ct.startsWith("text/plain")) {
                return String(part.data ?: ByteArray(0))
            }
        }
        return ""
    }

    companion object {
        private const val TAG = "MmsReceiver"
    }
}
