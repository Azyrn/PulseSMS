package com.skeler.pulse.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import java.util.Locale
import android.util.Log
import android.widget.Toast
import androidx.core.app.RemoteInput
import com.skeler.pulse.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuickComposeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_SET_CONTACT -> handleSetContact(context, intent)
            ACTION_SEND_MESSAGE -> handleSendMessage(context, intent)
            ACTION_DISMISSED -> handleDismissed(context)
        }
    }

    private fun handleSetContact(context: Context, intent: Intent) {
        val results = RemoteInput.getResultsFromIntent(intent)
        val raw = results?.getCharSequence(QuickComposeNotificationManager.KEY_CONTACT_INPUT)
            ?.toString()?.trim() ?: return
        if (raw.isBlank()) return

        val normalized = normalizePhoneNumber(context, raw)
        if (normalized == null) {
            Toast.makeText(context, R.string.quick_compose_invalid_contact_toast, Toast.LENGTH_SHORT).show()
            return
        }

        QuickComposeNotificationManager.setTargetNumber(context, normalized)
        QuickComposeNotificationManager.show(context)
    }

    private fun handleSendMessage(context: Context, intent: Intent) {
        val results = RemoteInput.getResultsFromIntent(intent)
        val message = results?.getCharSequence(QuickComposeNotificationManager.KEY_MESSAGE_INPUT)
            ?.toString()?.trim() ?: return
        if (message.isBlank()) {
            Log.w(TAG, "Empty message, ignoring")
            return
        }

        val address = QuickComposeNotificationManager.getTargetNumber(context)
        if (address.isNullOrBlank()) {
            Log.w(TAG, "No target address set, ignoring")
            Toast.makeText(context, R.string.quick_compose_no_contact_toast, Toast.LENGTH_SHORT).show()
            return
        }
        if (normalizePhoneNumber(context, address) == null) {
            Log.w(TAG, "Stored address is invalid, ignoring")
            Toast.makeText(context, R.string.quick_compose_invalid_contact_toast, Toast.LENGTH_SHORT).show()
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.i(TAG, "Sending quick compose SMS to $address")
                SystemSmsSender(context, Dispatchers.IO).sendSmsFireAndForget(address, message)
                Log.i(TAG, "Quick compose SMS sent successfully")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, R.string.quick_compose_sent_toast, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send quick compose message", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, R.string.quick_compose_failed_toast, Toast.LENGTH_SHORT).show()
                }
            } finally {
                QuickComposeNotificationManager.show(context)
                pendingResult.finish()
            }
        }
    }

    private fun normalizePhoneNumber(context: Context, raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        if (trimmed.startsWith("+")) {
            val digits = trimmed.count { it.isDigit() }
            if (digits < 8) return null
            return trimmed
        }

        val cleaned = trimmed.replace(Regex("[^\\d+]"), "")
        val digits = cleaned.count { it.isDigit() }
        if (digits < 8) return null

        val country = try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            tm?.simCountryIso?.uppercase()
                ?: tm?.networkCountryIso?.uppercase()
                ?: Locale.getDefault().country.uppercase()
        } catch (e: Exception) {
            Locale.getDefault().country.uppercase()
        }

        return try {
            PhoneNumberUtils.formatNumberToE164(cleaned, country) ?: trimmed
        } catch (e: Exception) {
            trimmed
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
        const val ACTION_SET_CONTACT = "com.skeler.pulse.action.QUICK_COMPOSE_SET_CONTACT"
        const val ACTION_SEND_MESSAGE = "com.skeler.pulse.action.QUICK_COMPOSE_SEND_MESSAGE"
        const val ACTION_DISMISSED = "com.skeler.pulse.action.QUICK_COMPOSE_DISMISSED"
    }
}
