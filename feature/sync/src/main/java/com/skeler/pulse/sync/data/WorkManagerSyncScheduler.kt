package com.skeler.pulse.sync.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.skeler.pulse.sync.api.SyncScheduler
import com.skeler.pulse.sync.worker.PulseSyncWorker
import java.util.concurrent.TimeUnit
import kotlin.math.max

class WorkManagerSyncScheduler(
    private val context: Context,
) : SyncScheduler {
    override fun enqueueConversationSync(conversationId: String, runAtEpochMillis: Long?) {
        val delayMillis = runAtEpochMillis?.let { max(0L, it - System.currentTimeMillis()) } ?: 0L
        val requestBuilder = OneTimeWorkRequestBuilder<PulseSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInputData(workDataOf(PulseSyncWorker.KEY_CONVERSATION_ID to conversationId))
        if (delayMillis > 0L) {
            requestBuilder.setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
        }
        val request = requestBuilder.build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueWorkName = uniqueWorkName(conversationId),
            existingWorkPolicy = ExistingWorkPolicy.REPLACE,
            request = request,
        )
    }

    companion object {
        fun uniqueWorkName(conversationId: String): String = "pulse_sync_$conversationId"
    }
}
