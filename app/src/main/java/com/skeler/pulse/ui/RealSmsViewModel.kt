package com.skeler.pulse.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skeler.pulse.InboxAccessState
import com.skeler.pulse.sms.ImportantMessagePreferences
import com.skeler.pulse.sms.InboxThreadPreferences
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
    val archivedThreads: List<SmsThread> = emptyList(),
    val pinnedThreadIds: Set<Long> = emptySet(),
    val archivedThreadIds: Set<Long> = emptySet(),
    val loading: Boolean = true,
    val showLoadingCard: Boolean = false,
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
    private val inboxThreadPreferences: InboxThreadPreferences,
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
                combine(
                    smsReader.observeThreads(),
                    inboxThreadPreferences.pinnedThreadIds,
                    inboxThreadPreferences.archivedThreadIds,
                ) { threads, pinnedIds, archivedIds ->
                    Triple(threads, pinnedIds, archivedIds)
                }.collectLatest { (threads, pinnedIds, archivedIds) ->
                    val readTarget = pendingReadTarget
                    val threadsWithReadOverlay = if (readTarget == null) {
                        threads
                    } else {
                        threads.map { thread ->
                            if (thread.matchesReadTarget(readTarget)) thread.asRead() else thread
                        }
                    }
                    val sortedThreads = threadsWithReadOverlay
                        .sortedWith(compareByDescending<SmsThread> { it.threadId in pinnedIds }.thenByDescending { it.date })
                    val visibleThreads = sortedThreads.filterNot { it.threadId in archivedIds }
                    val archivedThreads = sortedThreads.filter { it.threadId in archivedIds }
                    _inboxState.value = _inboxState.value.copy(
                        threads = visibleThreads,
                        archivedThreads = archivedThreads,
                        pinnedThreadIds = pinnedIds,
                        archivedThreadIds = archivedIds,
                        loading = false,
                        showLoadingCard = false,
                        errorMessage = null,
                    )
                }
            } catch (_: Exception) {
                _inboxState.value = _inboxState.value.copy(
                    threads = emptyList(),
                    archivedThreads = emptyList(),
                    loading = false,
                    showLoadingCard = false,
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
                _inboxState.value = _inboxState.value.copy(
                    loading = true,
                    showLoadingCard = false,
                    errorMessage = null,
                )
                observeInbox()
            }
        } else {
            inboxJob?.cancel()
            inboxJob = null
            _inboxState.value = _inboxState.value.copy(
                threads = emptyList(),
                archivedThreads = emptyList(),
                loading = false,
                showLoadingCard = false,
                errorMessage = null,
            )
        }
    }

    fun refreshInbox() {
        _inboxState.value = _inboxState.value.copy(
            loading = true,
            showLoadingCard = true,
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
                val hasUnreadInbound = messages.hasUnreadInboundMessages()
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
                ) to hasUnreadInbound
            }.collectLatest { (conversationState, hasUnreadInbound) ->
                _conversationState.value = conversationState
                if (hasUnreadInbound) {
                    smsReader.setThreadUnreadState(threadId = threadId, address = address, unread = false)
                }
            }
        }
    }

    fun closeConversation() {
        conversationJob?.cancel()
        conversationJob = null
        activeConversationAddress = null
        activeConversationThreadId = null
        pendingReadTarget = null
        _conversationState.value = RealConversationState(loading = false)
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

    fun toggleThreadPinned(threadId: Long) {
        viewModelScope.launch {
            inboxThreadPreferences.togglePinned(threadId)
        }
    }

    fun toggleThreadArchived(threadId: Long) {
        viewModelScope.launch {
            inboxThreadPreferences.toggleArchived(threadId)
        }
    }

    fun setThreadUnread(threadId: Long?, address: String, unread: Boolean) {
        viewModelScope.launch {
            smsReader.setThreadUnreadState(threadId = threadId, address = address, unread = unread)
        }
    }

    fun deleteThread(threadId: Long?, address: String) {
        viewModelScope.launch {
            smsReader.deleteThread(threadId = threadId, address = address)
            if (threadId != null) {
                inboxThreadPreferences.removeThread(threadId)
            }
        }
    }

    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            smsReader.deleteMessage(messageId)
        }
    }

    override fun onCleared() {
        inboxJob?.cancel()
        conversationJob?.cancel()
        super.onCleared()
    }
}
