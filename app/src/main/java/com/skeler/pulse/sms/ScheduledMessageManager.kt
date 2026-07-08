package com.skeler.pulse.sms

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class ScheduledMessageManager(private val context: Context) {

    private val db get() = ScheduledMessageDatabase.getInstance(context)
    private val dao get() = db.scheduledMessageDao()

    suspend fun schedule(
        address: String,
        body: String,
        scheduledAtMillis: Long,
        subscriptionId: Int? = null,
        encryptedBody: String? = null,
    ): Long {
        val entity = ScheduledMessageEntity(
            address = address,
            body = body,
            scheduledAtMillis = scheduledAtMillis,
            subscriptionId = subscriptionId,
        )
        val id = dao.insert(entity)
        enqueueWorker(id, scheduledAtMillis, encryptedBody)
        return id
    }

    suspend fun cancel(id: Long) {
        dao.cancel(id)
        WorkManager.getInstance(context).cancelUniqueWork(scheduleWorkName(id))
    }

    suspend fun getPending(limit: Int = 50): List<ScheduledMessageEntity> {
        return dao.allPending().take(limit)
    }

    fun processPendingSync() {
        val db = ScheduledMessageDatabase.getInstance(context)
        val dao = db.scheduledMessageDao()
        val now = System.currentTimeMillis()
        kotlinx.coroutines.runBlocking {
            val pending = dao.pendingMessages(now)
            for (message in pending) {
                enqueueWorker(message.id, message.scheduledAtMillis)
            }
        }
    }

    private fun enqueueWorker(messageId: Long, scheduledAtMillis: Long, encryptedBody: String? = null) {
        val delayMs = maxOf(0L, scheduledAtMillis - System.currentTimeMillis())
        val workRequest = OneTimeWorkRequestBuilder<ScheduledMessageWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag("scheduled_message")
            .setInputData(
                androidx.work.workDataOf(
                    ScheduledMessageWorker.MESSAGE_ID_KEY to messageId,
                    ScheduledMessageWorker.ENCRYPTED_BODY_KEY to encryptedBody,
                )
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            scheduleWorkName(messageId),
            ExistingWorkPolicy.REPLACE,
            workRequest,
        )
    }

    private fun scheduleWorkName(messageId: Long) = "send_scheduled_$messageId"
}
