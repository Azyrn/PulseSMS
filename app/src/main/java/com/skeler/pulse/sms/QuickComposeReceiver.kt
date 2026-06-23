package com.skeler.pulse.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QuickComposeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_DISMISSED -> handleDismissed(context)
        }
    }

    private fun handleDismissed(context: Context) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (NotificationPreferences(context).isQuickComposeEnabled()) {
                    QuickComposeNotificationManager.show(context)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to re-show quick compose notification after dismiss", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "QuickComposeReceiver"
        const val ACTION_DISMISSED = "com.skeler.pulse.action.QUICK_COMPOSE_DISMISSED"
    }
}
