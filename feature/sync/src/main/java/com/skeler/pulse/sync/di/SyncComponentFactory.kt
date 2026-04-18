package com.skeler.pulse.sync.di

import android.content.Context
import com.skeler.pulse.contracts.observability.ObservabilityProvider
import com.skeler.pulse.database.api.BusinessComplianceStatusStore
import com.skeler.pulse.database.api.EncryptedMessageStore
import com.skeler.pulse.observability.PulseObservabilityProvider
import com.skeler.pulse.sync.api.ComplianceSyncTransport
import com.skeler.pulse.sync.api.SyncScheduler
import com.skeler.pulse.sync.api.SyncTransport
import com.skeler.pulse.sync.data.WorkManagerSyncScheduler
import com.skeler.pulse.sync.domain.MessageSyncOrchestrator
import com.skeler.pulse.sync.domain.SyncRetryPolicy

data class SyncComponent(
    val syncScheduler: SyncScheduler,
    val messageSyncOrchestrator: MessageSyncOrchestrator,
)

object SyncComponentFactory {
    fun create(
        context: Context,
        businessComplianceStatusStore: BusinessComplianceStatusStore,
        complianceTransport: ComplianceSyncTransport,
        encryptedMessageStore: EncryptedMessageStore,
        transport: SyncTransport,
        observabilityProvider: ObservabilityProvider = PulseObservabilityProvider("feature.sync"),
    ): SyncComponent {
        val retryPolicy = SyncRetryPolicy()
        return SyncComponent(
            syncScheduler = WorkManagerSyncScheduler(context),
            messageSyncOrchestrator = MessageSyncOrchestrator(
                businessComplianceStatusStore = businessComplianceStatusStore,
                complianceTransport = complianceTransport,
                encryptedMessageStore = encryptedMessageStore,
                transport = transport,
                retryPolicy = retryPolicy,
                observabilityProvider = observabilityProvider,
            ),
        )
    }
}
