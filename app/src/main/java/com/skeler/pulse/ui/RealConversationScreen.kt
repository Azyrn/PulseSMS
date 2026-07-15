package com.skeler.pulse.ui

import android.content.ClipData
import android.content.ClipboardManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skeler.pulse.R
import com.skeler.pulse.design.util.elasticOverscroll
import com.skeler.pulse.design.util.isNearListEnd
import com.skeler.pulse.design.util.rememberReducedMotionEnabled
import com.skeler.pulse.design.util.rememberSmoothFlingBehavior
import com.skeler.pulse.design.util.scrollToItemSmoothly
import com.skeler.pulse.sms.SystemSms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RealConversationScreen(
    title: String,
    address: String,
    initialDraft: String,
    initialSubscriptionId: Int?,
    messages: List<SystemSms>,
    loading: Boolean,
    importantMessageIds: Set<Long>,
    isReplyable: Boolean,
    sendState: SendState,
    onBack: () -> Unit,
    onSubscriptionIdChange: (Int?) -> Unit,
    onSend: (String) -> Unit,
    onRetrySend: () -> Unit,
    onClearSendState: () -> Unit,
    onDraftConsumed: () -> Unit,
    onDeleteMessage: (Long) -> Unit,
    onBlockConversation: () -> Unit,
    onForwardMessage: (String) -> Unit,
    onCallAddress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    val reducedMotion = rememberReducedMotionEnabled()
    val listFlingBehavior = rememberSmoothFlingBehavior(enabled = !reducedMotion)
    var draft by rememberSaveable(address) { mutableStateOf("") }
    var shouldShowDiscardDraftDialog by rememberSaveable(address) { mutableStateOf(false) }
    var previousMessageCount by remember(address) { mutableIntStateOf(0) }
    val fallbackSimSlotLabel = stringResource(R.string.conversation_sim_default_slot)
    val fallbackSimCarrierLabel = stringResource(R.string.conversation_sim_default_carrier)
    val fallbackSimOption = remember(fallbackSimSlotLabel, fallbackSimCarrierLabel) {
        NewChatSimOption(
            key = "sim_default",
            subscriptionId = null,
            slotLabel = fallbackSimSlotLabel,
            carrierLabel = fallbackSimCarrierLabel,
        )
    }
    val simOptions by produceState(
        initialValue = emptyList<NewChatSimOption>(),
    ) {
        value = withContext(Dispatchers.IO) {
            loadSimOptions(context)
        }
    }
    val availableSimOptions = remember(simOptions, fallbackSimOption) {
        if (simOptions.isEmpty()) listOf(fallbackSimOption) else simOptions
    }
    var selectedSimKey by rememberSaveable(address) { mutableStateOf<String?>(null) }

    LaunchedEffect(address, initialSubscriptionId, availableSimOptions) {
        val matchingOption = availableSimOptions.firstOrNull { it.subscriptionId == initialSubscriptionId }
        selectedSimKey = when {
            matchingOption != null -> matchingOption.key
            availableSimOptions.any { it.key == selectedSimKey } -> selectedSimKey
            else -> availableSimOptions.firstOrNull()?.key
        }
    }

    val selectedSim = remember(availableSimOptions, selectedSimKey) {
        availableSimOptions.firstOrNull { it.key == selectedSimKey } ?: availableSimOptions.firstOrNull()
    }

    LaunchedEffect(selectedSim?.subscriptionId) {
        onSubscriptionIdChange(selectedSim?.subscriptionId)
    }

    LaunchedEffect(address, initialDraft) {
        if (initialDraft.isNotBlank()) {
            draft = initialDraft
            onDraftConsumed()
        }
    }

    LaunchedEffect(address) {
        onClearSendState()
    }

    LaunchedEffect(sendState) {
        val nextDraft = draftAfterSendState(draft, sendState)
        if (draft != nextDraft) {
            draft = nextDraft
        }
        when (sendState) {
            is SendState.Sent -> {
                delay(1200)
                onClearSendState()
            }
            else -> Unit
        }
    }

    val timelineItems = remember(messages) { messages.toConversationTimeline() }
    val unreadCount = remember(messages) { messages.count { it.isInbound && !it.read } }
    val importantCount = remember(messages, importantMessageIds) {
        messages.count { it.id in importantMessageIds }
    }
    var contextMenuMessageId by rememberSaveable(address) { mutableStateOf<Long?>(null) }
    val contextMenuMessage = remember(messages, contextMenuMessageId) {
        messages.firstOrNull { it.id == contextMenuMessageId }
    }
    val clipboardMessageLabel = stringResource(R.string.conversation_clipboard_message_label)
    val clipboardCodeLabel = stringResource(R.string.conversation_clipboard_code_label)
    val clipboardManager = remember(context) {
        context.getSystemService(ClipboardManager::class.java)
    }
    val isNearEnd by remember(listState) {
        derivedStateOf { listState.isNearListEnd() }
    }
    var hasPositionedInitialMessages by remember(address) { mutableStateOf(false) }

    LaunchedEffect(address) {
        previousMessageCount = 0
        hasPositionedInitialMessages = false
    }

    fun requestBackNavigation() {
        if (shouldConfirmDiscardDraft(draft)) {
            shouldShowDiscardDraftDialog = true
            return
        }
        onBack()
    }

    BackHandler {
        if (shouldShowDiscardDraftDialog) {
            shouldShowDiscardDraftDialog = false
        } else {
            requestBackNavigation()
        }
    }

    LaunchedEffect(address, loading, timelineItems.size) {
        if (!loading && !hasPositionedInitialMessages && timelineItems.isNotEmpty()) {
            listState.scrollToItem(conversationTimelineLazyListIndex(timelineItems.lastIndex))
            previousMessageCount = messages.size
            hasPositionedInitialMessages = true
        }
    }

    LaunchedEffect(messages.size, timelineItems.size) {
        if (timelineItems.isEmpty()) {
            previousMessageCount = 0
            return@LaunchedEffect
        }

        val listGrew = messages.size > previousMessageCount
        if (hasPositionedInitialMessages && listGrew && isNearEnd) {
            listState.scrollToItemSmoothly(conversationTimelineLazyListIndex(timelineItems.lastIndex))
        }
        previousMessageCount = messages.size
    }

    // ── Keyboard auto-scroll: when IME opens, scroll to latest message ──
    val isKeyboardVisible = WindowInsets.isImeVisible
    LaunchedEffect(isKeyboardVisible, timelineItems.size) {
        if (isKeyboardVisible && timelineItems.isNotEmpty()) {
            listState.scrollToItemSmoothly(conversationTimelineLazyListIndex(timelineItems.lastIndex))
        }
    }

    val conversationBackdropBrush = conversationBackdropBrush()
    val conversationAvatarColors = MaterialTheme.colorScheme.conversationAvatarColors(title)

    if (shouldShowDiscardDraftDialog) {
        AlertDialog(
            onDismissRequest = { shouldShowDiscardDraftDialog = false },
            title = { Text("Discard draft?") },
            text = { Text("You have an unsent message. Leave this conversation and discard it?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        shouldShowDiscardDraftDialog = false
                        onBack()
                    },
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { shouldShowDiscardDraftDialog = false }) {
                    Text("Keep editing")
                }
            },
        )
    }

    Scaffold(
        modifier = modifier
            .background(conversationBackdropBrush),
        containerColor = Color.Transparent,
        topBar = {
            ConversationTopBar(
                title = title,
                address = address,
                messages = messages,
                unreadCount = unreadCount,
                importantCount = importantCount,
                avatarColors = conversationAvatarColors,
                onBack = ::requestBackNavigation,
                onCallAddress = onCallAddress,
            )
        },
        bottomBar = {
            ConversationBottomBar(
                isReplyable = isReplyable,
                draft = draft,
                sendState = sendState,
                simOptions = availableSimOptions,
                selectedSimKey = selectedSim?.key,
                onRetrySend = onRetrySend,
                onSimOptionClick = { option -> selectedSimKey = option.key },
                onDraftChange = {
                    draft = it
                    if (sendState is SendState.Failed || sendState is SendState.Sent) {
                        onClearSendState()
                    }
                },
                onSend = {
                    val message = draft.trim()
                    if (message.isEmpty()) return@ConversationBottomBar
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onSend(message)
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(conversationBackdropBrush),
        ) {
            LazyColumn(
                state = listState,
                flingBehavior = listFlingBehavior,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .elasticOverscroll(
                        enabled = !reducedMotion,
                        state = listState,
                ),
                contentPadding = PaddingValues(
                    horizontal = ConversationVisualTokens.timelineHorizontalPadding,
                    vertical = ConversationVisualTokens.timelineVerticalPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                conversationTimelineItems(
                    title = title,
                    address = address,
                    messages = messages,
                    unreadCount = unreadCount,
                    importantCount = importantCount,
                    latestTimestamp = messages.lastOrNull()?.timestamp,
                    avatarColors = conversationAvatarColors,
                    loading = loading,
                    timelineItems = timelineItems,
                    importantMessageIds = importantMessageIds,
                    contextMenuMessageId = contextMenuMessageId,
                    reducedMotion = reducedMotion,
                    onToggleContextMenu = { messageId ->
                        contextMenuMessageId = if (contextMenuMessageId == messageId) null else messageId
                    },
                    onDismissMenu = { contextMenuMessageId = null },
                    onCopy = { message ->
                        clipboardManager?.setPrimaryClip(
                            ClipData.newPlainText(clipboardMessageLabel, message.body)
                        )
                        contextMenuMessageId = null
                    },
                    onCopyCode = { code ->
                        clipboardManager?.setPrimaryClip(ClipData.newPlainText(clipboardCodeLabel, code))
                        contextMenuMessageId = null
                    },
                    onDelete = { message ->
                        onDeleteMessage(message.id)
                        contextMenuMessageId = null
                    },
                    onBlock = {
                        onBlockConversation()
                        contextMenuMessageId = null
                    },
                    onForward = { message ->
                        onForwardMessage(message.body)
                        contextMenuMessageId = null
                    },
                )
            }
        }

    }
}
