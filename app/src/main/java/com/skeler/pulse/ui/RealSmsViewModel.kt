package com.skeler.pulse.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skeler.pulse.sms.SmsThread
import com.skeler.pulse.sms.ImportantMessagePreferences
import com.skeler.pulse.sms.SystemSms
import com.skeler.pulse.sms.SystemSmsReader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * State for the real SMS inbox, reading from [SystemSmsReader].
 */
data class RealInboxState(
    val threads: List<SmsThread> = emptyList(),
    val loading: Boolean = true,
)

/**
 * State for a single conversation's messages.
 */
data class RealConversationState(
    val address: String = "",
    val messages: List<SystemSms> = emptyList(),
    val loading: Boolean = true,
    val importantMessageIds: Set<Long> = emptySet(),
)

/**
 * ViewModel that reads real SMS from the system content provider.
 *
 * Replaces the fake [PulseHomeViewModel] with actual phone messages.
 * Requires [android.permission.READ_SMS].
 */
class RealSmsViewModel(
    private val smsReader: SystemSmsReader,
    private val importantMessagePreferences: ImportantMessagePreferences,
) : ViewModel() {

    private val _inboxState = MutableStateFlow(RealInboxState())
    val inboxState: StateFlow<RealInboxState> = _inboxState.asStateFlow()

    private val _conversationState = MutableStateFlow(RealConversationState())
    val conversationState: StateFlow<RealConversationState> = _conversationState.asStateFlow()
    private var conversationJob: Job? = null
    private var activeConversationAddress: String? = null

    init {
        observeInbox()
    }

    private fun observeInbox() {
        viewModelScope.launch {
            smsReader.observeThreads().collectLatest { threads ->
                _inboxState.value = RealInboxState(threads = threads, loading = false)
            }
        }
    }

    fun openConversation(address: String) {
        if (activeConversationAddress == address && conversationJob?.isActive == true) return
        activeConversationAddress = address
        conversationJob?.cancel()
        _conversationState.value = RealConversationState(address = address, loading = true)
        conversationJob = viewModelScope.launch {
            combine(
                smsReader.observeMessages(address),
                importantMessagePreferences.importantMessageIds,
            ) { messages, importantIds ->
                val visibleImportantIds = messages.asSequence()
                    .map(SystemSms::id)
                    .filter(importantIds::contains)
                    .toSet()
                RealConversationState(
                    address = address,
                    messages = messages,
                    loading = false,
                    importantMessageIds = visibleImportantIds,
                )
            }.collectLatest { conversationState ->
                _conversationState.value = conversationState
            }
        }
    }

    fun toggleImportantMessage(messageId: Long) {
        viewModelScope.launch {
            importantMessagePreferences.toggleImportant(messageId)
        }
    }

    fun sendMessage(address: String, body: String) {
        if (body.isBlank()) return
        viewModelScope.launch {
            try {
                smsReader.sendSms(address, body)
            } catch (_: Exception) {
                // Send failed — will be visible as message not appearing
            }
        }
    }

    override fun onCleared() {
        conversationJob?.cancel()
        super.onCleared()
    }
}
