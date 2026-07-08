package com.skeler.pulse.sms

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class MessageCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val prefs = MessageCleanupPreferences(applicationContext)
            val importantPrefs = ImportantMessagePreferences(applicationContext)
            val maxSms = prefs.getMaxSmsPerThread()
            val maxMms = prefs.getMaxMmsPerThread()

            if (maxSms == MessageCleanupPreferences.KEEP_ALL &&
                maxMms == MessageCleanupPreferences.KEEP_ALL
            ) {
                return Result.success()
            }

            val importantIds = importantPrefs.importantMessageIds.first()
            val reader = SystemSmsReader(applicationContext)
            val deleted = reader.cleanupMessages(
                maxSmsPerThread = maxSms,
                maxMmsPerThread = maxMms,
                importantMessageIds = importantIds,
            )
            if (deleted > 0) {
                Log.i(TAG, "Cleaned up $deleted old messages")
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Message cleanup failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "MessageCleanupWorker"
        const val UNIQUE_WORK_NAME = "periodic_message_cleanup"
        private const val UNIQUE_NOW_NAME = "cleanup_now"

        fun enqueueNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<MessageCleanupWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_NOW_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<MessageCleanupWorker>(
                24, TimeUnit.HOURS,
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
