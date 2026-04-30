package com.skeler.pulse

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.skeler.pulse.config.SyncRuntimeConfigProvider
import com.skeler.pulse.database.di.DatabaseComponentFactory
import com.skeler.pulse.messaging.data.DefaultConversationRepository
import com.skeler.pulse.messaging.domain.ObserveInboxUseCase
import com.skeler.pulse.messaging.domain.ObserveConversationUseCase
import com.skeler.pulse.messaging.domain.RequestConversationRefreshUseCase
import com.skeler.pulse.messaging.domain.SendMessageUseCase
import com.skeler.pulse.messaging.ui.MessagingViewModel
import com.skeler.pulse.observability.PulseObservabilityProvider
import com.skeler.pulse.security.di.SecurityComponentFactory
import com.skeler.pulse.sync.data.HttpComplianceSyncTransport
import com.skeler.pulse.sync.data.HttpSyncTransport
import com.skeler.pulse.sync.di.SyncComponent
import com.skeler.pulse.sync.di.SyncComponentFactory
import com.skeler.pulse.ui.PulseHomeViewModel
import com.skeler.pulse.ui.RealSmsViewModel
import com.skeler.pulse.sms.ImportantMessagePreferences
import com.skeler.pulse.sms.InboxThreadPreferences
import com.skeler.pulse.sms.SystemSmsReader

class AppContainer(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val syncRuntimeConfig = SyncRuntimeConfigProvider.fromBuildConfig()
    private val observabilityProvider = PulseObservabilityProvider("app")

    val databaseComponent = DatabaseComponentFactory.create(
        context = appContext,
        observabilityProvider = observabilityProvider,
    )

    val securityComponent = SecurityComponentFactory.create(
        context = appContext,
        observabilityProvider = observabilityProvider,
        businessComplianceProvider = databaseComponent.businessComplianceProvider,
    )

    val syncComponent: SyncComponent = SyncComponentFactory.create(
        context = appContext,
        businessComplianceStatusStore = databaseComponent.businessComplianceStatusStore,
        complianceTransport = HttpComplianceSyncTransport(
            endpointConfig = syncRuntimeConfig.endpointConfig,
            observabilityProvider = observabilityProvider,
        ),
        encryptedMessageStore = databaseComponent.encryptedMessageStore,
        transport = HttpSyncTransport(
            endpointConfig = syncRuntimeConfig.endpointConfig,
            observabilityProvider = observabilityProvider,
        ),
        observabilityProvider = observabilityProvider,
    )

    private val conversationRepository = DefaultConversationRepository(
        encryptedMessageStore = databaseComponent.encryptedMessageStore,
        messageProtector = securityComponent.messageProtector,
        businessComplianceProvider = securityComponent.businessComplianceProvider,
        syncScheduler = syncComponent.syncScheduler,
    )

    fun messagingViewModelFactory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MessagingViewModel(
                initialConversationId = MainActivity.DEFAULT_CONVERSATION_ID,
                observeConversation = ObserveConversationUseCase(conversationRepository),
                requestConversationRefresh = RequestConversationRefreshUseCase(conversationRepository),
                sendMessage = SendMessageUseCase(conversationRepository),
            ) as T
    }

    fun homeViewModelFactory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PulseHomeViewModel(
                observeInbox = ObserveInboxUseCase(conversationRepository),
                requestConversationRefresh = RequestConversationRefreshUseCase(conversationRepository),
            ) as T
    }

    fun realSmsViewModelFactory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RealSmsViewModel(
                smsReader = SystemSmsReader(appContext),
                importantMessagePreferences = ImportantMessagePreferences(appContext),
                inboxThreadPreferences = InboxThreadPreferences(appContext),
            ) as T
    }
}
