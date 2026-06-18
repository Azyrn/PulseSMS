package com.skeler.pulse.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Build
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.skeler.pulse.MainActivity
import com.skeler.pulse.R

object QuickComposeNotificationManager {

    private const val NOTIFICATION_ID = 9001
    private const val REQUEST_CODE_CONTACT = 900101
    private const val REQUEST_CODE_SEND = 900102
    private const val REQUEST_CODE_OPEN = 900103
    private const val REQUEST_CODE_DISMISS = 900104
    const val CHANNEL_ID = "quick_compose_channel"
    private const val PREFS_NAME = "quick_compose"
    private const val KEY_TARGET_NUMBER = "target_number"
    private const val TAG = "QuickComposeNotifMgr"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.quick_compose_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.quick_compose_channel_description)
                setShowBadge(false)
            }
            context.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }

    fun show(context: Context) {
        // Auto-select the last contacted number for convenience on first show
        if (getTargetNumber(context) == null) {
            getLastContactedNumber(context)?.let { setTargetNumber(context, it) }
        }
        val targetNumber = getTargetNumber(context)
        val targetDisplay = targetNumber ?: context.getString(R.string.quick_compose_no_contact)

        val contactRemoteInput = RemoteInput.Builder(KEY_CONTACT_INPUT)
            .setLabel(context.getString(R.string.quick_compose_contact_hint))
            .build()

        val contactIntent = Intent(context, QuickComposeReceiver::class.java).apply {
            action = QuickComposeReceiver.ACTION_SET_CONTACT
        }
        val contactPendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE_CONTACT, contactIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        val contactAction = NotificationCompat.Action.Builder(
            R.drawable.ic_action_reply,
            context.getString(R.string.quick_compose_contact_action),
            contactPendingIntent,
        )
            .addRemoteInput(contactRemoteInput)
            .build()

        val messageRemoteInput = RemoteInput.Builder(KEY_MESSAGE_INPUT)
            .setLabel(context.getString(R.string.quick_compose_message_hint))
            .build()
        val sendIntent = Intent(context, QuickComposeReceiver::class.java).apply {
            action = QuickComposeReceiver.ACTION_SEND_MESSAGE
        }
        val sendPendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE_SEND, sendIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        val sendAction = NotificationCompat.Action.Builder(
            R.drawable.ic_action_reply,
            context.getString(R.string.quick_compose_send_action),
            sendPendingIntent,
        )
            .addRemoteInput(messageRemoteInput)
            .setAllowGeneratedReplies(true)
            .build()

        val contentText = "${context.getString(R.string.quick_compose_to_label)} $targetDisplay"
        val openIntent = MainActivity.createLaunchIntent(
            context = context,
            conversationAddress = targetNumber,
        )
        val openPendingIntent = PendingIntent.getActivity(
            context, REQUEST_CODE_OPEN, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val dismissIntent = Intent(context, QuickComposeReceiver::class.java).apply {
            action = QuickComposeReceiver.ACTION_DISMISSED
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE_DISMISS, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.quick_compose_title))
            .setContentText(contentText)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setDeleteIntent(dismissPendingIntent)
            .addAction(contactAction)
            .addAction(sendAction)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun getTargetNumber(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_TARGET_NUMBER, null)
    }

    fun setTargetNumber(context: Context, number: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TARGET_NUMBER, number)
            .commit() // synchronous: caller may be BroadcastReceiver without goAsync()
    }

    private fun getLastContactedNumber(context: Context): String? {
        val uri = Telephony.Sms.CONTENT_URI.buildUpon()
            .appendQueryParameter("limit", "1")
            .build()
        val cursor: Cursor? = try {
            context.contentResolver.query(
                uri,
                arrayOf(Telephony.Sms.ADDRESS),
                "${Telephony.Sms.TYPE} = ${Telephony.Sms.MESSAGE_TYPE_SENT}",
                null,
                "${Telephony.Sms.DATE} DESC",
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query last contacted number", e)
            null
        }
        return cursor?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    }

    fun hide(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    fun clearTargetNumber(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_TARGET_NUMBER)
            .commit() // synchronous: caller may be BroadcastReceiver without goAsync()
    }

    const val KEY_CONTACT_INPUT = "contact_input"
    const val KEY_MESSAGE_INPUT = "message_input"
}
