package com.skeler.pulse.sync.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.skeler.pulse.sync.domain.MessageSyncOrchestrator
import com.skeler.pulse.sync.domain.SyncRunResult

class PulseSyncWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {

    override suspend fun doWork(): Result {
        val conversationId = inputData.getString(KEY_CONVERSATION_ID)
            ?: return Result.failure()
        val orchestrator = SyncWorkerDependenciesHolder.dependencies.messageSyncOrchestrator()
            ?: return Result.retry()

        return when (val result = orchestrator.run(conversationId)) {
            is SyncRunResult.Success -> Result.success()
            is SyncRunResult.PartialFailure ->
                if (result.failed > 0) {
                    Result.retry()
                } else {
                    Result.success()
                }
            is SyncRunResult.Failure -> Result.failure()
        }
    }

    companion object {
        const val KEY_CONVERSATION_ID: String = "conversation_id"
    }
}

interface SyncWorkerDependencies {
    fun messageSyncOrchestrator(): MessageSyncOrchestrator?
}

object SyncWorkerDependenciesHolder {
    var dependencies: SyncWorkerDependencies = object : SyncWorkerDependencies {
        override fun messageSyncOrchestrator(): MessageSyncOrchestrator? = null
    }
}
