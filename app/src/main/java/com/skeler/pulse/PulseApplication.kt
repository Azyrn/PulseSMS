package com.skeler.pulse

import android.app.Application
import android.content.IntentFilter
import android.os.Build
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat
import com.skeler.pulse.sms.MessageCleanupWorker
import com.skeler.pulse.sms.NotificationPreferences
import com.skeler.pulse.sms.QuickComposeNotificationManager
import com.skeler.pulse.sms.SmsNotificationHelper
import com.skeler.pulse.sms.SmsProcessingHelper
import com.skeler.pulse.sms.SmsReceiver
import com.skeler.pulse.sync.worker.SyncWorkerDependenciesHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PulseApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    companion object {
        private const val TAG = "PulseApplication"
    }

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
        SyncWorkerDependenciesHolder.dependencies = object : com.skeler.pulse.sync.worker.SyncWorkerDependencies {
            override fun messageSyncOrchestrator() = appContainer.syncComponent.messageSyncOrchestrator
        }
        SmsNotificationHelper.createNotificationChannel(this)
        QuickComposeNotificationManager.createChannel(this)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (NotificationPreferences(this@PulseApplication).isQuickComposeEnabled()) {
                    QuickComposeNotificationManager.show(this@PulseApplication)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show quick compose notification", e)
            }
        }

        MessageCleanupWorker.schedulePeriodic(this)

        registerSmsFallbackReceiver()
    }

    private fun registerSmsFallbackReceiver() {
        val filter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
        val receiverFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.RECEIVER_EXPORTED
        } else {
            0
        }
        ContextCompat.registerReceiver(
            this,
            SmsReceiver(),
            filter,
            receiverFlags,
        )
    }
}
