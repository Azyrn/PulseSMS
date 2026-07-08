package com.skeler.pulse.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.skeler.pulse.MainActivity
import com.skeler.pulse.R
import com.skeler.pulse.contact.displayNameFor

object SmsNotificationHelper {

    private const val CHANNEL_ID = "pulse_sms_channel"
    private const val CHANNEL_NAME = "Messages"
    private const val CHANNEL_DESCRIPTION = "Incoming SMS and MMS messages"
    private const val REQUEST_CODE_OFFSET_OPEN = 100000
    private const val REQUEST_CODE_OFFSET_REPLY = 150000
    private const val REQUEST_CODE_OFFSET_MARK_READ = 200000
    private const val REQUEST_CODE_OFFSET_DELETE = 300000

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

    fun notifyIncomingSms(
        context: Context,
        sender: String,
        body: String,
        messageId: Long = -1L,
        notificationId: Int = sender.hashCode() and 0x7fffffff,
        imageUri: android.net.Uri? = null,
        quickReplyEnabled: Boolean = true,
        isMms: Boolean = false,
    ) {
        val launchIntent = MainActivity.createLaunchIntent(
            context = context,
            conversationAddress = sender,
        )
        val openAppIntent = PendingIntent.getActivity(
            context, notificationId, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(displayNameFor(context, sender))
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent)

        val bitmap = imageUri?.let { uri ->
            try {
                // First pass: decode bounds to compute sample size
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, opts)
                }
                val sampleSize = maxOf(opts.outWidth / 512, maxOf(opts.outHeight / 512, 1))
                // Second pass: decode with sample size
                val bmp = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.Options().apply { inSampleSize = sampleSize }
                        .let { BitmapFactory.decodeStream(stream, null, it) }
                }
                Log.i("SmsNotificationHelper", "Notification bitmap: ${bmp?.width}x${bmp?.height} (sample=$sampleSize) from $uri")
                bmp
            } catch (e: Exception) {
                Log.e("SmsNotificationHelper", "Failed to decode notification image from $uri", e)
                null
            }
        }
        if (bitmap != null) {
            builder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap).bigLargeIcon(null as android.graphics.Bitmap?))
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }

        if (messageId > 0) {
            if (quickReplyEnabled) {
                val remoteInput = RemoteInput.Builder(SmsNotificationActionReceiver.KEY_REPLY_TEXT)
                    .setLabel(context.getString(R.string.notification_action_reply))
                    .build()

                val replyIntent = Intent(context, SmsNotificationActionReceiver::class.java).apply {
                    action = SmsNotificationActionReceiver.ACTION_REPLY
                    putExtra(SmsNotificationActionReceiver.EXTRA_MESSAGE_ID, messageId)
                    putExtra(SmsNotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                    putExtra(SmsNotificationActionReceiver.EXTRA_SENDER_ADDRESS, sender)
                    putExtra(SmsNotificationActionReceiver.EXTRA_IS_MMS, isMms)
                }
                val replyPendingIntent = PendingIntent.getBroadcast(
                    context, notificationId + REQUEST_CODE_OFFSET_REPLY, replyIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                )
                val replyAction = NotificationCompat.Action.Builder(
                    R.drawable.ic_action_reply, context.getString(R.string.notification_action_reply), replyPendingIntent,
                )
                    .addRemoteInput(remoteInput)
                    .setAllowGeneratedReplies(true)
                    .build()
                builder.addAction(replyAction)
            }

            builder.addAction(
                R.drawable.ic_action_mark_read,
                context.getString(R.string.notification_action_mark_read),
                createActionIntent(context, SmsNotificationActionReceiver.ACTION_MARK_READ, messageId, notificationId, isMms),
            )
            builder.addAction(
                R.drawable.ic_action_delete,
                context.getString(R.string.notification_action_delete),
                createActionIntent(context, SmsNotificationActionReceiver.ACTION_DELETE, messageId, notificationId, isMms),
            )
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            Log.e("SmsNotificationHelper", "POST_NOTIFICATIONS not granted", e)
        } catch (e: Exception) {
            Log.e("SmsNotificationHelper", "Failed to show notification", e)
        }
    }

    private fun createActionIntent(
        context: Context,
        action: String,
        messageId: Long,
        notificationId: Int,
        isMms: Boolean = false,
    ): PendingIntent {
        val intent = Intent(context, SmsNotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(SmsNotificationActionReceiver.EXTRA_MESSAGE_ID, messageId)
            putExtra(SmsNotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(SmsNotificationActionReceiver.EXTRA_IS_MMS, isMms)
        }
        val requestCode = when (action) {
            SmsNotificationActionReceiver.ACTION_MARK_READ -> notificationId + REQUEST_CODE_OFFSET_MARK_READ
            SmsNotificationActionReceiver.ACTION_DELETE -> notificationId + REQUEST_CODE_OFFSET_DELETE
            else -> notificationId
        }
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
