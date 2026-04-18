package com.skeler.pulse.database.di

import android.content.Context
import com.skeler.pulse.contracts.observability.ObservabilityProvider
import com.skeler.pulse.database.api.BusinessComplianceStatusStore
import com.skeler.pulse.database.api.EncryptedMessageStore
import com.skeler.pulse.database.api.PulseDatabaseFactory
import com.skeler.pulse.database.data.Base64CiphertextCodec
import com.skeler.pulse.database.data.PulseDatabase
import com.skeler.pulse.database.data.RoomBusinessComplianceProvider
import com.skeler.pulse.database.data.RoomEncryptedMessageStore
import com.skeler.pulse.observability.PulseObservabilityProvider
import com.skeler.pulse.security.api.BusinessComplianceProvider

data class DatabaseComponent(
    val database: PulseDatabase,
    val businessComplianceProvider: BusinessComplianceProvider,
    val businessComplianceStatusStore: BusinessComplianceStatusStore,
    val encryptedMessageStore: EncryptedMessageStore,
)

object DatabaseComponentFactory {
    fun create(
        context: Context,
        observabilityProvider: ObservabilityProvider = PulseObservabilityProvider("core.database"),
    ): DatabaseComponent {
        val database = PulseDatabaseFactory.create(context)
        val businessComplianceProvider = RoomBusinessComplianceProvider(
            businessComplianceDao = database.businessComplianceDao(),
        )
        return DatabaseComponent(
            database = database,
            businessComplianceProvider = businessComplianceProvider,
            businessComplianceStatusStore = businessComplianceProvider,
            encryptedMessageStore = RoomEncryptedMessageStore(
                encryptedMessageDao = database.encryptedMessageDao(),
                ciphertextCodec = Base64CiphertextCodec(),
                observabilityProvider = observabilityProvider,
            ),
        )
    }
}
