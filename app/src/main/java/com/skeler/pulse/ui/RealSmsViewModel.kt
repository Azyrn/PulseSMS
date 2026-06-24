package com.skeler.pulse.ui

import android.content.Context
import android.net.Uri

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skeler.pulse.InboxAccessState
import com.skeler.pulse.contact.matchesBlockedSenderKey
import com.skeler.pulse.contact.toBlockedSenderKeyOrNull
import com.skeler.pulse.R
import com.skeler.pulse.sms.DraftPreferences
import com.skeler.pulse.sms.EncryptionPreferences
import com.skeler.pulse.sms.ImportantMessagePreferences
import com.skeler.pulse.sms.InboxThreadPreferences
import com.skeler.pulse.sms.MessageCleanupPreferences
import com.skeler.pulse.sms.MessageReactionPreferences
import com.skeler.pulse.sms.ReactionParser
import com.skeler.pulse.sms.ScheduledMessageDatabase
import com.skeler.pulse.sms.ScheduledMessageManager
import com.skeler.pulse.sms.SmsBackupManager
import com.skeler.pulse.sms.SmsEncryptionManager
import com.skeler.pulse.sms.SmsThread
import com.skeler.pulse.sms.SystemSms
import com.skeler.pulse.sms.SystemSmsReader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    private val context: Context,
    private val smsReader: SystemSmsReader,
    private val importantMessagePreferences: ImportantMessagePreferences,
    private val inboxThreadPreferences: InboxThreadPreferences,
    private val messageReactionPreferences: MessageReactionPreferences,
    private val encryptionPreferences: EncryptionPreferences,
) : ViewModel() {

    private val draftPreferences = DraftPreferences(context)
    private val encryptionManager = SmsEncryptionManager(context)
    private val scheduledMessageManager = ScheduledMessageManager(context)
    private val backupManager = SmsBackupManager(context, encryptionManager)
    private val scheduledDao = ScheduledMessageDatabase.getInstance(context).scheduledMessageDao()

    private val _inboxState = MutableStateFlow(RealInboxState())
    val inboxState: StateFlow<RealInboxState> = _inboxState.asStateFlow()

    private val _conversationState = MutableStateFlow(RealConversationState())
    val conversationState: StateFlow<RealConversationState> = _conversationState.asStateFlow()

    private val _sendState = MutableStateFlow<SendState>(SendState.Idle)
    val sendState: StateFlow<SendState> = _sendState.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    private val cleanupPreferences by lazy { MessageCleanupPreferences(context) }

    private var inboxJob: Job? = null
    private var scheduledJob: Job? = null
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
            scheduledJob?.cancel()
            scheduledJob = launch {
                scheduledDao.observePending().collect { messages ->
                    val addresses = messages.map { it.address }.toSet()
                    _inboxState.update { it.copy(scheduledAddresses = addresses) }
                }
            }

            try {
                combine(
                    smsReader.observeThreads(),
                    inboxThreadPreferences.pinnedThreadIds,
                    inboxThreadPreferences.archivedThreadIds,
                    inboxThreadPreferences.blockedAddresses,
                    inboxThreadPreferences.threadEmojis,
                ) { threads, pinnedIds, archivedIds, blockedAddresses, threadEmojis ->
                    InboxThreadPreferenceSnapshot(
                        threads = threads,
                        pinnedIds = pinnedIds,
                        archivedIds = archivedIds,
                        blockedAddresses = blockedAddresses,
                        threadEmojis = threadEmojis,
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
                    val decryptedThreads = threadsWithReadOverlay.map { thread ->
                        if (encryptionManager.isEncrypted(thread.snippet)) {
                            thread.copy(snippet = encryptionManager.decrypt(thread.snippet)
                                ?: SmsEncryptionManager.KEY_LOST_PLACEHOLDER)
                        } else {
                            thread
                        }
                    }
                    val sortedThreads = decryptedThreads
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
                        threadEmojis = snapshot.threadEmojis,
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
                    errorMessage = context.getString(R.string.inbox_read_error_body),
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
                messageReactionPreferences.messageReactions,
                scheduledDao.observePendingForAddress(address),
            ) { (recent, totalCount), importantIds, messageReactions, scheduledMessages ->
                if (recent.size >= batchSize) {
                    hasMoreMessages = true
                }
                val loadedIds = loadedOlderMessages.mapTo(hashSetOf()) { it.id }
                val dedupedRecent = recent.filterNot { it.id in loadedIds }
                val allMessages = loadedOlderMessages + dedupedRecent
                val decryptedMessages = allMessages.map { message ->
                    if (encryptionManager.isEncrypted(message.body)) {
                        message.copy(body = encryptionManager.decrypt(message.body)
                            ?: SmsEncryptionManager.KEY_LOST_PLACEHOLDER)
                    } else {
                        message
                    }
                }
                val hasUnreadInbound = decryptedMessages.hasUnreadInboundMessages()
                val visibleMessages = if (pendingReadTarget == ReadConversationTarget(address, threadId)) {
                    decryptedMessages.map(SystemSms::asReadIfInbound)
                } else {
                    decryptedMessages
                }

                val reactionMessageIds = mutableSetOf<Long>()
                val parsedReactions = mutableMapOf<Long, String>()
                val unmatched = mutableListOf<UnmatchedReaction>()
                for (msg in visibleMessages) {
                    val parsed = ReactionParser.parseReaction(msg.body) ?: continue
                    reactionMessageIds.add(msg.id)
                    if (msg.isOutbound) continue
                    // inbound echo of our own reaction SMS (self-testing)
                    if (visibleMessages.any { it.id != msg.id && it.isOutbound && it.body == msg.body }) continue
                    val target = ReactionParser.findMessageMatch(
                        referencedText = parsed.referencedText,
                        messages = visibleMessages,
                    )
                    if (target != null) {
                        parsedReactions[target.id] = parsed.emoji
                    } else {
                        unmatched += UnmatchedReaction(
                            emoji = parsed.emoji,
                            referencedText = parsed.referencedText,
                            date = msg.date,
                        )
                    }
                }
                val filteredMessages = visibleMessages.filterNot { it.id in reactionMessageIds }

                val mergedReactions = messageReactions.toMutableMap()
                for ((targetId, emoji) in parsedReactions) {
                    if (targetId !in mergedReactions) {
                        mergedReactions[targetId] = emoji
                    }
                }

                val visibleImportantIds = filteredMessages.asSequence()
                    .map(SystemSms::id)
                    .filter(importantIds::contains)
                    .toSet()
                RealConversationState(
                    address = address,
                    messages = filteredMessages,
                    loading = false,
                    importantMessageIds = visibleImportantIds,
                    messageReactions = mergedReactions,
                    unmatchedReactions = unmatched,
                    isReplyable = address.isReplyableConversationAddress(),
                    hasMoreMessages = hasMoreMessages,
                    totalMessageCount = totalCount,
                    scheduledMessages = scheduledMessages,
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
            val deduped = olderMessages.filterNot { it.id in loadedIds }.map { message ->
                if (encryptionManager.isEncrypted(message.body)) {
                    message.copy(body = encryptionManager.decrypt(message.body)
                        ?: SmsEncryptionManager.KEY_LOST_PLACEHOLDER)
                } else {
                    message
                }
            }
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

    fun setMessageReaction(messageId: Long, emoji: String?) {
        val conversationState = _conversationState.value
        val message = conversationState.messages.firstOrNull { it.id == messageId }
        // TODO(reaction-sms): send reaction as SMS — blocked by carrier compatibility testing
        val allIds = if (emoji != null && message != null && message.body.isNotBlank()) {
            val ids = mutableListOf(messageId)
            val previous = ReactionParser.findMessageMatch(
                message.body,
                conversationState.messages.filter { it.date < message.date },
            )
            if (previous != null && previous.id != messageId) {
                ids.add(previous.id)
            }
            ids
        } else {
            listOf(messageId)
        }
        viewModelScope.launch {
            allIds.forEach { id ->
                messageReactionPreferences.setReaction(id, emoji)
            }
            _conversationState.update { state ->
                val updated = if (emoji != null) {
                    allIds.fold(state.messageReactions) { acc, id -> acc + (id to emoji) }
                } else {
                    allIds.fold(state.messageReactions) { acc, id -> acc - id }
                }
                state.copy(messageReactions = updated)
            }
        }
    }

    private var pendingVoiceUri: Uri? = null

    fun sendMessage(address: String, body: String, imageUris: List<Uri> = emptyList(), subscriptionId: Int? = null) {
        val trimmedBody = body.trim()
        if (trimmedBody.isBlank() && imageUris.isEmpty()) return

        viewModelScope.launch {
            draftPreferences.clearDraft(address)
            _conversationState.update { it.copy(draft = "") }
        }

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
                val encryptedBody = if (imageUris.isEmpty() && encryptionPreferences.isEncryptionEnabled()) {
                    encryptionManager.encrypt(trimmedBody)
                } else {
                    null
                }
                if (imageUris.isNotEmpty()) {
                    smsReader.sendMms(address, trimmedBody, imageUris)
                } else {
                    smsReader.sendSms(address, trimmedBody, subscriptionId, waitForDelivery = false, encryptedBody = encryptedBody)
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

    fun loadDraft(address: String) {
        viewModelScope.launch {
            draftPreferences.observeDraft(address).collect { draftText ->
                _conversationState.update { it.copy(draft = draftText) }
            }
        }
    }

    fun saveDraft(address: String, text: String) {
        viewModelScope.launch {
            draftPreferences.saveDraft(address, text)
            _conversationState.update { it.copy(draft = text) }
        }
    }

    fun scheduleMessage(address: String, body: String, scheduledAtMillis: Long, subscriptionId: Int? = null) {
        viewModelScope.launch {
            val encryptedBody = encryptionManager.encrypt(body)
            scheduledMessageManager.schedule(address, body, scheduledAtMillis, subscriptionId, encryptedBody = encryptedBody)
            draftPreferences.clearDraft(address)
            _conversationState.update { it.copy(draft = "") }
        }
    }

    fun cancelScheduledMessage(messageId: Long) {
        viewModelScope.launch {
            scheduledMessageManager.cancel(messageId)
        }
    }

    fun exportBackup() {
        viewModelScope.launch {
            try {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                )
                val file = java.io.File(downloadsDir, "PulseSMS_backup_${System.currentTimeMillis()}.xml")
                val count = java.io.FileOutputStream(file).use { outputStream ->
                    backupManager.exportSms(outputStream)
                }
                _userMessage.tryEmit(context.getString(R.string.backup_export_success, count))
            } catch (_: Exception) {
                _userMessage.tryEmit(context.getString(R.string.backup_export_failed))
            }
        }
    }

    fun exportBackupToUri(uri: Uri) {
        viewModelScope.launch {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val count = backupManager.exportSms(outputStream)
                    _userMessage.tryEmit(context.getString(R.string.backup_export_success, count))
                } ?: run {
                    _userMessage.tryEmit(context.getString(R.string.backup_export_failed))
                }
            } catch (_: Exception) {
                _userMessage.tryEmit(context.getString(R.string.backup_export_failed))
            }
        }
    }

    fun importBackupFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val count = backupManager.importSms(inputStream)
                    _userMessage.tryEmit(context.getString(R.string.backup_import_success, count))
                } ?: run {
                    _userMessage.tryEmit(context.getString(R.string.backup_import_failed))
                }
            } catch (_: Exception) {
                _userMessage.tryEmit(context.getString(R.string.backup_import_failed))
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

    fun setThreadEmoji(threadId: Long, emoji: String?) {
        viewModelScope.launch {
            inboxThreadPreferences.setThreadEmoji(threadId, emoji)
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

    fun runCleanupNow(onResult: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val maxSms = cleanupPreferences.getMaxSmsPerThread()
            val maxMms = cleanupPreferences.getMaxMmsPerThread()
            if (maxSms == MessageCleanupPreferences.KEEP_ALL &&
                maxMms == MessageCleanupPreferences.KEEP_ALL
            ) {
                onResult(0)
                return@launch
            }
            val importantIds = importantMessagePreferences.importantMessageIds.first()
            val deleted = smsReader.cleanupMessages(
                maxSmsPerThread = maxSms,
                maxMmsPerThread = maxMms,
                importantMessageIds = importantIds,
            )
            onResult(deleted)
        }
    }

    fun hasDraftForAddress(address: String): Boolean {
        return _inboxState.value.drafts.containsKey(address)
    }

    fun loadDraftForAddress(address: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                onResult(draftPreferences.observeDraft(address).first())
            } catch (_: Exception) { }
        }
    }

    override fun onCleared() {
        inboxJob?.cancel()
        scheduledJob?.cancel()
        conversationJob?.cancel()
        sendJob?.cancel()
        super.onCleared()
    }
}

private data class InboxThreadPreferenceSnapshot(
    val threads: List<SmsThread>,
    val pinnedIds: Set<Long>,
    val archivedIds: Set<Long>,
    val blockedAddresses: Set<String>,
    val threadEmojis: Map<Long, String>,
)
