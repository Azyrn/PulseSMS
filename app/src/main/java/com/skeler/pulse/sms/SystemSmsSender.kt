package com.skeler.pulse.sms

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.provider.Telephony
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
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
    suspend fun sendSms(address: String, body: String, subscriptionId: Int? = null) = withContext(ioDispatcher) {
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
            awaitDeliveryCallbacks(parts, messageUri, token)
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
        val status = if (messageType == Telephony.Sms.MESSAGE_TYPE_SENT) {
            Telephony.Sms.STATUS_COMPLETE
        } else {
            Telephony.Sms.STATUS_FAILED
        }
        val values = ContentValues().apply {
            put(Telephony.Sms.TYPE, messageType)
            put(Telephony.Sms.STATUS, status)
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
                    if (resultCode != Activity.RESULT_OK) {
                        failures.add(resultCode)
                    }
                    if (remainingParts.decrementAndGet() == 0 && completed.compareAndSet(false, true)) {
                        context.unregisterReceiver(this)
                        if (failures.isEmpty()) {
                            continuation.resume(Unit)
                        } else {
                            continuation.resumeWithException(
                                SmsSendException("SMS send failed with result ${failures.first()}")
                            )
                        }
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

    private fun deliveryCallbackToken(address: String, messageUri: Uri?): String =
        "${java.util.UUID.randomUUID()}_${messageUri?.lastPathSegment.orEmpty()}_${address.hashCode()}"

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
