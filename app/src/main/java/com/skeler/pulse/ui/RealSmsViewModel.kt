package com.skeler.pulse.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skeler.pulse.InboxAccessState
import com.skeler.pulse.sms.ImportantMessagePreferences
import com.skeler.pulse.sms.SmsThread
import com.skeler.pulse.sms.SystemSms
import com.skeler.pulse.sms.SystemSmsReader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal fun List<SystemSms>.hasUnreadInboundMessages(): Boolean = any { message ->
    message.isInbound && !message.read
}

private fun SystemSms.asReadIfInbound(): SystemSms = if (isInbound && !read) {
    copy(read = true)
} else {
    this
}

private fun SmsThread.asRead(): SmsThread = if (unreadCount > 0) {
    copy(unreadCount = 0)
} else {
    this
}

internal fun SmsThread.matchesReadTarget(target: ReadConversationTarget): Boolean = when {
    threadId == target.threadId -> true
    target.threadId == null && address.equals(target.address, ignoreCase = false) -> true
    else -> false
}

/**
 * State for the real SMS inbox, reading from [SystemSmsReader].
 */
data class RealInboxState(
    val threads: List<SmsThread> = emptyList(),
    val loading: Boolean = true,
    val permissionDenied: Boolean = false,
    val isDefaultSmsApp: Boolean = true,
    val errorMessage: String? = null,
)

internal data class ReadConversationTarget(
    val address: String,
    val threadId: Long?,
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

sealed interface SendState {
    data object Idle : SendState
    data class Sending(val body: String) : SendState
    data class Sent(val body: String) : SendState
    data class Failed(val body: String) : SendState
}

private data class PendingSendRequest(
    val address: String,
    val body: String,
    val subscriptionId: Int?,
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

    private val _sendState = MutableStateFlow<SendState>(SendState.Idle)
    val sendState: StateFlow<SendState> = _sendState.asStateFlow()

    private var inboxJob: Job? = null
    private var conversationJob: Job? = null
    private var activeConversationAddress: String? = null
    private var activeConversationThreadId: Long? = null
    private var pendingReadTarget: ReadConversationTarget? = null
    private var lastSendRequest: PendingSendRequest? = null

    private fun observeInbox() {
        inboxJob?.cancel()
        inboxJob = viewModelScope.launch {
            try {
                smsReader.observeThreads().collectLatest { threads ->
                    val readTarget = pendingReadTarget
                    val visibleThreads = if (readTarget == null) {
                        threads
                    } else {
                        threads.map { thread ->
                            if (thread.matchesReadTarget(readTarget)) thread.asRead() else thread
                        }
                    }
                    _inboxState.value = _inboxState.value.copy(
                        threads = visibleThreads,
                        loading = false,
                        errorMessage = null,
                    )
                }
            } catch (_: Exception) {
                _inboxState.value = _inboxState.value.copy(
                    threads = emptyList(),
                    loading = false,
                    errorMessage = "Pulse couldn't read your messages right now.",
                )
            }
        }
    }

    fun updateInboxAccessState(accessState: InboxAccessState) {
        _inboxState.value = _inboxState.value.copy(
            permissionDenied = accessState.permissionDenied,
            isDefaultSmsApp = accessState.isDefaultSmsApp,
        )
        if (accessState.isReady) {
            if (inboxJob?.isActive != true) {
                _inboxState.value = _inboxState.value.copy(loading = true, errorMessage = null)
                observeInbox()
            }
        } else {
            inboxJob?.cancel()
            inboxJob = null
            _inboxState.value = _inboxState.value.copy(
                threads = emptyList(),
                loading = false,
                errorMessage = null,
            )
        }
    }

    fun refreshInbox() {
        _inboxState.value = _inboxState.value.copy(
            loading = true,
            errorMessage = null,
        )
        observeInbox()
    }

    fun openConversation(address: String, threadId: Long? = null) {
        if (
            activeConversationAddress == address &&
            activeConversationThreadId == threadId &&
            conversationJob?.isActive == true
        ) return
        activeConversationAddress = address
        activeConversationThreadId = threadId
        pendingReadTarget = ReadConversationTarget(address = address, threadId = threadId)
        conversationJob?.cancel()
        _conversationState.value = RealConversationState(address = address, loading = true)
        _inboxState.value = _inboxState.value.copy(
            threads = _inboxState.value.threads.map { thread ->
                if (thread.matchesReadTarget(pendingReadTarget!!)) thread.asRead() else thread
            },
        )
        conversationJob = viewModelScope.launch {
            combine(
                smsReader.observeMessages(address = address, threadId = threadId),
                importantMessagePreferences.importantMessageIds,
            ) { messages, importantIds ->
                val visibleMessages = if (pendingReadTarget == ReadConversationTarget(address, threadId)) {
                    messages.map(SystemSms::asReadIfInbound)
                } else {
                    messages
                }
                val visibleImportantIds = visibleMessages.asSequence()
                    .map(SystemSms::id)
                    .filter(importantIds::contains)
                    .toSet()
                RealConversationState(
                    address = address,
                    messages = visibleMessages,
                    loading = false,
                    importantMessageIds = visibleImportantIds,
                )
            }.collectLatest { conversationState ->
                _conversationState.value = conversationState
                if (conversationState.messages.hasUnreadInboundMessages()) {
                    smsReader.markThreadAsRead(threadId = threadId, address = address)
                }
            }
        }
    }

    fun toggleImportantMessage(messageId: Long) {
        viewModelScope.launch {
            importantMessagePreferences.toggleImportant(messageId)
        }
    }

    fun sendMessage(address: String, body: String, subscriptionId: Int? = null) {
        val trimmedBody = body.trim()
        if (trimmedBody.isBlank()) return

        val request = PendingSendRequest(
            address = address,
            body = trimmedBody,
            subscriptionId = subscriptionId,
        )
        lastSendRequest = request
        _sendState.value = SendState.Sending(trimmedBody)

        viewModelScope.launch {
            try {
                smsReader.sendSms(address, trimmedBody, subscriptionId)
                _sendState.value = SendState.Sent(trimmedBody)
            } catch (_: Exception) {
                _sendState.value = SendState.Failed(trimmedBody)
            }
        }
    }

    fun retrySend() {
        val request = lastSendRequest ?: return
        if (_sendState.value !is SendState.Failed) return
        sendMessage(
            address = request.address,
            body = request.body,
            subscriptionId = request.subscriptionId,
        )
    }

    fun clearSendState() {
        if (_sendState.value !is SendState.Sending) {
            _sendState.value = SendState.Idle
        }
    }

    override fun onCleared() {
        inboxJob?.cancel()
        conversationJob?.cancel()
        super.onCleared()
    }
}
