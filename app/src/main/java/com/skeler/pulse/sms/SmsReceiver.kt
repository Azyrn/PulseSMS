package com.skeler.pulse.sms

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony
import android.telephony.SmsMessage

/**
 * BroadcastReceiver for incoming SMS messages.
 *
 * Registered with `android.provider.Telephony.SMS_DELIVER` intent filter.
 * This broadcast is only delivered when Pulse is the default SMS app.
 *
 * Responsibilities:
 * 1. Extract SMS PDUs from the intent
 * 2. Write the message to the system SMS Provider (`content://sms/inbox`)
 * 3. Show a notification to the user
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // Group message parts by sender (multi-part SMS)
        val grouped = messages.groupBy { it.originatingAddress ?: "Unknown" }

        for ((sender, parts) in grouped) {
            val body = parts.joinToString("") { it.messageBody ?: "" }
            if (body.isBlank()) continue

            val persistedUri = writeSmsToProvider(context, sender, body, parts.first())
            if (persistedUri != null) {
                SmsNotificationHelper.notifyIncomingSms(context, sender, body)
            } else {
                SmsNotificationHelper.notifyIncomingSms(
                    context = context,
                    sender = sender,
                    body = "New message received, but Pulse couldn't save it yet.",
                )
            }
        }
    }

    /**
     * Writes the received SMS to the system SMS Provider so it appears
     * in the standard SMS database and is accessible to other apps.
     */
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
            context.contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
        } catch (_: Exception) {
            // Provider write may fail if permissions aren't fully granted yet
            null
        }
}
