package com.skeler.pulse.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.ui.text.input.KeyboardType
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skeler.pulse.R
import com.skeler.pulse.design.component.StatusPill
import com.skeler.pulse.design.util.elasticOverscroll
import com.skeler.pulse.design.util.rememberReducedMotionEnabled
import com.skeler.pulse.design.util.rememberSmoothFlingBehavior
import com.skeler.pulse.design.util.scrollToItemSmoothly
import com.skeler.pulse.sms.ScheduledMessageEntity
import com.skeler.pulse.sms.SystemSms
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun RealConversationScreen(
    title: String,
    address: String,
    initialDraft: String,
    initialSubscriptionId: Int?,
    messages: List<SystemSms>,
    loading: Boolean,
    importantMessageIds: Set<Long>,
    messageReactions: Map<Long, String>,
    unmatchedReactions: List<UnmatchedReaction>,
    isReplyable: Boolean,
    sendState: SendState,
    hasMoreMessages: Boolean = false,
    loadingMore: Boolean = false,
    totalMessageCount: Int = 0,
    scheduledMessages: List<ScheduledMessageEntity> = emptyList(),
    onBack: () -> Unit,
    onSubscriptionIdChange: (Int?) -> Unit,
    onSend: (String, List<Uri>) -> Unit,
    onSendVoice: (Uri) -> Unit = {},
    onRetrySend: () -> Unit,
    onClearSendState: () -> Unit,
    onDraftConsumed: () -> Unit,
    onDraftChange: (String) -> Unit = {},
    onScheduleMessage: (String, Long) -> Unit = { _, _ -> },
    onCancelScheduledMessage: (Long) -> Unit = {},
    onDeleteMessage: (Long) -> Unit,
    onDeleteMessages: (List<SystemSms>) -> Unit,
    onBlockConversation: () -> Unit,
    onForwardMessage: (String) -> Unit,
    onCallAddress: () -> Unit,
    onLoadMoreMessages: () -> Unit = {},
    onSetMessageReaction: (Long, String?) -> Unit,
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
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var searchQuery by rememberSaveable(address) { mutableStateOf("") }
    var isSearching by rememberSaveable(address) { mutableStateOf(false) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
    ) { uris -> selectedImageUris = selectedImageUris + uris }
    val cameraImageUri = remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success && cameraImageUri.value != null) {
            selectedImageUris = selectedImageUris + cameraImageUri.value!!
        }
        cameraImageUri.value = null
    }
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
            is SendState.Sending -> {
                delay(100.milliseconds)
                listState.scrollToItemSmoothly(0)
            }
            is SendState.Sent -> {
                delay(200.milliseconds)
                listState.scrollToItemSmoothly(0)
                delay(1000.milliseconds)
                onClearSendState()
            }
            is SendState.Failed -> {
                android.widget.Toast
                    .makeText(context, R.string.conversation_send_status_failed_title, android.widget.Toast.LENGTH_SHORT)
                    .show()
            }
            else -> Unit
        }
    }

    val filteredMessages = remember(messages, searchQuery) {
        if (searchQuery.isBlank()) {
            messages
        } else {
            val query = searchQuery.trim().lowercase()
            messages.filter { it.body.lowercase().contains(query) }
        }
    }
    val searchMatchCount = remember(messages, searchQuery) {
        if (searchQuery.isBlank()) 0
        else messages.count { it.body.lowercase().contains(searchQuery.trim().lowercase()) }
    }

    val timelineItems = remember(filteredMessages, unmatchedReactions) {
        buildConversationTimeline(
            messages = filteredMessages,
            unmatchedReactions = unmatchedReactions,
            unreadMessagesFormatter = { count ->
                context.resources.getQuantityString(R.plurals.conversation_unread_messages, count, count)
            },
            todayLabel = context.getString(R.string.conversation_today),
            yesterdayLabel = context.getString(R.string.conversation_yesterday),
        )
    }
    val unreadCount = remember(messages) { messages.count { it.isInbound && !it.read } }
    val importantCount = remember(messages, importantMessageIds) {
        messages.count { it.id in importantMessageIds }
    }
    var selectedMessages by remember { mutableStateOf<Set<SystemSms>>(emptySet()) }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }
    var infoSheetMessage by remember { mutableStateOf<SystemSms?>(null) }
    var reactionPickerMessageId by remember { mutableStateOf<Long?>(null) }
    val clipboardMessageLabel = stringResource(R.string.conversation_clipboard_message_label)
    val clipboardCodeLabel = stringResource(R.string.conversation_clipboard_code_label)
    val clipboardManager = remember(context) {
        context.getSystemService(ClipboardManager::class.java)
    }
    val isNearEnd by remember(listState) {
        derivedStateOf {
            val info = listState.layoutInfo
            if (info.totalItemsCount == 0) return@derivedStateOf true
            val firstVisibleIndex = info.visibleItemsInfo.firstOrNull()?.index ?: return@derivedStateOf true
            firstVisibleIndex <= 2
        }
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
        if (isSearching) {
            isSearching = false
            searchQuery = ""
            return
        }
        onDraftChange(draft)
        onBack()
    }

    BackHandler {
        if (showAttachmentMenu) {
            showAttachmentMenu = false
        } else if (selectedMessages.isNotEmpty()) {
            selectedMessages = emptySet()
        } else if (shouldShowDiscardDraftDialog) {
            shouldShowDiscardDraftDialog = false
        } else if (isSearching) {
            isSearching = false
            searchQuery = ""
        } else {
            requestBackNavigation()
        }
    }

    LaunchedEffect(address, loading, timelineItems.size) {
        if (!loading && !hasPositionedInitialMessages && timelineItems.isNotEmpty()) {
            listState.scrollToItem(0)
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
            listState.scrollToItemSmoothly(0)
        }
        previousMessageCount = messages.size
    }

    val isKeyboardVisible = WindowInsets.isImeVisible
    LaunchedEffect(isKeyboardVisible, timelineItems.size) {
        if (isKeyboardVisible && timelineItems.isNotEmpty()) {
            listState.scrollToItemSmoothly(0)
        }
    }

    val conversationBackdropBrush = conversationBackdropBrush()
    val conversationAvatarColors = MaterialTheme.colorScheme.conversationAvatarColors(title)

    if (shouldShowDiscardDraftDialog) {
        AlertDialog(
            onDismissRequest = { shouldShowDiscardDraftDialog = false },
            title = { Text(stringResource(R.string.conversation_discard_draft_title)) },
            text = { Text(stringResource(R.string.conversation_discard_draft_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        shouldShowDiscardDraftDialog = false
                        onBack()
                    },
                ) {
                    Text(stringResource(R.string.conversation_discard_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { shouldShowDiscardDraftDialog = false }) {
                    Text(stringResource(R.string.conversation_discard_cancel))
                }
            },
        )
    }

    if (showDeleteSelectedDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSelectedDialog = false },
            title = { Text(stringResource(R.string.conversation_delete_title)) },
            text = { Text(pluralStringResource(R.plurals.conversation_delete_body, selectedMessages.size, selectedMessages.size)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteMessages(selectedMessages.toList())
                        selectedMessages = emptySet()
                        showDeleteSelectedDialog = false
                    },
                ) {
                    Text(stringResource(R.string.thread_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showScheduleDialog) {
        ScheduleMessageDialog(
            onDismiss = { showScheduleDialog = false },
            onConfirm = { scheduledAtMillis ->
                if (draft.isNotBlank()) {
                    onScheduleMessage(draft.trim(), scheduledAtMillis)
                    draft = ""
                    onDraftChange("")
                }
                showScheduleDialog = false
            },
        )
    }

    Scaffold(
        modifier = modifier
            .background(conversationBackdropBrush),
        containerColor = Color.Transparent,
        topBar = {
            if (selectedMessages.isNotEmpty()) {
                ConversationSelectionTopBar(
                    selectedCount = selectedMessages.size,
                    onClose = { selectedMessages = emptySet() },
                    onCopy = {
                        val selectedTextMessages = messages.filter { it in selectedMessages }
                        val text = selectedTextMessages.joinToString("\n\n") { it.body }
                        if (text.isNotEmpty()) {
                            @Suppress("UseOfSetterInsteadOfPropertyAccess")
                            clipboardManager?.setPrimaryClip(
                                ClipData.newPlainText(clipboardMessageLabel, text)
                            )
                        }
                    },
                    onDelete = { showDeleteSelectedDialog = true },
                    onInfo = { infoSheetMessage = selectedMessages.firstOrNull() },
                )
            } else {
                ConversationTopBar(
                    title = title,
                    address = address,
                    messages = messages,
                    unreadCount = unreadCount,
                    importantCount = importantCount,
                    totalMessageCount = totalMessageCount,
                    avatarColors = conversationAvatarColors,
                    isSearching = isSearching,
                    searchQuery = searchQuery,
                    searchMatchCount = searchMatchCount,
                    onBack = ::requestBackNavigation,
                    onCallAddress = onCallAddress,
                    onSearchQueryChange = { searchQuery = it },
                    onSearchToggle = { isSearching = !isSearching },
                )
            }
        },
        bottomBar = {
            if (isSearching) {
                ConversationSearchBottomBar(
                    matchCount = searchMatchCount,
                    onClose = {
                        isSearching = false
                        searchQuery = ""
                    },
                )
            } else {
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
                        onDraftChange(it)
                        if (sendState is SendState.Failed || sendState is SendState.Sent) {
                            onClearSendState()
                        }
                    },
                    selectedImageUris = selectedImageUris,
                    onImageSelected = { selectedImageUris = it },
                    onImagePickFromGallery = {
                        imagePickerLauncher.launch("image/*")
                    },
                    onTakePhoto = {
                        val photoFile = createImageFile(context)
                        val photoUri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.mmsfileprovider",
                            photoFile,
                        )
                        cameraImageUri.value = photoUri
                        cameraLauncher.launch(photoUri)
                    },
                    onSend = {
                        val message = draft.trim()
                        if (message.isEmpty() && selectedImageUris.isEmpty()) return@ConversationBottomBar
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onDraftChange("")
                        onSend(message, selectedImageUris)
                        selectedImageUris = emptyList()
                    },
                    onVoiceRecorded = { uri ->
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onSendVoice(uri)
                    },
                    showAttachmentMenu = showAttachmentMenu,
                    onAttachmentMenuVisibilityChange = { showAttachmentMenu = it },
                    onScheduleClick = { showScheduleDialog = true },
                    canSchedule = draft.isNotBlank() && !isSearching,
                )
            }
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
                reverseLayout = true,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .elasticOverscroll(
                        enabled = !reducedMotion,
                        state = listState,
                        reverseLayout = true,
                ),
                contentPadding = PaddingValues(
                    horizontal = ConversationVisualTokens.timelineHorizontalPadding,
                    vertical = ConversationVisualTokens.timelineVerticalPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (scheduledMessages.isNotEmpty() && !isSearching) {
                    items(
                        items = scheduledMessages,
                        key = { "scheduled_${it.id}" },
                        contentType = { "scheduled_message" },
                    ) { msg ->
                        ScheduledMessageBubble(
                            message = msg,
                            scheduledMessages = scheduledMessages,
                            onCancel = onCancelScheduledMessage,
                        )
                    }
                }
                conversationTimelineItems(
                    title = title,
                    address = address,
                    messages = filteredMessages,
                    unreadCount = unreadCount,
                    importantCount = importantCount,
                    latestTimestamp = messages.lastOrNull()?.timestamp,
                    avatarColors = conversationAvatarColors,
                    loading = loading,
                    timelineItems = timelineItems,
                    importantMessageIds = importantMessageIds,
                    messageReactions = messageReactions,
                    selectedMessages = selectedMessages,
                    reducedMotion = reducedMotion,
                    hasMoreMessages = hasMoreMessages,
                    loadingMore = loadingMore,
                    searchQuery = searchQuery,
                    onLoadMoreMessages = onLoadMoreMessages,
                    onCopyCode = { code ->
                        @Suppress("UseOfSetterInsteadOfPropertyAccess")
                        clipboardManager?.setPrimaryClip(ClipData.newPlainText(clipboardCodeLabel, code))
                    },
                    onToggleMessageSelection = { message ->
                        selectedMessages = if (message in selectedMessages) {
                            selectedMessages - message
                        } else {
                            selectedMessages + message
                        }
                    },
                    onMessageEmojiClick = { messageId ->
                        reactionPickerMessageId = messageId
                    },
                )
            }
        }

    MessageInfoSheet(
        message = infoSheetMessage,
        onDismiss = { infoSheetMessage = null },
    )

    val reactionMessageId = reactionPickerMessageId
    if (reactionMessageId != null) {
        InboxEmojiPickerSheet(
            currentEmoji = messageReactions[reactionMessageId],
            onEmojiSelected = { emoji ->
                onSetMessageReaction(reactionMessageId, emoji)
            },
            onDismiss = { reactionPickerMessageId = null },
        )
    }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleMessageDialog(
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis() + 3600000L,
    )
    val initialCalendar = remember { Calendar.getInstance().apply { add(Calendar.MINUTE, 15) } }
    var hour by remember { mutableStateOf(initialCalendar.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableStateOf(initialCalendar.get(Calendar.MINUTE)) }
    val timezoneId = remember { TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT) }
    val isPastTime by remember {
        derivedStateOf {
            val dateMillis = datePickerState.selectedDateMillis ?: return@derivedStateOf true
            val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
            cal.set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            cal.set(Calendar.MINUTE, minute.coerceIn(0, 59))
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis <= System.currentTimeMillis()
        }
    }
    val dateFormat = remember { SimpleDateFormat("EEE d MMM", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val displayDate by remember {
        derivedStateOf {
            val dateMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
            dateFormat.format(Date(dateMillis))
        }
    }
    val displayTime by remember {
        derivedStateOf {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            cal.set(Calendar.MINUTE, minute.coerceIn(0, 59))
            timeFormat.format(cal.time)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = !isPastTime,
                onClick = {
                    val dateMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
                    cal.set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
                    cal.set(Calendar.MINUTE, minute.coerceIn(0, 59))
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    onConfirm(cal.timeInMillis)
                },
            ) {
                Text(stringResource(R.string.conversation_schedule_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        title = { Text(stringResource(R.string.conversation_schedule_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = if (hour <= 9) "0$hour" else hour.toString(),
                        onValueChange = { text ->
                            val filtered = text.filter { it.isDigit() }
                            val value = filtered.take(2).toIntOrNull() ?: 0
                            hour = value.coerceAtMost(23)
                        },
                        label = { Text("HH") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = if (minute <= 9) "0$minute" else minute.toString(),
                        onValueChange = { text ->
                            val filtered = text.filter { it.isDigit() }
                            val value = filtered.take(2).toIntOrNull() ?: 0
                            minute = value.coerceAtMost(59)
                        },
                        label = { Text("MM") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        text = "${stringResource(R.string.conversation_schedule_date)}: $displayDate · $displayTime $timezoneId",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
                if (isPastTime) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.conversation_schedule_past_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Spacer(Modifier.height(12.dp))
                DatePicker(state = datePickerState)
            }
        },
    )
}

@Composable
private fun ConversationSearchBottomBar(
    matchCount: Int,
    onClose: () -> Unit,
) {
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = if (matchCount > 0) {
                    stringResource(R.string.conversation_search_results, matchCount)
                } else {
                    stringResource(R.string.conversation_search_no_results)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ScheduledMessageBubble(
    message: ScheduledMessageEntity,
    scheduledMessages: List<ScheduledMessageEntity>,
    onCancel: (Long) -> Unit,
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 24.dp,
                topEnd = 24.dp,
                bottomStart = 24.dp,
                bottomEnd = 10.dp,
            ),
            color = MaterialTheme.colorScheme.tertiaryContainer,
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                    )
                    Text(
                        text = dateFormat.format(Date(message.scheduledAtMillis)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                    )
                    StatusPill(
                        label = stringResource(R.string.scheduled_messages_snippet),
                        containerColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
        }
    }
}

private fun createImageFile(context: android.content.Context): java.io.File {
    val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
    val imageDir = java.io.File(context.cacheDir, "camera_photos")
    imageDir.mkdirs()
    return java.io.File(imageDir, "MMS_$timeStamp.jpg")
}
