package com.skeler.pulse.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.skeler.pulse.MainActivity
import com.skeler.pulse.R

/**
 * Notification helper for incoming SMS messages.
 *
 * Creates the required [NotificationChannel] on API 26+ and posts
 * heads-up notifications for received messages.
 */
object SmsNotificationHelper {

    private const val CHANNEL_ID = "pulse_sms_channel"
    private const val CHANNEL_NAME = "Messages"
    private const val CHANNEL_DESCRIPTION = "Incoming SMS and MMS messages"

    /**
     * Creates the SMS notification channel. Safe to call multiple times —
     * Android no-ops if the channel already exists.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    /**
     * Posts a notification for a received SMS.
     *
     * @param sender The sender address (phone number)
     * @param body   The message body text
     */
    fun notifyIncomingSms(
        context: Context,
        sender: String,
        body: String,
        notificationId: Int = sender.hashCode(),
    ) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(sender)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS permission not granted — silent fail
        }
    }
}
