package com.skeler.pulse.sms

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf

class ScheduledMessageWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): ListenableWorker.Result {
        val messageId = inputData.getLong(MESSAGE_ID_KEY, -1L)
        if (messageId == -1L) return ListenableWorker.Result.failure()

        val encryptedBody = inputData.getString(ENCRYPTED_BODY_KEY)

        val db = ScheduledMessageDatabase.getInstance(applicationContext)
        val dao = db.scheduledMessageDao()
        val message = dao.findById(messageId) ?: return ListenableWorker.Result.failure()

        if (message.isSent || message.isCancelled) return ListenableWorker.Result.success()

        return try {
            val sender = SystemSmsSender(applicationContext, ioDispatcher = kotlinx.coroutines.Dispatchers.IO)
            sender.sendSms(
                address = message.address,
                body = message.body,
                subscriptionId = message.subscriptionId,
                waitForDelivery = false,
                encryptedBody = encryptedBody,
            )
            dao.markSent(messageId)
            ListenableWorker.Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) ListenableWorker.Result.retry() else ListenableWorker.Result.failure(
                workDataOf("error" to (e.message ?: "unknown"))
            )
        }
    }

    companion object {
        const val MESSAGE_ID_KEY = "scheduled_message_id"
        const val ENCRYPTED_BODY_KEY = "encrypted_body"
    }
}
