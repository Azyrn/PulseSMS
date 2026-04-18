package com.skeler.pulse.sms

import android.app.Service
import android.content.ContentValues
import android.content.Intent
import android.os.IBinder
import android.provider.Telephony
import android.telephony.SmsManager

/**
 * Service that handles `android.intent.action.RESPOND_VIA_MESSAGE`.
 *
 * This is invoked when the user chooses to respond to an incoming call
 * with a quick text message from the call screen.
 *
 * Required by Android to make the app eligible as the default SMS handler.
 */
class RespondViaMessageService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "android.intent.action.RESPOND_VIA_MESSAGE") {
            val recipient = intent.data?.schemeSpecificPart?.replace("-", "")?.trim()
            val body = intent.getStringExtra(Intent.EXTRA_TEXT)

            if (!recipient.isNullOrBlank() && !body.isNullOrBlank()) {
                sendSmsAndLog(recipient, body)
            }
        }
        stopSelf(startId)
        return START_NOT_STICKY
    }

    /**
     * Sends the SMS via [SmsManager] and writes it to the system SMS Provider
     * so it appears in the sent messages.
     */
    @Suppress("DEPRECATION")
    private fun sendSmsAndLog(recipient: String, body: String) {
        try {
            val smsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(body)
            smsManager.sendMultipartTextMessage(recipient, null, parts, null, null)

            // Write to sent folder
            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, recipient)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, System.currentTimeMillis())
                put(Telephony.Sms.READ, 1)
                put(Telephony.Sms.SEEN, 1)
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
            }
            contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
        } catch (_: Exception) {
            // SMS send or provider write failed
        }
    }
}
