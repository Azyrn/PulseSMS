package com.skeler.pulse.sms

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import com.skeler.pulse.R
import java.util.Collections
import java.util.LinkedHashSet

object SmsProcessingHelper {

    private const val TAG = "SmsProcessingHelper"
    private const val MAX_DEDUP_SIZE = 64

    private val processedRecently = Collections.synchronizedSet(
        LinkedHashSet<String>(MAX_DEDUP_SIZE),
    )

    /**
     * Processes incoming SMS PDUs — persists to provider and shows notification.
     * Deduplicates by sender+body+timestamp to handle cases where both
     * [SMS_DELIVER] and [SMS_RECEIVED] fire for the same message.
     */
    suspend fun processIncomingSms(context: Context, messages: Array<SmsMessage>) {
        val grouped = messages.groupBy { it.originatingAddress ?: "Unknown" }
        for ((sender, parts) in grouped) {
            val body = parts.joinToString("") { it.messageBody ?: "" }
            if (body.isBlank()) continue

            val timestampMillis = parts.first().timestampMillis
            val dedupKey = "$sender|$body|$timestampMillis"
            synchronized(processedRecently) {
                if (!processedRecently.add(dedupKey)) continue
                if (processedRecently.size > MAX_DEDUP_SIZE) {
                    processedRecently.remove(processedRecently.first())
                }
            }

            OtpClipboardAutoCopy.copyIncomingCodeIfEnabled(context, body)

            val quickReplyEnabled = try {
                NotificationPreferences(context).isQuickReplyEnabled()
            } catch (e: Exception) {
                true
            }
            val isMuted = InboxThreadPreferences(context).isAddressMuted(sender)
            val persistedUri = writeSmsToProvider(context, sender, body, parts.first())
            if (!isMuted) {
                if (persistedUri != null) {
                    val messageId = ContentUris.parseId(persistedUri)
                    SmsNotificationHelper.notifyIncomingSms(
                        context = context,
                        sender = sender,
                        body = body,
                        messageId = messageId,
                        quickReplyEnabled = quickReplyEnabled,
                    )
                } else {
                    SmsNotificationHelper.notifyIncomingSms(
                        context = context,
                        sender = sender,
                        body = context.getString(R.string.sms_body_save_failed),
                        quickReplyEnabled = quickReplyEnabled,
                    )
                }
            }
        }
    }

    private fun writeSmsToProvider(
        context: Context,
        sender: String,
        body: String,
        smsMessage: SmsMessage,
    ): Uri? =
        try {
            val receivedAt = System.currentTimeMillis()
            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, sender)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, receivedAt)
                put(Telephony.Sms.DATE_SENT, smsMessage.timestampMillis)
                put(Telephony.Sms.READ, 0)
                put(Telephony.Sms.SEEN, 0)
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
                put(Telephony.Sms.THREAD_ID, Telephony.Threads.getOrCreateThreadId(context, sender))
            }
            val uri = context.contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
            if (uri != null) {
                context.contentResolver.notifyChange(uri, null)
            }
            uri
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write SMS to provider", e)
            null
        }
}
