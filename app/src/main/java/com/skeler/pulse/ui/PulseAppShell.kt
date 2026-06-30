package com.skeler.pulse.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri as AndroidUri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.core.app.NotificationManagerCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.skeler.pulse.InboxAccessState
import com.skeler.pulse.PulseLaunchRequest
import com.skeler.pulse.contact.displayNameFor
import com.skeler.pulse.contact.matchesBlockedSenderKey
import com.skeler.pulse.contact.toBlockedSenderKeyOrNull
import com.skeler.pulse.design.theme.SerafinaThemeViewModel
import com.skeler.pulse.design.util.rememberReducedMotionEnabled
import com.skeler.pulse.security.auth.checkBiometricAvailability
import com.skeler.pulse.R
import com.skeler.pulse.shouldHandleOpenNewChatRequest

@Composable
fun PulseAppShell(
    smsViewModel: RealSmsViewModel,
    launchRequest: PulseLaunchRequest? = null,
    openNewChatRequestKey: Int = 0,
    accessState: InboxAccessState = InboxAccessState(),
    onLaunchRequestConsumed: () -> Unit = {},
    onRequestNewChat: () -> Unit = {},
    onRequestSmsPermissions: () -> Unit = {},
    onOpenConversation: (String, Long?) -> Unit,
    onSendMessage: (String, String, List<AndroidUri>, Int?) -> Unit,
    themeViewModel: SerafinaThemeViewModel,
    onRequestDefaultSms: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var backStack by rememberSaveable { mutableStateOf(listOf(DESTINATION_INBOX)) }
    var activeAddress by rememberSaveable { mutableStateOf("") }
    var activeConversationTitle by rememberSaveable { mutableStateOf("") }
    var activeSubscriptionId by rememberSaveable { mutableStateOf<Int?>(null) }
    var conversationDraftSeed by rememberSaveable { mutableStateOf("") }
    var pendingForwardDraft by rememberSaveable { mutableStateOf<String?>(null) }
    var newChatQuery by rememberSaveable { mutableStateOf("") }
    var lastHandledNewChatRequestKey by rememberSaveable { mutableIntStateOf(0) }
    var consumedLaunchRequest by remember { mutableStateOf(false) }
    LaunchedEffect(launchRequest) {
        if (launchRequest != null) consumedLaunchRequest = false
    }
    var isAuthenticated by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val shellThemeState by themeViewModel.state.collectAsState()
    val currentScreen = backStack.lastOrNull() ?: DESTINATION_INBOX
    val isShowingInboxAccessGate = shouldShowInboxAccessGate(
        accessState = accessState,
        isLockScreen = currentScreen == DESTINATION_LOCK,
    )
    val renderedScreen = if (isShowingInboxAccessGate) DESTINATION_INBOX else currentScreen
    val reducedMotion = rememberReducedMotionEnabled()
    val inboxListState = rememberLazyListState()
    val inboxFilterState = rememberLazyListState()
    val archivedListState = rememberLazyListState()
    val settingsListState = rememberLazyListState()
    val blockedNumbersListState = rememberLazyListState()
    val newChatListState = rememberLazyListState()
    val scheduledMessagesListState = rememberLazyListState()

    LaunchedEffect(launchRequest, accessState, consumedLaunchRequest) {
        if (!accessState.isReady || consumedLaunchRequest) return@LaunchedEffect
        val request = launchRequest ?: return@LaunchedEffect
        consumedLaunchRequest = true
        val requestedAddress = request.conversationAddress
        conversationDraftSeed = request.draftBody
        if (requestedAddress.isNotBlank()) {
            activeAddress = requestedAddress
            activeConversationTitle = request.conversationTitle.ifBlank { displayNameFor(context, requestedAddress) }
            activeSubscriptionId = null
            NotificationManagerCompat.from(context).cancel(requestedAddress.hashCode() and 0x7fffffff)
            onOpenConversation(requestedAddress, null)
            backStack = listOf(DESTINATION_INBOX, DESTINATION_CONVERSATION)
        } else {
            activeAddress = ""
            activeConversationTitle = ""
            activeSubscriptionId = null
            backStack = listOf(DESTINATION_INBOX, DESTINATION_NEW_CHAT)
        }
        onLaunchRequestConsumed()
    }

    LaunchedEffect(openNewChatRequestKey, lastHandledNewChatRequestKey, accessState) {
        if (!shouldHandleOpenNewChatRequest(openNewChatRequestKey, lastHandledNewChatRequestKey, accessState)) {
            return@LaunchedEffect
        }
        backStack = listOf(DESTINATION_INBOX, DESTINATION_NEW_CHAT)
        lastHandledNewChatRequestKey = openNewChatRequestKey
    }

    // Trigger app lock when either configured unlock method requires authentication.
    LaunchedEffect(shellThemeState.fingerprintEnabled, shellThemeState.password, isAuthenticated) {
        val securityEnabled = shellThemeState.fingerprintEnabled || shellThemeState.password.isNotBlank()
        if (!securityEnabled) {
            isAuthenticated = false
            if (backStack.lastOrNull() == DESTINATION_LOCK) {
                backStack = listOf(DESTINATION_INBOX)
            }
            return@LaunchedEffect
        }
        if (!isAuthenticated) {
            backStack = listOf(DESTINATION_LOCK)
        }
    }

    fun navigateBack() {
        if (currentScreen == DESTINATION_CONVERSATION) {
            smsViewModel.closeConversation()
        }
        backStack = backStack.dropLast(1).ifEmpty { listOf(DESTINATION_INBOX) }
    }

    BackHandler(enabled = currentScreen != DESTINATION_INBOX && !isShowingInboxAccessGate) {
        if (currentScreen == DESTINATION_LOCK) {
            (context as? android.app.Activity)?.finish()
        } else {
            navigateBack()
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        AnimatedContent(
            targetState = renderedScreen,
            transitionSpec = {
                screenTransition(
                    initialState = initialState,
                    targetState = targetState,
                    reducedMotion = reducedMotion,
                )
            },
            label = "screen_transition",
        ) { screen ->
            when (screen) {
                DESTINATION_INBOX -> {
                    val inboxState by smsViewModel.inboxState.collectAsState()
                    if (!accessState.isReady) {
                        InboxOnboardingScreen(
                            accessState = accessState,
                            hasPendingLaunchRequest = launchRequest != null,
                            onRequestSmsPermissions = onRequestSmsPermissions,
                            onRequestDefaultSms = onRequestDefaultSms,
                        )
                    } else {
                        RealInboxScreen(
                            state = inboxState,
                            listState = inboxListState,
                            filterState = inboxFilterState,
                            onOpenConversation = { address, threadId ->
                                activeAddress = address
                                activeConversationTitle = displayNameFor(context, address)
                                activeSubscriptionId = null
                                conversationDraftSeed = ""
                                NotificationManagerCompat.from(context).cancel(address.hashCode() and 0x7fffffff)
                                onOpenConversation(address, threadId)
                                backStack = backStack + DESTINATION_CONVERSATION
                            },
                            onOpenArchivedChats = {
                                backStack = backStack + DESTINATION_ARCHIVED
                            },
                            onOpenSettings = {
                                backStack = backStack + DESTINATION_SETTINGS
                            },
                            onOpenNewChat = onRequestNewChat,
                            onRefreshInbox = smsViewModel::refreshInbox,
                            onTogglePinned = smsViewModel::toggleThreadPinned,
                            onSetPinned = smsViewModel::setThreadPinned,
                            onToggleArchived = smsViewModel::toggleThreadArchived,
                            onSetArchived = smsViewModel::setThreadArchived,
                            onSetThreadUnread = smsViewModel::setThreadUnread,
                            onBlockThread = smsViewModel::blockThread,
                            onSetThreadMuted = { address, muted ->
                                if (muted) smsViewModel.muteThread(address)
                                else smsViewModel.unmuteThread(address)
                            },
                            onDeleteThread = smsViewModel::deleteThread,
                        )
                    }
                }

                DESTINATION_NEW_CHAT -> {
                    val inboxState by smsViewModel.inboxState.collectAsState()
                    NewChatScreen(
                        threads = inboxState.threads,
                        listState = newChatListState,
                        query = newChatQuery,
                        onQueryChange = { newChatQuery = it },
                        onBack = {
                            navigateBack()
                        },
                        onStartConversation = { recipient, subscriptionId ->
                            activeAddress = recipient.address
                            activeConversationTitle = displayNameFor(context, recipient.address)
                            activeSubscriptionId = subscriptionId
                            conversationDraftSeed = pendingForwardDraft.orEmpty()
                            pendingForwardDraft = null
                            onOpenConversation(recipient.address, null)
                            backStack = backStack + DESTINATION_CONVERSATION
                        },
                    )
                }

                DESTINATION_CONVERSATION -> {
                    val conversationState by smsViewModel.conversationState.collectAsState()
                    val sendState by smsViewModel.sendState.collectAsState()
                    val inboxState by smsViewModel.inboxState.collectAsState()
                    LaunchedEffect(activeAddress, conversationState.messages.size) {
                        if (!conversationState.loading && activeAddress.isNotBlank()) {
                            NotificationManagerCompat.from(context).cancel(activeAddress.hashCode() and 0x7fffffff)
                        }
                    }
                    RealConversationScreen(
                        title = activeConversationTitle.ifBlank { displayNameFor(context, activeAddress) },
                        address = activeAddress,
                        initialDraft = conversationDraftSeed,
                        initialSubscriptionId = activeSubscriptionId,
                        messages = if (conversationState.address == activeAddress) {
                            conversationState.messages
                        } else {
                            emptyList()
                        },
                        loading = conversationState.address == activeAddress && conversationState.loading,
                        importantMessageIds = if (conversationState.address == activeAddress) {
                            conversationState.importantMessageIds
                        } else {
                            emptySet()
                        },
                        messageReactions = if (conversationState.address == activeAddress) {
                            conversationState.messageReactions
                        } else {
                            emptyMap()
                        },
                        unmatchedReactions = if (conversationState.address == activeAddress) {
                            conversationState.unmatchedReactions
                        } else {
                            emptyList()
                        },
                        isReplyable = conversationReplyabilityForActiveRoute(
                            activeAddress = activeAddress,
                            conversationState = conversationState,
                        ),
                        hasMoreMessages = conversationState.hasMoreMessages,
                        loadingMore = conversationState.loadingMore,
                        totalMessageCount = conversationState.totalMessageCount,
                        sendState = sendState,
                        scheduledMessages = conversationState.scheduledMessages,
                        onBack = {
                            navigateBack()
                        },
                        onSubscriptionIdChange = { activeSubscriptionId = it },
                        onSend = { body, imageUris ->
                            onSendMessage(activeAddress, body, imageUris, activeSubscriptionId)
                        },
                        onSendVoice = { uri ->
                            smsViewModel.sendVoiceMessage(activeAddress, uri)
                        },
                        onRetrySend = smsViewModel::retrySend,
                        onClearSendState = smsViewModel::clearSendState,
                        onDraftConsumed = { conversationDraftSeed = "" },
                        onDraftChange = { text -> smsViewModel.saveDraft(activeAddress, text) },
                        onScheduleMessage = { body, time ->
                            smsViewModel.scheduleMessage(activeAddress, body, time, activeSubscriptionId)
                        },
                        onCancelScheduledMessage = smsViewModel::cancelScheduledMessage,
                        onDeleteMessage = smsViewModel::deleteMessage,
                        onDeleteMessages = smsViewModel::deleteMessages,
                        onDeleteConversation = {
                            smsViewModel.deleteThread(null, activeAddress)
                            smsViewModel.closeConversation()
                            backStack = listOf(DESTINATION_INBOX)
                        },
                        onMuteConversation = {
                            val mutedKey = activeAddress.toBlockedSenderKeyOrNull()
                            val isCurrentlyMuted = mutedKey != null &&
                                inboxState.mutedAddresses.any { it.matchesBlockedSenderKey(mutedKey) }
                            if (isCurrentlyMuted) {
                                smsViewModel.unmuteThread(activeAddress)
                            } else {
                                smsViewModel.muteThread(activeAddress)
                            }
                        },
                        isMuted = inboxState.mutedAddresses.let { muted ->
                            val mutedKey = activeAddress.toBlockedSenderKeyOrNull()
                            mutedKey != null && muted.any { it.matchesBlockedSenderKey(mutedKey) }
                        },
                        onBlockConversation = {
                            smsViewModel.blockThread(activeAddress)
                            smsViewModel.closeConversation()
                            backStack = listOf(DESTINATION_INBOX)
                        },
                        onForwardMessage = { body ->
                            pendingForwardDraft = body
                            newChatQuery = ""
                            backStack = listOf(DESTINATION_INBOX, DESTINATION_NEW_CHAT)
                        },
                        onCallAddress = {
                            openDialer(context, activeAddress)
                        },
                        onLoadMoreMessages = smsViewModel::loadMoreMessages,
                        onSetMessageReaction = smsViewModel::setMessageReaction,
                    )
                }

                DESTINATION_SETTINGS -> {
                    val inboxState by smsViewModel.inboxState.collectAsState()
                    SettingsScreen(
                        themeViewModel = themeViewModel,
                        listState = settingsListState,
                        archivedCount = inboxState.archivedThreads.size,
                        blockedCount = inboxState.blockedAddresses.size,
                        onBack = {
                            backStack = backStack.dropLast(1).ifEmpty { listOf(DESTINATION_INBOX) }
                        },
                        onRequestDefaultSms = onRequestDefaultSms,
                        onOpenArchivedChats = {
                            backStack = backStack + DESTINATION_ARCHIVED
                        },
                        onOpenSecurity = {
                            backStack = backStack + DESTINATION_SECURITY
                        },
                        onOpenBlockedNumbers = {
                            backStack = backStack + DESTINATION_BLOCKED_NUMBERS
                        },
                        onExportBackup = { uri -> smsViewModel.exportBackupToUri(uri) },
                        onImportBackup = { uri -> smsViewModel.importBackupFromUri(uri) },
                        isDefaultSmsApp = inboxState.isDefaultSmsApp,
                    )
                }

                DESTINATION_ARCHIVED -> {
                    val inboxState by smsViewModel.inboxState.collectAsState()
                    ArchivedChatsScreen(
                        drafts = inboxState.drafts,
                        scheduledAddresses = inboxState.scheduledAddresses,
                        threads = inboxState.archivedThreads,
                        pinnedThreadIds = inboxState.pinnedThreadIds,
                        archivedThreadIds = inboxState.archivedThreadIds,
                        loading = inboxState.loading,
                        errorMessage = inboxState.errorMessage,
                        listState = archivedListState,
                        onBack = {
                            backStack = backStack.dropLast(1).ifEmpty { listOf(DESTINATION_INBOX) }
                        },
                        onOpenConversation = { address, threadId ->
                            activeAddress = address
                            activeConversationTitle = displayNameFor(context, address)
                            activeSubscriptionId = null
                            conversationDraftSeed = ""
                            onOpenConversation(address, threadId)
                            backStack = backStack + DESTINATION_CONVERSATION
                        },
                        onRefreshInbox = smsViewModel::refreshInbox,
                        onTogglePinned = smsViewModel::toggleThreadPinned,
                        onSetPinned = smsViewModel::setThreadPinned,
                        onToggleArchived = smsViewModel::toggleThreadArchived,
                        onSetArchived = smsViewModel::setThreadArchived,
                        onSetThreadUnread = smsViewModel::setThreadUnread,
                        onBlockThread = smsViewModel::blockThread,
                        onDeleteThread = smsViewModel::deleteThread,
                    )
                }

                DESTINATION_SECURITY -> {
                    SecuritySettingsScreen(
                        themeViewModel = themeViewModel,
                        onBack = {
                            backStack = backStack.dropLast(1).ifEmpty { listOf(DESTINATION_INBOX) }
                        },
                    )
                }

                DESTINATION_BLOCKED_NUMBERS -> {
                    val inboxState by smsViewModel.inboxState.collectAsState()
                    BlockedNumbersScreen(
                        blockedAddresses = inboxState.blockedAddresses,
                        listState = blockedNumbersListState,
                        onBack = {
                            backStack = backStack.dropLast(1).ifEmpty { listOf(DESTINATION_INBOX) }
                        },
                        onUnblock = smsViewModel::unblockThread,
                    )
                }

                DESTINATION_LOCK -> {
                    LockScreen(
                        biometricEnabled = shellThemeState.fingerprintEnabled,
                        biometricAvailability = checkBiometricAvailability(context),
                        passwordVerifierToken = shellThemeState.password,
                        onAuthenticated = {
                            isAuthenticated = true
                            backStack = listOf(DESTINATION_INBOX)
                        },
                        onCancel = {
                            // Stay on lock screen — user must authenticate
                        },
                    )
                }
            }
        }
    }
}

private fun openDialer(context: Context, address: String) {
    val dialableNumber = address.toDialablePhoneNumberOrNull() ?: return
    val dialIntent = Intent(Intent.ACTION_DIAL, AndroidUri.fromParts("tel", dialableNumber, null))
    try {
        context.startActivity(dialIntent)
    } catch (exception: ActivityNotFoundException) {
        Toast.makeText(context, context.getString(R.string.no_call_app_available), Toast.LENGTH_SHORT).show()
    }
}

// ═══════════════════════════════════════════════════════════
// REAL SMS INBOX
// ═══════════════════════════════════════════════════════════
