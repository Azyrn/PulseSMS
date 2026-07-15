package com.skeler.pulse.sms

import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsNotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val messageId = intent.getLongExtra(EXTRA_MESSAGE_ID, -1L)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val isMms = intent.getBooleanExtra(EXTRA_IS_MMS, false)

        // Telephony provider calls can block; keep them off the main thread.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_MARK_READ -> handleMarkRead(context, messageId, notificationId, isMms)
                    ACTION_DELETE -> handleDelete(context, messageId, notificationId, isMms)
                    ACTION_REPLY -> handleReply(context, intent, notificationId, isMms)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Notification action $action failed", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun messageUri(isMms: Boolean) =
        if (isMms) Telephony.Mms.CONTENT_URI else Telephony.Sms.CONTENT_URI

    private fun handleMarkRead(context: Context, messageId: Long, notificationId: Int, isMms: Boolean) {
        if (messageId < 0) return

        markMessageOrThreadRead(context, messageId, isMms)

        context.contentResolver.notifyChange(Telephony.Sms.CONTENT_URI, null)
        context.contentResolver.notifyChange(Telephony.Mms.CONTENT_URI, null)
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    private fun handleDelete(context: Context, messageId: Long, notificationId: Int, isMms: Boolean) {
        if (messageId < 0) return

        val threadId = resolveThreadId(context, messageId, isMms)

        context.contentResolver.delete(
            messageUri(isMms),
            "${Telephony.Sms._ID} = ?",
            arrayOf(messageId.toString()),
        )
        if (isMms) {
            deleteMmsPartCacheFile(context, messageId)
        }

        if (threadId != null && threadId > 0L) {
            val remainingSms = countSmsInThread(context, threadId)
            val remainingMms = countMmsInThread(context, threadId)
            if (remainingSms == 0 && remainingMms == 0) {
                deleteEmptyThread(context, threadId)
            } else {
                markThreadAsRead(context, threadId)
            }
        }

        context.contentResolver.notifyChange(Telephony.Sms.CONTENT_URI, null)
        context.contentResolver.notifyChange(Telephony.Mms.CONTENT_URI, null)
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    private suspend fun handleReply(context: Context, intent: Intent, notificationId: Int, isMms: Boolean) {
        val messageId = intent.getLongExtra(EXTRA_MESSAGE_ID, -1L)
        val results = RemoteInput.getResultsFromIntent(intent)
        val replyText = results?.getCharSequence(KEY_REPLY_TEXT)?.toString() ?: return
        if (replyText.isBlank()) return

        val address = intent.getStringExtra(EXTRA_SENDER_ADDRESS) ?: return

        if (messageId > 0) {
            markMessageOrThreadRead(context, messageId, isMms)
            context.contentResolver.notifyChange(Telephony.Sms.CONTENT_URI, null)
            context.contentResolver.notifyChange(Telephony.Mms.CONTENT_URI, null)
        }

        try {
            SystemSmsSender(context, Dispatchers.IO).sendSmsFireAndForget(address, replyText)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send reply via SMS", e)
        }

        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    private fun markMessageOrThreadRead(context: Context, messageId: Long, isMms: Boolean) {
        val threadId = resolveThreadId(context, messageId, isMms)
        if (threadId != null && threadId > 0L) {
            markThreadAsRead(context, threadId)
        } else {
            val values = ContentValues().apply {
                put(Telephony.Sms.READ, 1)
                put(Telephony.Sms.SEEN, 1)
            }
            context.contentResolver.update(
                messageUri(isMms),
                values,
                "${Telephony.Sms._ID} = ?",
                arrayOf(messageId.toString()),
            )
        }
    }

    private fun resolveThreadId(context: Context, messageId: Long, isMms: Boolean): Long? {
        val cursor = context.contentResolver.query(
            messageUri(isMms),
            arrayOf("thread_id"),
            "${Telephony.Sms._ID} = ?",
            arrayOf(messageId.toString()),
            null,
        )
        return cursor?.use {
            if (it.moveToFirst()) it.getLong(0) else null
        }
    }

    private fun countSmsInThread(context: Context, threadId: Long): Int {
        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms._ID),
            "${Telephony.Sms.THREAD_ID} = ?",
            arrayOf(threadId.toString()),
            null,
        )
        return cursor?.use { it.count } ?: 0
    }

    private fun countMmsInThread(context: Context, threadId: Long): Int {
        val cursor = try {
            context.contentResolver.query(
                Telephony.Mms.CONTENT_URI,
                arrayOf("_id"),
                "thread_id = ?",
                arrayOf(threadId.toString()),
                null,
            )
        } catch (e: SecurityException) {
            null
        }
        return cursor?.use { it.count } ?: 0
    }

    private fun markThreadAsRead(context: Context, threadId: Long) {
        // Only touch unread rows — updating already-read rows spams content observers.
        val values = ContentValues().apply {
            put(Telephony.Sms.READ, 1)
            put(Telephony.Sms.SEEN, 1)
        }
        context.contentResolver.update(
            Telephony.Sms.CONTENT_URI,
            values,
            "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.READ} = 0",
            arrayOf(threadId.toString()),
        )
        val mmsValues = ContentValues().apply {
            put("read", 1)
            put("seen", 1)
        }
        context.contentResolver.update(
            Telephony.Mms.CONTENT_URI,
            mmsValues,
            "thread_id = ? AND read = 0",
            arrayOf(threadId.toString()),
        )
    }

    private fun deleteEmptyThread(context: Context, threadId: Long) {
        // The thread is already empty; deleting the conversation URI removes the
        // orphaned thread row so other SMS apps don't show a ghost conversation.
        try {
            context.contentResolver.delete(
                ContentUris.withAppendedId(Telephony.Threads.CONTENT_URI, threadId),
                null, null,
            )
        } catch (e: Exception) {
            Log.w(TAG, "Could not delete empty thread $threadId", e)
        }
    }

    private fun deleteMmsPartCacheFile(context: Context, mmsId: Long) {
        val file = java.io.File(context.cacheDir, "mms_parts/$mmsId")
        if (file.exists()) file.delete()
    }

    companion object {
        private const val TAG = "SmsNotificationAction"
        const val ACTION_MARK_READ = "com.skeler.pulse.action.MARK_READ"
        const val ACTION_DELETE = "com.skeler.pulse.action.DELETE"
        const val ACTION_REPLY = "com.skeler.pulse.action.REPLY"
        const val KEY_REPLY_TEXT = "reply_text"
        const val EXTRA_MESSAGE_ID = "extra_message_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_SENDER_ADDRESS = "extra_sender_address"
        const val EXTRA_IS_MMS = "extra_is_mms"
    }
}
