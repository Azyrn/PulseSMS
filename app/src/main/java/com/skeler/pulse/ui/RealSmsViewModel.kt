package com.skeler.pulse.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skeler.pulse.InboxAccessState
import com.skeler.pulse.contact.matchesBlockedSenderKey
import com.skeler.pulse.contact.toBlockedSenderKeyOrNull
import com.skeler.pulse.sms.ImportantMessagePreferences
import com.skeler.pulse.sms.InboxThreadPreferences
import com.skeler.pulse.sms.SmsThread
import com.skeler.pulse.sms.SystemSms
import com.skeler.pulse.sms.SystemSmsReader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private data class PendingSendRequest(
    val address: String,
    val body: String,
    val imageUris: List<Uri> = emptyList(),
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
    private var sendJob: Job? = null
    private var conversationJob: Job? = null
    private var activeConversationAddress: String? = null
    private var activeConversationThreadId: Long? = null
    private var pendingReadTarget: ReadConversationTarget? = null
    private var lastSendRequest: PendingSendRequest? = null
    private var sendSequence = 0
    private val loadedOlderMessages = mutableListOf<SystemSms>()
    private var hasMoreMessages: Boolean = false

    private fun observeInbox() {
        inboxJob?.cancel()
        inboxJob = viewModelScope.launch {
            try {
                combine(
                    smsReader.observeThreads(),
                    inboxThreadPreferences.pinnedThreadIds,
                    inboxThreadPreferences.archivedThreadIds,
                    inboxThreadPreferences.blockedAddresses,
                ) { threads, pinnedIds, archivedIds, blockedAddresses ->
                    InboxThreadPreferenceSnapshot(
                        threads = threads,
                        pinnedIds = pinnedIds,
                        archivedIds = archivedIds,
                        blockedAddresses = blockedAddresses,
                    )
                }.collectLatest { snapshot ->
                    val readTarget = pendingReadTarget
                    val threadsWithReadOverlay = if (readTarget == null) {
                        snapshot.threads
                    } else {
                        snapshot.threads.map { thread ->
                            if (thread.matchesReadTarget(readTarget)) thread.asRead() else thread
                        }
                    }
                    val sortedThreads = threadsWithReadOverlay
                        .withoutBlockedAddresses(snapshot.blockedAddresses)
                        .sortedWith(compareByDescending<SmsThread> { it.threadId in snapshot.pinnedIds }.thenByDescending { it.date })
                    val visibleThreads = sortedThreads.filterNot { it.threadId in snapshot.archivedIds }
                    val archivedThreads = sortedThreads.filter { it.threadId in snapshot.archivedIds }
                    _inboxState.value = _inboxState.value.copy(
                        threads = visibleThreads,
                        archivedThreads = archivedThreads,
                        pinnedThreadIds = snapshot.pinnedIds,
                        archivedThreadIds = snapshot.archivedIds,
                        blockedAddresses = snapshot.blockedAddresses,
                        loading = false,
                        showLoadingCard = false,
                        errorMessage = null,
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
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
        _conversationState.value = RealConversationState(
            address = address,
            loading = true,
            isReplyable = address.isReplyableConversationAddress(),
        )
        _inboxState.value = _inboxState.value.copy(
            threads = _inboxState.value.threads.map { thread ->
                if (thread.matchesReadTarget(pendingReadTarget!!)) thread.asRead() else thread
            },
        )
        val batchSize = SystemSmsReader.DEFAULT_MESSAGE_LIMIT
        loadedOlderMessages.clear()
        hasMoreMessages = false
        conversationJob = viewModelScope.launch {
            combine(
                smsReader.observeMessages(address = address, threadId = threadId, maxCount = batchSize),
                importantMessagePreferences.importantMessageIds,
            ) { (recent, totalCount), importantIds ->
                if (recent.size >= batchSize) {
                    hasMoreMessages = true
                }
                val loadedIds = loadedOlderMessages.mapTo(hashSetOf()) { it.id }
                val dedupedRecent = recent.filterNot { it.id in loadedIds }
                val allMessages = loadedOlderMessages + dedupedRecent
                val hasUnreadInbound = allMessages.hasUnreadInboundMessages()
                val visibleMessages = if (pendingReadTarget == ReadConversationTarget(address, threadId)) {
                    allMessages.map(SystemSms::asReadIfInbound)
                } else {
                    allMessages
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
                    isReplyable = address.isReplyableConversationAddress(),
                    hasMoreMessages = hasMoreMessages,
                    totalMessageCount = totalCount,
                ) to hasUnreadInbound
            }.collectLatest { (conversationState, hasUnreadInbound) ->
                _conversationState.value = conversationState
                if (hasUnreadInbound) {
                    smsReader.setThreadUnreadState(threadId = threadId, address = address, unread = false)
                }
            }
        }
    }

    fun loadMoreMessages() {
        if (!hasMoreMessages || _conversationState.value.loadingMore) return
        val currentState = _conversationState.value
        val beforeDate = loadedOlderMessages.firstOrNull()?.date
            ?: currentState.messages.firstOrNull()?.date
            ?: return
        val address = currentState.address
        val threadId = activeConversationThreadId

        _conversationState.value = _conversationState.value.copy(loadingMore = true)
        viewModelScope.launch {
            val batchSize = SystemSmsReader.DEFAULT_MESSAGE_LIMIT
            val olderMessages = smsReader.readOlderMessages(
                address = address,
                threadId = threadId,
                beforeDate = beforeDate,
                limit = batchSize,
            )
            hasMoreMessages = olderMessages.size >= batchSize
            val loadedIds = loadedOlderMessages.mapTo(hashSetOf()) { it.id }
            val deduped = olderMessages.filterNot { it.id in loadedIds }
            loadedOlderMessages.addAll(0, deduped)
            _conversationState.update { state ->
                val recentIds = olderMessages.mapTo(hashSetOf()) { it.id }
                val dedupedRecent = state.messages.filterNot { it.id in recentIds }
                state.copy(
                    messages = loadedOlderMessages + dedupedRecent,
                    hasMoreMessages = hasMoreMessages,
                    loadingMore = false,
                )
            }
        }
    }

    fun closeConversation() {
        sendJob?.cancel()
        sendJob = null
        conversationJob?.cancel()
        conversationJob = null
        activeConversationAddress = null
        activeConversationThreadId = null
        pendingReadTarget = null
        loadedOlderMessages.clear()
        hasMoreMessages = false
        _conversationState.value = RealConversationState(loading = false)
        _sendState.value = SendState.Idle
    }

    fun toggleImportantMessage(messageId: Long) {
        viewModelScope.launch {
            importantMessagePreferences.toggleImportant(messageId)
        }
    }

    private var pendingVoiceUri: Uri? = null

    fun sendMessage(address: String, body: String, imageUris: List<Uri> = emptyList(), subscriptionId: Int? = null) {
        val trimmedBody = body.trim()
        if (trimmedBody.isBlank() && imageUris.isEmpty()) return

        val request = PendingSendRequest(
            address = address,
            body = trimmedBody,
            imageUris = imageUris,
            subscriptionId = subscriptionId,
        )
        lastSendRequest = request

        sendJob?.cancel()
        val seq = ++sendSequence
        _sendState.value = SendState.Sending(trimmedBody)
        sendJob = viewModelScope.launch {
            try {
                if (imageUris.isNotEmpty()) {
                    smsReader.sendMms(address, trimmedBody, imageUris)
                } else {
                    smsReader.sendSms(address, trimmedBody, subscriptionId)
                }
                if (sendSequence == seq) {
                    _sendState.value = SendState.Sent(trimmedBody)
                }
            } catch (_: CancellationException) {
                if (sendSequence == seq) {
                    _sendState.value = SendState.Idle
                }
            } catch (_: Exception) {
                if (sendSequence == seq) {
                    _sendState.value = SendState.Failed(trimmedBody)
                }
            }
        }
    }

    fun sendVoiceMessage(address: String, audioUri: Uri) {
        pendingVoiceUri = audioUri
        sendJob?.cancel()
        val seq = ++sendSequence
        _sendState.value = SendState.Sending("")
        sendJob = viewModelScope.launch {
            try {
                smsReader.sendVoiceMms(address, "", audioUri)
                if (sendSequence == seq) {
                    _sendState.value = SendState.Sent("")
                }
            } catch (_: CancellationException) {
                if (sendSequence == seq) {
                    _sendState.value = SendState.Idle
                }
            } catch (_: Exception) {
                if (sendSequence == seq) {
                    _sendState.value = SendState.Failed("")
                }
            }
        }
    }

    fun retrySend() {
        val request = lastSendRequest ?: return
        if (_sendState.value !is SendState.Failed) return
        sendMessage(
            address = request.address,
            body = request.body,
            imageUris = request.imageUris,
            subscriptionId = request.subscriptionId,
        )
    }

    fun clearSendState() {
        _sendState.value = SendState.Idle
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

    fun blockThread(address: String) {
        val blockedKey = address.toBlockedSenderKeyOrNull() ?: return
        _inboxState.value = _inboxState.value.let { current ->
            val blockedAddresses = current.blockedAddresses
                .filterNot { existing -> existing.matchesBlockedSenderKey(blockedKey) }
                .toSet() + blockedKey
            current.copy(
                threads = current.threads.withoutBlockedAddresses(blockedAddresses),
                archivedThreads = current.archivedThreads.withoutBlockedAddresses(blockedAddresses),
                blockedAddresses = blockedAddresses,
            )
        }
        viewModelScope.launch {
            inboxThreadPreferences.blockAddress(address)
        }
    }

    fun unblockThread(address: String) {
        val blockedKey = address.toBlockedSenderKeyOrNull() ?: return
        _inboxState.value = _inboxState.value.let { current ->
            current.copy(
                blockedAddresses = current.blockedAddresses
                    .filterNot { existing -> existing.matchesBlockedSenderKey(blockedKey) }
                    .toSet(),
            )
        }
        viewModelScope.launch {
            inboxThreadPreferences.unblockAddress(address)
        }
    }

    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            smsReader.deleteMessage(messageId)
        }
    }

    fun deleteMessages(messages: List<SystemSms>) {
        viewModelScope.launch {
            smsReader.deleteMessages(messages)
        }
    }

    override fun onCleared() {
        inboxJob?.cancel()
        conversationJob?.cancel()
        super.onCleared()
    }
}

private data class InboxThreadPreferenceSnapshot(
    val threads: List<SmsThread>,
    val pinnedIds: Set<Long>,
    val archivedIds: Set<Long>,
    val blockedAddresses: Set<String>,
)
