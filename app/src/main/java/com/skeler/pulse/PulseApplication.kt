package com.skeler.pulse

import android.app.Application
import com.skeler.pulse.sms.SmsNotificationHelper
import com.skeler.pulse.sync.worker.SyncWorkerDependenciesHolder

class PulseApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
        SyncWorkerDependenciesHolder.dependencies = object : com.skeler.pulse.sync.worker.SyncWorkerDependencies {
            override fun messageSyncOrchestrator() = appContainer.syncComponent.messageSyncOrchestrator
        }
        SmsNotificationHelper.createNotificationChannel(this)
    }
}
