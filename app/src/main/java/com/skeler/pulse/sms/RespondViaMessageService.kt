package com.skeler.pulse.sms

import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Service that handles `android.intent.action.RESPOND_VIA_MESSAGE`.
 *
 * This is invoked when the user chooses to respond to an incoming call
 * with a quick text message from the call screen.
 *
 * Required by Android to make the app eligible as the default SMS handler.
 */
class RespondViaMessageService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "android.intent.action.RESPOND_VIA_MESSAGE") {
            val recipient = intent.data?.schemeSpecificPart
                ?.substringBefore("?")
                ?.replace("-", "")
                ?.trim()
            val body = intent.getStringExtra(Intent.EXTRA_TEXT)

            if (!recipient.isNullOrBlank() && !body.isNullOrBlank()) {
                serviceScope.launch {
                    try {
                        SystemSmsReader(applicationContext).sendSms(recipient, body)
                    } finally {
                        stopSelf(startId)
                    }
                }
                return START_NOT_STICKY
            }
        }
        stopSelf(startId)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
