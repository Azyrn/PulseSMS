package com.skeler.pulse.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

/**
 * BroadcastReceiver for incoming MMS messages (WAP Push).
 *
 * Registered with `android.provider.Telephony.WAP_PUSH_DELIVER` intent filter
 * and `application/vnd.wap.mms-message` MIME type.
 *
 * This broadcast is only delivered when Pulse is the default SMS app.
 *
 * Current implementation acknowledges receipt to prevent message loss.
 * Full MMS download and rendering is deferred to Phase 2.
 */
class MmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION) return

        // Acknowledge receipt — prevents the system from discarding the MMS.
        // The MMS PDU is in the intent extras under "data" and "pdu".
        // Full MMS parsing (downloading content, extracting images/text) will
        // be implemented in Phase 2.

        SmsNotificationHelper.notifyIncomingSms(
            context = context,
            sender = "MMS",
            body = "New multimedia message received",
        )
    }
}
