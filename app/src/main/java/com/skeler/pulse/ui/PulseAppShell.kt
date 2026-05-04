@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)

package com.skeler.pulse.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import com.skeler.pulse.InboxAccessState
import com.skeler.pulse.PulseLaunchRequest
import com.skeler.pulse.contact.displayNameFor
import com.skeler.pulse.shouldHandleLaunchRequest
import com.skeler.pulse.shouldHandleOpenNewChatRequest

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.MarkunreadMailbox
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skeler.pulse.MainActivity
import com.skeler.pulse.design.component.SerafinaAvatar
import com.skeler.pulse.design.component.SerafinaProgressIndicator
import com.skeler.pulse.design.component.StatusPill
import com.skeler.pulse.design.theme.SerafinaPalette
import com.skeler.pulse.design.theme.SerafinaThemeMode
import com.skeler.pulse.design.theme.SerafinaThemeViewModel
import com.skeler.pulse.design.util.elasticOverscroll
import com.skeler.pulse.design.util.isNearListEnd
import com.skeler.pulse.design.util.motionAnimateItemModifier
import com.skeler.pulse.design.util.rememberEntranceModifier
import com.skeler.pulse.design.util.rememberMomentumFlingBehavior
import com.skeler.pulse.design.util.rememberReducedMotionEnabled
import com.skeler.pulse.design.util.rememberSmoothFlingBehavior
import com.skeler.pulse.design.util.scrollToItemSmoothly
import com.skeler.pulse.sms.SmsThread
import com.skeler.pulse.sms.SystemSms
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.skeler.pulse.security.auth.BiometricAvailability
import com.skeler.pulse.security.auth.BiometricAuthResult
import com.skeler.pulse.security.auth.checkBiometricAvailability
import com.skeler.pulse.security.auth.showBiometricPrompt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val DESTINATION_INBOX = "inbox"
private const val DESTINATION_NEW_CHAT = "new_chat"
private const val DESTINATION_CONVERSATION = "conversation"
private const val DESTINATION_SETTINGS = "settings"
private const val DESTINATION_ARCHIVED = "archived"
private const val DESTINATION_SECURITY = "security"
private const val DESTINATION_LOCK = "lock"
private const val SCREEN_TRANSITION_DURATION_MILLIS = 180
private const val SCREEN_TRANSITION_EXIT_DURATION_MILLIS = 120

private enum class InboxFilter(val label: String) {
    All("All"), Personal("Personal"), Business("Business"), OTP("OTP"),
}

private data class SettingsChoiceOption(
    val id: String,
    val label: String,
    val accentColor: Color? = null,
)

private fun screenDepth(destination: String): Int = when (destination) {
    DESTINATION_INBOX -> 0
    DESTINATION_NEW_CHAT,
    DESTINATION_SETTINGS,
    DESTINATION_ARCHIVED -> 1
    DESTINATION_CONVERSATION,
    DESTINATION_SECURITY,
    DESTINATION_LOCK -> 2
    else -> 0
}

private fun screenTransition(
    initialState: String,
    targetState: String,
    reducedMotion: Boolean,
): ContentTransform {
    if (reducedMotion) {
        return ContentTransform(
            targetContentEnter = fadeIn(tween(durationMillis = 0)),
            initialContentExit = fadeOut(tween(durationMillis = 0)),
            sizeTransform = null,
        )
    }

    val forward = screenDepth(targetState) >= screenDepth(initialState)
    val enterOffset: (Int) -> Int = { width -> if (forward) width / 5 else -width / 5 }
    val exitOffset: (Int) -> Int = { width -> if (forward) -width / 8 else width / 8 }

    val enterTransition =
        slideInHorizontally(
            animationSpec = tween(
                durationMillis = SCREEN_TRANSITION_DURATION_MILLIS,
                easing = FastOutSlowInEasing,
            ),
            initialOffsetX = enterOffset,
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = SCREEN_TRANSITION_EXIT_DURATION_MILLIS,
                easing = FastOutSlowInEasing,
            ),
        )

    val exitTransition =
        slideOutHorizontally(
            animationSpec = tween(
                durationMillis = SCREEN_TRANSITION_EXIT_DURATION_MILLIS,
                easing = LinearOutSlowInEasing,
            ),
            targetOffsetX = exitOffset,
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = SCREEN_TRANSITION_EXIT_DURATION_MILLIS,
                easing = LinearOutSlowInEasing,
            ),
        )

    return ContentTransform(
        targetContentEnter = enterTransition,
        initialContentExit = exitTransition,
        sizeTransform = null,
    )
}

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
    onSendMessage: (String, String, Int?) -> Unit,
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
    var consumedLaunchRequest by rememberSaveable { mutableStateOf(false) }
    var isAuthenticated by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val shellThemeState by themeViewModel.state.collectAsState()
    val currentScreen = backStack.lastOrNull() ?: DESTINATION_INBOX
    val reducedMotion = rememberReducedMotionEnabled()
    val inboxListState = rememberLazyListState()
    val inboxFilterState = rememberLazyListState()
    val archivedListState = rememberLazyListState()
    val settingsListState = rememberLazyListState()
    val newChatListState = rememberLazyListState()

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

    // Trigger biometric auth check when fingerprint is enabled and not yet authenticated.
    LaunchedEffect(shellThemeState.fingerprintEnabled, isAuthenticated) {
        if (!shellThemeState.fingerprintEnabled || isAuthenticated) return@LaunchedEffect
        when (checkBiometricAvailability(context)) {
            BiometricAvailability.Available -> {
                backStack = listOf(DESTINATION_LOCK)
            }
            else -> {
                backStack = listOf(DESTINATION_LOCK)
            }
        }
    }

    fun navigateBack() {
        if (currentScreen == DESTINATION_CONVERSATION) {
            smsViewModel.closeConversation()
        }
        backStack = backStack.dropLast(1).ifEmpty { listOf(DESTINATION_INBOX) }
    }

    BackHandler(enabled = currentScreen != DESTINATION_INBOX) {
        if (currentScreen != DESTINATION_LOCK) {
            navigateBack()
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        AnimatedContent(
            targetState = currentScreen,
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
                                onOpenConversation(address, threadId)
                                backStack = listOf(DESTINATION_INBOX, DESTINATION_CONVERSATION)
                            },
                            onOpenArchivedChats = {
                                backStack = listOf(DESTINATION_INBOX, DESTINATION_ARCHIVED)
                            },
                            onOpenSettings = {
                                backStack = listOf(DESTINATION_INBOX, DESTINATION_SETTINGS)
                            },
                            onOpenNewChat = onRequestNewChat,
                            onRefreshInbox = smsViewModel::refreshInbox,
                            onTogglePinned = smsViewModel::toggleThreadPinned,
                            onToggleArchived = smsViewModel::toggleThreadArchived,
                            onSetThreadUnread = smsViewModel::setThreadUnread,
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
                            backStack = listOf(DESTINATION_INBOX)
                        },
                        onStartConversation = { recipient, subscriptionId ->
                            activeAddress = recipient.address
                            activeConversationTitle = displayNameFor(context, recipient.address)
                            activeSubscriptionId = subscriptionId
                            conversationDraftSeed = pendingForwardDraft.orEmpty()
                            pendingForwardDraft = null
                            onOpenConversation(recipient.address, null)
                            backStack = listOf(DESTINATION_INBOX, DESTINATION_NEW_CHAT, DESTINATION_CONVERSATION)
                        },
                    )
                }

                DESTINATION_CONVERSATION -> {
                    val conversationState by smsViewModel.conversationState.collectAsState()
                    val sendState by smsViewModel.sendState.collectAsState()
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
                        sendState = sendState,
                        onBack = {
                            navigateBack()
                        },
                        onSubscriptionIdChange = { activeSubscriptionId = it },
                        onSend = { body ->
                            onSendMessage(activeAddress, body, activeSubscriptionId)
                        },
                        onRetrySend = smsViewModel::retrySend,
                        onClearSendState = smsViewModel::clearSendState,
                        onDraftConsumed = { conversationDraftSeed = "" },
                        onDeleteMessage = smsViewModel::deleteMessage,
                        onForwardMessage = { body ->
                            pendingForwardDraft = body
                            newChatQuery = ""
                            backStack = listOf(DESTINATION_INBOX, DESTINATION_NEW_CHAT)
                        },
                    )
                }

                DESTINATION_SETTINGS -> {
                    val inboxState by smsViewModel.inboxState.collectAsState()
                    SettingsScreen(
                        themeViewModel = themeViewModel,
                        listState = settingsListState,
                        archivedCount = inboxState.archivedThreads.size,
                        onBack = {
                            backStack = backStack.dropLast(1).ifEmpty { listOf(DESTINATION_INBOX) }
                        },
                        onRequestDefaultSms = onRequestDefaultSms,
                        onOpenArchivedChats = {
                            backStack = listOf(DESTINATION_INBOX, DESTINATION_SETTINGS, DESTINATION_ARCHIVED)
                        },
                        onOpenSecurity = {
                            backStack = listOf(DESTINATION_INBOX, DESTINATION_SETTINGS, DESTINATION_SECURITY)
                        },
                        isDefaultSmsApp = inboxState.isDefaultSmsApp,
                    )
                }

                DESTINATION_ARCHIVED -> {
                    val inboxState by smsViewModel.inboxState.collectAsState()
                    ArchivedChatsScreen(
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
                            backStack = listOf(DESTINATION_INBOX, DESTINATION_SETTINGS, DESTINATION_ARCHIVED, DESTINATION_CONVERSATION)
                        },
                        onRefreshInbox = smsViewModel::refreshInbox,
                        onTogglePinned = smsViewModel::toggleThreadPinned,
                        onToggleArchived = smsViewModel::toggleThreadArchived,
                        onSetThreadUnread = smsViewModel::setThreadUnread,
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

                DESTINATION_LOCK -> {
                    LockScreen(
                        availability = checkBiometricAvailability(context),
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

// ═══════════════════════════════════════════════════════════
// REAL SMS INBOX
// ═══════════════════════════════════════════════════════════

@Composable
private fun RealInboxScreen(
    state: RealInboxState,
    listState: LazyListState,
    filterState: LazyListState,
    onOpenConversation: (String, Long?) -> Unit,
    onOpenArchivedChats: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNewChat: () -> Unit,
    onRefreshInbox: () -> Unit,
    onTogglePinned: (Long) -> Unit,
    onToggleArchived: (Long) -> Unit,
    onSetThreadUnread: (Long?, String, Boolean) -> Unit,
    onDeleteThread: (Long?, String) -> Unit,
) {
    var selectedFilter by rememberSaveable { mutableIntStateOf(0) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var contextMenuThreadId by rememberSaveable { mutableStateOf<Long?>(null) }
    val context = LocalContext.current
    val reducedMotion = rememberReducedMotionEnabled()
    val listFlingBehavior = rememberSmoothFlingBehavior(enabled = !reducedMotion)
    val filterFlingBehavior = rememberSmoothFlingBehavior(enabled = !reducedMotion)

    val filteredByChip = remember(state.threads, selectedFilter) {
        when (InboxFilter.entries[selectedFilter]) {
            InboxFilter.All -> state.threads
            InboxFilter.OTP -> state.threads.filter { t ->
                t.snippet.contains("code", true) || t.snippet.contains("OTP", true) ||
                    t.snippet.contains("verification", true) || t.snippet.contains("verify", true)
            }
            InboxFilter.Business -> state.threads.filter { t -> t.address.any { it.isLetter() } }
            InboxFilter.Personal -> state.threads.filter { t -> t.address.all { it.isDigit() || it == '+' || it == ' ' } }
        }
    }

    val normalizedQuery = remember(searchQuery) { searchQuery.trim() }
    val searchableDisplayNames = remember(filteredByChip, context) {
        filteredByChip.associateWith { thread ->
            displayNameFor(context, thread.address)
        }
    }
    val filteredThreads = remember(filteredByChip, normalizedQuery, searchableDisplayNames) {
        if (normalizedQuery.isBlank()) {
            filteredByChip
        } else {
            filteredByChip.filter { thread ->
                val displayName = searchableDisplayNames[thread].orEmpty()
                displayName.contains(normalizedQuery, ignoreCase = true) ||
                    thread.address.contains(normalizedQuery, ignoreCase = true) ||
                    thread.snippet.contains(normalizedQuery, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(normalizedQuery, selectedFilter) {
        if (listState.firstVisibleItemIndex > 0) {
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(filteredThreads, contextMenuThreadId) {
        val activeThreadId = contextMenuThreadId ?: return@LaunchedEffect
        if (filteredThreads.none { it.threadId == activeThreadId }) {
            contextMenuThreadId = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
                actions = {
                    IconButton(onClick = onOpenArchivedChats) {
                        Icon(Icons.Rounded.Archive, contentDescription = "Archived chats", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier.semantics {
                    role = Role.Button
                    contentDescription = "New chat"
                },
                onClick = onOpenNewChat,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(24.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.AddComment,
                        contentDescription = null,
                    )
                },
                text = {
                    Text(
                        text = "New chat",
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
            state = listState,
            flingBehavior = listFlingBehavior,
            modifier = Modifier
                .fillMaxSize()
                .elasticOverscroll(
                    enabled = !reducedMotion,
                    state = listState,
                ),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 4.dp,
                bottom = innerPadding.calculateBottomPadding() + 16.dp,
                start = 16.dp, end = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item(key = "inbox_search") {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .padding(bottom = 6.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(34.dp)) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear search",
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    },
                    placeholder = {
                        Text(
                            text = "Search conversations",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    shape = RoundedCornerShape(18.dp),
                )
            }
            item(key = "filter_chips") {
                LazyRow(
                    state = filterState,
                    flingBehavior = filterFlingBehavior,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .elasticOverscroll(
                            enabled = !reducedMotion,
                            state = filterState,
                            orientation = Orientation.Horizontal,
                        ),
                ) {
                    items(
                        count = InboxFilter.entries.size,
                        key = { index -> "inbox_filter_${InboxFilter.entries[index].name}" },
                        contentType = { "inbox_filter_chip" },
                    ) { index ->
                        val filter = InboxFilter.entries[index]
                        val animatedModifier = motionAnimateItemModifier(reducedMotion)
                            .then(rememberEntranceModifier(filter.name, reducedMotion))
                        FilterChip(
                            modifier = animatedModifier,
                            selected = selectedFilter == index,
                            onClick = { selectedFilter = index },
                            label = { Text(filter.label) },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }
            }
            when {
                state.loading && state.showLoadingCard -> {
                    item(key = "inbox_loading") {
                        InboxLoadingStateCard(onRefreshInbox = onRefreshInbox)
                    }
                }
                state.errorMessage != null -> {
                    item(key = "inbox_error") {
                        InboxErrorStateCard(
                            message = state.errorMessage,
                            onRetry = onRefreshInbox,
                        )
                    }
                }
                state.threads.isEmpty() && !state.loading -> {
                    item(key = "inbox_empty") {
                        InboxEmptyStateCard(onOpenNewChat = onOpenNewChat)
                    }
                }
                filteredThreads.isEmpty() -> {
                    item(key = "inbox_filtered_empty") {
                        InboxFilteredEmptyStateCard(
                            activeFilter = if (normalizedQuery.isNotBlank()) {
                                "${InboxFilter.entries[selectedFilter].label} · \"$normalizedQuery\""
                            } else {
                                InboxFilter.entries[selectedFilter].label
                            },
                            onShowAll = {
                                selectedFilter = InboxFilter.All.ordinal
                                searchQuery = ""
                            },
                        )
                    }
                }
                else -> {
                    items(
                        items = filteredThreads,
                        key = { "${it.threadId}:${it.address}" },
                        contentType = { "inbox_thread" },
                    ) { thread ->
                        val isMenuOpenForThread = contextMenuThreadId == thread.threadId
                        val itemModifier = motionAnimateItemModifier(reducedMotion)
                            .then(rememberEntranceModifier(thread.address, reducedMotion))
                            .then(if (isMenuOpenForThread) Modifier.zIndex(2f) else Modifier)
                        SmsThreadCard(
                            thread = thread,
                            isPinned = thread.threadId in state.pinnedThreadIds,
                            isArchived = thread.threadId in state.archivedThreadIds,
                            isContextMenuOpen = contextMenuThreadId == thread.threadId,
                            onClick = { onOpenConversation(thread.address, thread.threadId) },
                            onLongPress = { contextMenuThreadId = thread.threadId },
                            onDismissMenu = { contextMenuThreadId = null },
                            onTogglePinned = { onTogglePinned(thread.threadId) },
                            onToggleArchived = { onToggleArchived(thread.threadId) },
                            onToggleUnread = {
                                onSetThreadUnread(
                                    thread.threadId,
                                    thread.address,
                                    thread.unreadCount == 0,
                                )
                            },
                            onDelete = { onDeleteThread(thread.threadId, thread.address) },
                            modifier = itemModifier,
                        )
                    }
                }
            }

        }
    }
}
}

@Composable
private fun ArchivedChatsScreen(
    threads: List<SmsThread>,
    pinnedThreadIds: Set<Long>,
    archivedThreadIds: Set<Long>,
    loading: Boolean,
    errorMessage: String?,
    listState: LazyListState,
    onBack: () -> Unit,
    onOpenConversation: (String, Long?) -> Unit,
    onRefreshInbox: () -> Unit,
    onTogglePinned: (Long) -> Unit,
    onToggleArchived: (Long) -> Unit,
    onSetThreadUnread: (Long?, String, Boolean) -> Unit,
    onDeleteThread: (Long?, String) -> Unit,
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val listFlingBehavior = rememberSmoothFlingBehavior(enabled = !reducedMotion)
    var contextMenuThreadId by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(threads, contextMenuThreadId) {
        val activeThreadId = contextMenuThreadId ?: return@LaunchedEffect
        if (threads.none { it.threadId == activeThreadId }) {
            contextMenuThreadId = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Archived chats", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            flingBehavior = listFlingBehavior,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .elasticOverscroll(
                    enabled = !reducedMotion,
                    state = listState,
                ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            when {
                loading -> {
                    item(key = "archived_loading") {
                        InboxLoadingStateCard(onRefreshInbox = onRefreshInbox)
                    }
                }
                errorMessage != null -> {
                    item(key = "archived_error") {
                        InboxErrorStateCard(
                            message = errorMessage,
                            onRetry = onRefreshInbox,
                        )
                    }
                }
                threads.isEmpty() -> {
                    item(key = "archived_empty") {
                        InboxStateCard(
                            title = "No archived chats",
                            body = "Archive a conversation from the inbox to keep it out of your primary list without deleting it.",
                            statusLabel = "Archive empty",
                            actionLabel = "Back to inbox",
                            icon = Icons.Rounded.Archive,
                            onAction = onBack,
                        )
                    }
                }
                else -> {
                    items(
                        items = threads,
                        key = { "${it.threadId}:${it.address}" },
                        contentType = { "archived_thread" },
                    ) { thread ->
                        val itemModifier = motionAnimateItemModifier(reducedMotion)
                            .then(rememberEntranceModifier("archived_${thread.threadId}_${thread.address}", reducedMotion))
                        val isMenuOpenForThread = contextMenuThreadId == thread.threadId
                        SmsThreadCard(
                            thread = thread,
                            isPinned = thread.threadId in pinnedThreadIds,
                            isArchived = thread.threadId in archivedThreadIds,
                            isContextMenuOpen = isMenuOpenForThread,
                            onClick = { onOpenConversation(thread.address, thread.threadId) },
                            onLongPress = { contextMenuThreadId = thread.threadId },
                            onDismissMenu = { contextMenuThreadId = null },
                            onTogglePinned = { onTogglePinned(thread.threadId) },
                            onToggleArchived = { onToggleArchived(thread.threadId) },
                            onToggleUnread = {
                                onSetThreadUnread(
                                    thread.threadId,
                                    thread.address,
                                    thread.unreadCount == 0,
                                )
                            },
                            onDelete = { onDeleteThread(thread.threadId, thread.address) },
                            modifier = if (isMenuOpenForThread) itemModifier.then(Modifier.zIndex(2f)) else itemModifier,
                        )
                    }
                }
            }

        }
    }
}

@Composable
private fun InboxOnboardingScreen(
    accessState: InboxAccessState,
    hasPendingLaunchRequest: Boolean,
    onRequestSmsPermissions: () -> Unit,
    onRequestDefaultSms: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isAlreadyDefault = !accessState.permissionDenied && accessState.isDefaultSmsApp

    val title: String
    val body: String
    val ctaLabel: String
    val ctaIcon: ImageVector
    val onCtaClick: () -> Unit
    val statusLabel: String

    if (accessState.permissionDenied) {
        title = "Unlock your inbox"
        body = "Pulse needs SMS access before it can read threads, open drafts, and route you into the right conversation."
        ctaLabel = "Grant permissions"
        ctaIcon = Icons.Rounded.Key
        onCtaClick = onRequestSmsPermissions
        statusLabel = "SMS permission required"
    } else if (isAlreadyDefault) {
        title = "Pulse is your default app"
        body = "Android is ready to route SMS messages to Pulse. Everything is set up correctly."
        ctaLabel = "Manage default app"
        ctaIcon = Icons.Rounded.CheckCircle
        onCtaClick = onRequestDefaultSms
        statusLabel = "Default SMS app active"
    } else {
        title = "Make Pulse your default"
        body = "Set Pulse as your default SMS app so Android can hand off compose requests, send reliably, and keep your inbox in one place."
        ctaLabel = "Set as default"
        ctaIcon = Icons.Rounded.MarkunreadMailbox
        onCtaClick = onRequestDefaultSms
        statusLabel = "Default SMS app required"
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                        ),
                    ),
                ),
        ) {
            val cardWidth = if (maxWidth > 720.dp) 520.dp else maxWidth - 32.dp
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    modifier = Modifier.widthIn(max = cardWidth),
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    tonalElevation = 0.dp,
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)),
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        StatusPill(
                            label = statusLabel,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            tonalElevation = 0.dp,
                            modifier = Modifier.size(72.dp),
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = ctaIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineMedium,
                            )
                            Text(
                                text = body,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (hasPendingLaunchRequest) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.68f),
                                tonalElevation = 0.dp,
                            ) {
                                Text(
                                    text = "A requested conversation is waiting. Pulse will open it as soon as setup is complete.",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                            }
                        }
                        Button(
                            onClick = onCtaClick,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = ctaIcon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(ctaLabel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InboxLoadingStateCard(
    onRefreshInbox: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InboxStateCard(
        title = "Loading your threads",
        body = "Pulse is syncing with Android so your latest messages appear in one place.",
        statusLabel = "Inbox refresh",
        icon = Icons.Rounded.HourglassTop,
        actionLabel = "Refresh",
        onAction = onRefreshInbox,
        modifier = modifier,
    ) {
        SerafinaProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun InboxEmptyStateCard(
    onOpenNewChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InboxStateCard(
        title = "Your inbox is ready",
        body = "There are no SMS threads here yet. Start a new conversation and Pulse will keep the lane warm for you.",
        statusLabel = "Zero threads",
        icon = Icons.Rounded.AddComment,
        actionLabel = "New chat",
        onAction = onOpenNewChat,
        modifier = modifier,
    )
}

@Composable
private fun InboxFilteredEmptyStateCard(
    activeFilter: String,
    onShowAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InboxStateCard(
        title = "Nothing in $activeFilter",
        body = "This filter is clear right now. Switch back to the full inbox to see every thread again.",
        statusLabel = "$activeFilter filter",
        icon = Icons.Rounded.Search,
        actionLabel = "Show all",
        onAction = onShowAll,
        modifier = modifier,
    )
}

@Composable
private fun InboxErrorStateCard(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InboxStateCard(
        title = "Inbox unavailable",
        body = message,
        statusLabel = "Read problem",
        icon = Icons.Rounded.ErrorOutline,
        actionLabel = "Try again",
        onAction = onRetry,
        modifier = modifier,
    )
}

@Composable
private fun InboxStateCard(
    title: String,
    body: String,
    statusLabel: String,
    icon: ImageVector,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    supportingContent: @Composable (() -> Unit)? = null,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier.background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.10f),
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f),
                    ),
                ),
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                        tonalElevation = 0.dp,
                        modifier = Modifier.size(52.dp),
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = accentColor,
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        StatusPill(
                            label = statusLabel,
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                }
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                supportingContent?.invoke()
                FilledTonalButton(onClick = onAction) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun SmsThreadCard(
    thread: SmsThread,
    isPinned: Boolean,
    isArchived: Boolean,
    isContextMenuOpen: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onDismissMenu: () -> Unit,
    onTogglePinned: () -> Unit,
    onToggleArchived: () -> Unit,
    onToggleUnread: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val reducedMotion = rememberReducedMotionEnabled()
    val displayName = remember(thread.address) { displayNameFor(context, thread.address) }
    val initials = displayName.toAvatarInitials()
    val hasUnread = thread.unreadCount > 0
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val containerColor by animateColorAsState(
        targetValue = when {
            isContextMenuOpen -> MaterialTheme.colorScheme.surfaceContainerHigh
            hasUnread -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        },
        label = "thread_card_container",
    )
    val outlineColor by animateColorAsState(
        targetValue = when {
            isContextMenuOpen -> MaterialTheme.colorScheme.primary.copy(alpha = 0.44f)
            hasUnread -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
            isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f)
        },
        label = "thread_card_outline",
    )
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed || isContextMenuOpen) 0.985f else 1f,
        animationSpec = if (reducedMotion) tween(0) else spring(
            stiffness = Spring.StiffnessMedium,
            dampingRatio = Spring.DampingRatioNoBouncy,
        ),
        label = "thread_card_press_scale",
    )
    val semanticsLabel = remember(displayName, thread.unreadCount) {
        buildString {
            append("Open thread ")
            append(displayName)
            if (thread.unreadCount > 0) {
                append(", ")
                append(thread.unreadCount)
                append(" unread")
            }
        }
    }
    val cardShape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .clip(cardShape)
            .semantics {
                role = Role.Button
                contentDescription = semanticsLabel
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongPress,
            ),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = SolidColor(outlineColor),
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                SerafinaAvatar(imageUrl = null, initials = initials, hasUnread = hasUnread, size = 48.dp)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = displayName,
                            style = if (hasUnread) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            else MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (isPinned) {
                            StatusPill(
                                label = "Pinned",
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                    Text(
                        text = thread.snippet,
                        style = if (hasUnread) {
                            MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                        color = if (hasUnread) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = thread.timestamp.toInboxTimestamp(),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (hasUnread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (hasUnread) {
                        Box(
                            modifier = Modifier.size(20.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(thread.unreadCount.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        }

        SerafinaContextMenu(
            expanded = isContextMenuOpen,
            onDismissRequest = onDismissMenu,
        ) {
            SerafinaContextMenuItem(
                text = if (isPinned) "Unpin" else "Pin",
                icon = if (isPinned) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                onClick = {
                    onDismissMenu()
                    onTogglePinned()
                },
            )
            SerafinaContextMenuItem(
                text = if (isArchived) "Unarchive" else "Archive",
                icon = Icons.Rounded.Archive,
                onClick = {
                    onDismissMenu()
                    onToggleArchived()
                },
            )
            SerafinaContextMenuItem(
                text = if (hasUnread) "Mark as read" else "Mark as unread",
                icon = Icons.Rounded.MarkunreadMailbox,
                onClick = {
                    onDismissMenu()
                    onToggleUnread()
                },
            )
            SerafinaContextMenuDivider()
            SerafinaContextMenuItem(
                text = "Delete",
                icon = Icons.Rounded.Delete,
                contentColor = MaterialTheme.colorScheme.error,
                onClick = {
                    onDismissMenu()
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun SerafinaContextMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    shadowElevation: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.widthIn(min = 204.dp),
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
        shadowElevation = shadowElevation,
        content = content,
    )
}

@Composable
private fun SerafinaContextMenuItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                color = contentColor,
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
            )
        },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        onClick = onClick,
    )
}

@Composable
private fun SerafinaContextMenuDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f),
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
    )
}

@Composable
private fun NewChatScreen(
    threads: List<SmsThread>,
    listState: LazyListState,
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onStartConversation: (NewChatRecipient, Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val fallbackSimOption = remember {
        NewChatSimOption(
            key = "sim_default",
            subscriptionId = null,
            slotLabel = "SIM 1",
            carrierLabel = "Default line",
        )
    }
    val directoryRecipients by produceState<List<NewChatRecipient>?>(
        initialValue = null,
        key1 = context,
    ) {
        value = withContext(Dispatchers.IO) {
            loadNewChatRecipients(context)
        }
    }
    val recipients = remember(directoryRecipients, threads) {
        directoryRecipients?.mergeThreadRecipients(threads)
    }
    val simOptions by produceState(
        initialValue = emptyList<NewChatSimOption>(),
        key1 = context,
    ) {
        value = withContext(Dispatchers.IO) {
            loadSimOptions(context)
        }
    }
    var selectedSimKey by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(simOptions) {
        if (simOptions.isNotEmpty() && simOptions.none { it.key == selectedSimKey }) {
            selectedSimKey = simOptions.first().key
        }
    }

    val availableSimOptions = remember(simOptions, fallbackSimOption) {
        if (simOptions.isEmpty()) listOf(fallbackSimOption) else simOptions
    }
    val selectedSim = remember(availableSimOptions, selectedSimKey) {
        availableSimOptions.firstOrNull { it.key == selectedSimKey } ?: availableSimOptions.firstOrNull()
    }
    val normalizedQuery = remember(query) { query.trim() }
    val filteredRecipients = remember(normalizedQuery, recipients) {
        val availableRecipients = recipients.orEmpty()
        if (normalizedQuery.isBlank()) {
            availableRecipients
        } else {
            availableRecipients.filter { recipient ->
                recipient.displayName.contains(normalizedQuery, ignoreCase = true) ||
                    recipient.address.contains(normalizedQuery, ignoreCase = true)
            }
        }
    }
    val directEntryAddress = normalizedQuery.takeIf { it.isDirectAddressCandidate() }
    val shouldShowDirectEntry = remember(directEntryAddress, filteredRecipients) {
        directEntryAddress != null && filteredRecipients.none { recipient ->
            recipient.address.equals(directEntryAddress, ignoreCase = true) ||
                recipient.displayName.equals(directEntryAddress, ignoreCase = true)
        }
    }
    val groupedRecipients = remember(filteredRecipients) {
        filteredRecipients.toContactGroups()
    }
    val recipientIndex = remember(filteredRecipients) {
        filteredRecipients.associateBy { it.key }
    }

    NewChatContactSelectionScreen(
        contactGroups = groupedRecipients,
        loading = recipients == null,
        searchQuery = query,
        simOptions = availableSimOptions,
        selectedSimKey = selectedSim?.key,
        onContactClick = { contact ->
            val recipient = recipientIndex[contact.key] ?: NewChatRecipient(
                key = contact.key,
                displayName = contact.name,
                address = contact.phoneNumber,
                sortLabel = contact.name,
            )
            onStartConversation(recipient, selectedSim?.subscriptionId)
        },
        onBackClick = onBack,
        onSearchQueryChange = onQueryChange,
        modifier = modifier,
        listState = listState,
        manualEntry = directEntryAddress?.takeIf { shouldShowDirectEntry }?.let { address ->
            ContactListItem(
                key = "manual_$address",
                name = address,
                phoneNumber = address,
            )
        },
        onManualEntryClick = { contact ->
            onStartConversation(
                NewChatRecipient(
                    key = contact.key,
                    displayName = contact.phoneNumber,
                    address = contact.phoneNumber,
                    sortLabel = contact.phoneNumber,
                ),
                selectedSim?.subscriptionId,
            )
        },
        onSimOptionClick = { option ->
            selectedSimKey = option.key
        },
    )
}

@Composable
private fun RealConversationScreen(
    title: String,
    address: String,
    initialDraft: String,
    initialSubscriptionId: Int?,
    messages: List<SystemSms>,
    loading: Boolean,
    importantMessageIds: Set<Long>,
    sendState: SendState,
    onBack: () -> Unit,
    onSubscriptionIdChange: (Int?) -> Unit,
    onSend: (String) -> Unit,
    onRetrySend: () -> Unit,
    onClearSendState: () -> Unit,
    onDraftConsumed: () -> Unit,
    onDeleteMessage: (Long) -> Unit,
    onForwardMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val reducedMotion = rememberReducedMotionEnabled()
    val listFlingBehavior = rememberSmoothFlingBehavior(enabled = !reducedMotion)
    var draft by rememberSaveable(address) { mutableStateOf("") }
    var previousMessageCount by remember(address) { mutableIntStateOf(0) }
    val fallbackSimOption = remember {
        NewChatSimOption(
            key = "sim_default",
            subscriptionId = null,
            slotLabel = "SIM 1",
            carrierLabel = "Default line",
        )
    }
    val simOptions by produceState(
        initialValue = emptyList<NewChatSimOption>(),
        key1 = context,
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
        when (sendState) {
            is SendState.Sent -> {
                draft = ""
                delay(1200)
                onClearSendState()
            }
            is SendState.Failed -> {
                if (draft.isBlank()) {
                    draft = sendState.body
                }
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

    LaunchedEffect(address, loading, timelineItems.size) {
        if (!loading && !hasPositionedInitialMessages && timelineItems.isNotEmpty()) {
            listState.scrollToItem(timelineItems.lastIndex)
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
            listState.scrollToItemSmoothly(timelineItems.lastIndex)
        }
        previousMessageCount = messages.size
    }

    val isReplyable = remember(address) {
        // Addresses containing letters are typically business/shortcode senders
        // that don't accept replies (OTP, bank, service notifications)
        address.none { it.isLetter() }
    }

    // ── Keyboard auto-scroll: when IME opens, scroll to latest message ──
    val isKeyboardVisible = WindowInsets.isImeVisible
    LaunchedEffect(isKeyboardVisible, isNearEnd, timelineItems.size) {
        if (isKeyboardVisible && isNearEnd && timelineItems.isNotEmpty()) {
            listState.scrollToItemSmoothly(timelineItems.lastIndex)
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = onBack,
                        modifier = Modifier.padding(start = 12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = "Back",
                        )
                    }
                },
                title = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SerafinaAvatar(
                            imageUrl = null,
                            initials = title.toAvatarInitials(),
                            hasUnread = messages.lastOrNull()?.let { it.isInbound && !it.read } == true,
                            size = 40.dp,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = address.toConversationMetaLabel(
                                    totalMessages = messages.size,
                                    unreadCount = unreadCount,
                                    importantCount = importantCount,
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            if (isReplyable) {
                Column {
                    ConversationSendStatusRow(
                        sendState = when (sendState) {
                            is SendState.Sent -> SendState.Idle
                            else -> sendState
                        },
                        onRetrySend = onRetrySend,
                    )
                    ConversationComposer(
                        draft = draft,
                        sendState = sendState,
                        simOptions = availableSimOptions,
                        selectedSimKey = selectedSim?.key,
                        onSimOptionClick = { option ->
                            selectedSimKey = option.key
                        },
                        onDraftChange = {
                            draft = it
                            if (sendState is SendState.Failed || sendState is SendState.Sent) {
                                onClearSendState()
                            }
                        },
                        onSend = {
                            val message = draft.trim()
                            if (message.isEmpty()) return@ConversationComposer
                            onSend(message)
                        },
                    )
                }
            } else {
                ReadOnlyConversationNotice()
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceContainerLowest,
                            MaterialTheme.colorScheme.surface,
                        ),
                    ),
                ),
        ) {
            LazyColumn(
                state = listState,
                flingBehavior = listFlingBehavior,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .elasticOverscroll(
                        enabled = !reducedMotion,
                        state = listState,
                    ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "conversation_header") {
                    ConversationOverviewCard(
                        title = title,
                        address = address,
                        messageCount = messages.size,
                        unreadCount = unreadCount,
                        importantCount = importantCount,
                        latestTimestamp = messages.lastOrNull()?.timestamp,
                        modifier = rememberEntranceModifier("conversation_header_$address", reducedMotion),
                    )
                }

                when {
                    loading -> {
                        item(key = "conversation_loading") {
                            ConversationLoadingSkeleton(
                                modifier = rememberEntranceModifier("conversation_loading_$address", reducedMotion),
                            )
                        }
                    }
                    timelineItems.isEmpty() -> {
                        item(key = "conversation_empty") {
                            EmptyConversationState(
                                title = title,
                                modifier = rememberEntranceModifier("conversation_empty_$address", reducedMotion),
                            )
                        }
                    }
                    else -> {
                        items(
                            items = timelineItems,
                            key = ConversationTimelineItem::key,
                            contentType = ConversationTimelineItem::contentType,
                        ) { item ->
                            when (item) {
                                is ConversationTimelineItem.DayDivider -> {
                                    Box(
                                        modifier = motionAnimateItemModifier(reducedMotion)
                                            .then(rememberEntranceModifier(item.key, reducedMotion))
                                            .fillMaxWidth(),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        ) {
                                            Text(
                                                text = item.label,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }

                                is ConversationTimelineItem.UnreadDivider -> {
                                    Box(
                                        modifier = motionAnimateItemModifier(reducedMotion)
                                            .then(rememberEntranceModifier(item.key, reducedMotion))
                                            .fillMaxWidth(),
                                        contentAlignment = Alignment.CenterStart,
                                    ) {
                                        StatusPill(
                                            label = item.label,
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                        )
                                    }
                                }

                                is ConversationTimelineItem.Message -> {
                                    ConversationMessageBubble(
                                        message = item.message,
                                        isImportant = item.message.id in importantMessageIds,
                                        isContextMenuOpen = contextMenuMessageId == item.message.id,
                                        onLongPress = {
                                            contextMenuMessageId =
                                                if (contextMenuMessageId == item.message.id) null else item.message.id
                                        },
                                        onDismissMenu = { contextMenuMessageId = null },
                                        onCopy = {
                                            clipboardManager?.setPrimaryClip(
                                                ClipData.newPlainText("message", item.message.body)
                                            )
                                            contextMenuMessageId = null
                                        },
                                        onDelete = {
                                            onDeleteMessage(item.message.id)
                                            contextMenuMessageId = null
                                        },
                                        onForward = {
                                            onForwardMessage(item.message.body)
                                            contextMenuMessageId = null
                                        },
                                        modifier = motionAnimateItemModifier(reducedMotion)
                                            .then(rememberEntranceModifier(item.key, reducedMotion)),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}

@Composable
private fun ConversationOverviewCard(
    title: String,
    address: String,
    messageCount: Int,
    unreadCount: Int,
    importantCount: Int,
    latestTimestamp: Instant?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SerafinaAvatar(
                imageUrl = null,
                initials = title.toAvatarInitials(),
                size = 52.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = address.toConversationMetaLabel(
                        totalMessages = messageCount.coerceAtLeast(0),
                        unreadCount = unreadCount,
                        importantCount = importantCount,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            latestTimestamp?.let { timestamp ->
                Text(
                    text = timestamp.toInboxTimestamp(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ConversationMessageBubble(
    message: SystemSms,
    isImportant: Boolean,
    isContextMenuOpen: Boolean,
    onLongPress: () -> Unit,
    onDismissMenu: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onForward: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val isOutbound = message.isOutbound
    val isUnread = message.isInbound && !message.read
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bubbleShape = RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 24.dp,
        bottomStart = if (isOutbound) 24.dp else 10.dp,
        bottomEnd = if (isOutbound) 10.dp else 24.dp,
    )
    val bubbleScale by animateFloatAsState(
        targetValue = if (isPressed || isContextMenuOpen) 0.985f else 1f,
        animationSpec = if (reducedMotion) tween(0) else spring(
            stiffness = Spring.StiffnessMedium,
            dampingRatio = Spring.DampingRatioNoBouncy,
        ),
        label = "message_bubble_press_scale",
    )
    val bubbleElevation by animateDpAsState(
        targetValue = if (isContextMenuOpen) 6.dp else if (isPressed) 2.dp else if (isOutbound) 0.dp else 1.dp,
        animationSpec = if (reducedMotion) tween(0) else spring(
            stiffness = Spring.StiffnessMedium,
            dampingRatio = Spring.DampingRatioNoBouncy,
        ),
        label = "message_bubble_press_elevation",
    )
    val bubbleOutlineColor by animateColorAsState(
        targetValue = when {
            isContextMenuOpen -> MaterialTheme.colorScheme.primary.copy(alpha = 0.46f)
            isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
            isUnread -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.42f)
            else -> Color.Transparent
        },
        label = "message_bubble_press_outline",
    )
    val shouldShowBubbleOutline = isContextMenuOpen || isPressed || (isUnread && !isOutbound)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isOutbound) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .graphicsLayer {
                    scaleX = bubbleScale
                    scaleY = bubbleScale
                }
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {},
                    onLongClick = onLongPress,
                ),
        ) {
            Column(
                modifier = Modifier.widthIn(max = 320.dp),
                horizontalAlignment = if (isOutbound) Alignment.End else Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Surface(
                    modifier = Modifier.then(
                        if (shouldShowBubbleOutline) {
                            Modifier.border(
                                width = 1.dp,
                                color = bubbleOutlineColor,
                                shape = bubbleShape,
                            )
                        } else {
                            Modifier
                        }
                    ),
                    shape = bubbleShape,
                    color = if (isOutbound) MaterialTheme.colorScheme.primaryContainer
                    else if (isUnread) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f)
                    else MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = bubbleElevation,
                ) {
                    Text(
                        text = message.body.ifBlank { " " },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (isUnread) FontWeight.Medium else FontWeight.Normal,
                        ),
                        color = if (isOutbound) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (isImportant) {
                        StatusPill(
                            label = "Kept",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    Text(
                        text = message.timestamp.toConversationTime(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUnread) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SerafinaContextMenu(
                expanded = isContextMenuOpen,
                onDismissRequest = onDismissMenu,
                shadowElevation = 0.dp,
            ) {
                SerafinaContextMenuItem(
                    text = "Copy",
                    icon = Icons.Rounded.ContentCopy,
                    onClick = onCopy,
                )
                SerafinaContextMenuItem(
                    text = "Forward",
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = onForward,
                )
                SerafinaContextMenuDivider()
                SerafinaContextMenuItem(
                    text = "Delete",
                    icon = Icons.Rounded.Delete,
                    contentColor = MaterialTheme.colorScheme.error,
                    onClick = onDelete,
                )
            }
        }
    }
}

@Composable
private fun ConversationComposer(
    draft: String,
    sendState: SendState,
    simOptions: List<NewChatSimOption>,
    selectedSimKey: String?,
    onSimOptionClick: (NewChatSimOption) -> Unit,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val isSending = sendState is SendState.Sending
    val canSend by remember(draft, isSending) {
        derivedStateOf { draft.isNotBlank() && !isSending }
    }
    val showSimToggle = simOptions.size > 1
    val fieldInteractionSource = remember { MutableInteractionSource() }
    val isFocused by fieldInteractionSource.collectIsFocusedAsState()
    val sendInteractionSource = remember { MutableInteractionSource() }
    val isSendPressed by sendInteractionSource.collectIsPressedAsState()
    val quickDuration = if (reducedMotion) 0 else 200
    val exitDuration = if (reducedMotion) 0 else 140

    // ── Pill container animations ──
    val containerColor by animateColorAsState(
        targetValue = if (isFocused) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(durationMillis = quickDuration),
        label = "composer_container",
    )
    val containerLift by animateFloatAsState(
        targetValue = if (isFocused) -2f else 0f,
        animationSpec = if (reducedMotion) tween(0) else spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioNoBouncy,
        ),
        label = "composer_lift",
    )

    // ── Send button animations (the hero moment) ──
    val sendScale by animateFloatAsState(
        targetValue = when {
            isSendPressed && canSend -> 0.92f
            canSend -> 1f
            else -> 0.88f
        },
        animationSpec = if (reducedMotion) tween(0) else spring(
            stiffness = Spring.StiffnessMedium,
            dampingRatio = Spring.DampingRatioNoBouncy,
        ),
        label = "send_scale",
    )
    val sendContainerColor by animateColorAsState(
        targetValue = if (canSend) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = tween(durationMillis = quickDuration),
        label = "send_color",
    )
    val sendContentColor by animateColorAsState(
        targetValue = if (canSend) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = quickDuration),
        label = "send_content",
    )
    val sendIconRotation by animateFloatAsState(
        targetValue = if (canSend) 0f else -30f,
        animationSpec = if (reducedMotion) tween(0) else spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioNoBouncy,
        ),
        label = "send_rotation",
    )

    val pillShape = RoundedCornerShape(28.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .graphicsLayer { translationY = containerLift },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showSimToggle) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
            ) {
                items(simOptions, key = { it.key }) { option ->
                    val selected = option.key == selectedSimKey
                    FilterChip(
                        selected = selected,
                        onClick = { onSimOptionClick(option) },
                        enabled = !isSending,
                        modifier = Modifier.height(32.dp),
                        label = {
                            Text(
                                text = option.slotLabel,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        leadingIcon = null,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            BasicTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier.weight(1f),
                enabled = !isSending,
                interactionSource = fieldInteractionSource,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send,
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (canSend) {
                            onSend()
                        }
                    },
                ),
                maxLines = 6,
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(pillShape)
                            .background(containerColor)
                            .heightIn(min = 52.dp)
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (draft.isEmpty()) {
                            Text(
                                text = if (isSending) "Sending…" else "Message",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                        innerTextField()
                    }
                },
            )

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .graphicsLayer {
                        scaleX = sendScale
                        scaleY = sendScale
                    }
                    .clip(CircleShape)
                    .background(sendContainerColor)
                    .clickable(
                        interactionSource = sendInteractionSource,
                        indication = null,
                        enabled = canSend,
                        onClick = onSend,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = "Send message",
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer { rotationZ = sendIconRotation },
                    tint = sendContentColor,
                )
            }
        }
    }
}

@Composable
private fun ConversationSendStatusRow(
    sendState: SendState,
    onRetrySend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = rememberReducedMotionEnabled()
    AnimatedVisibility(
        visible = sendState !is SendState.Idle,
        enter = fadeIn(animationSpec = tween(if (reducedMotion) 0 else 180)),
        exit = fadeOut(animationSpec = tween(if (reducedMotion) 0 else 120)),
        modifier = modifier,
    ) {
        val containerColor = when (sendState) {
            is SendState.Sending -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
            is SendState.Sent -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
            is SendState.Failed -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.76f)
            SendState.Idle -> MaterialTheme.colorScheme.surfaceContainerLow
        }
        val contentColor = when (sendState) {
            is SendState.Sending -> MaterialTheme.colorScheme.onTertiaryContainer
            is SendState.Sent -> MaterialTheme.colorScheme.onSecondaryContainer
            is SendState.Failed -> MaterialTheme.colorScheme.onErrorContainer
            SendState.Idle -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        val icon = when (sendState) {
            is SendState.Sending -> Icons.Rounded.HourglassTop
            is SendState.Sent -> Icons.Rounded.CheckCircle
            is SendState.Failed -> Icons.Rounded.ErrorOutline
            SendState.Idle -> Icons.Rounded.CheckCircle
        }
        val title = when (sendState) {
            is SendState.Sending -> "Sending message"
            is SendState.Sent -> "Message sent"
            is SendState.Failed -> "Send failed"
            SendState.Idle -> ""
        }
        val subtitle = when (sendState) {
            is SendState.Sending -> "Holding your draft until Android confirms the handoff."
            is SendState.Sent -> "Your draft cleared after the send completed."
            is SendState.Failed -> "Your draft is still here. Retry when you're ready."
            SendState.Idle -> ""
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(top = 8.dp),
            shape = RoundedCornerShape(24.dp),
            color = containerColor,
            tonalElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                    tonalElevation = 0.dp,
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = contentColor,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.84f),
                    )
                }
                if (sendState is SendState.Failed) {
                    FilledTonalButton(onClick = onRetrySend) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Retry")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadOnlyConversationNotice(
    modifier: Modifier = Modifier,
) {
    val borderColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        label = "read_only_notice_border",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp),
            )
            .semantics {
                contentDescription = "Read only business sender. Replies are disabled."
            },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(32.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Outlined.Sms,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "Read only",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Business sender — replies unavailable",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ConversationLoadingSkeleton(
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            StatusPill(
                label = "Opening thread",
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            SerafinaProgressIndicator(modifier = Modifier.fillMaxWidth())
            repeat(3) { index ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (index % 2 == 0) Arrangement.Start else Arrangement.End,
                ) {
                    Surface(
                        modifier = Modifier.widthIn(max = 280.dp),
                        shape = RoundedCornerShape(
                            topStart = 24.dp,
                            topEnd = 24.dp,
                            bottomStart = if (index % 2 == 0) 10.dp else 24.dp,
                            bottomEnd = if (index % 2 == 0) 24.dp else 10.dp,
                        ),
                        color = if (index % 2 == 0) {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                        },
                        tonalElevation = 0.dp,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth(if (index == 1) 0.78f else 0.9f)
                                    .height(14.dp),
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                                tonalElevation = 0.dp,
                            ) {}
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth(if (index == 2) 0.52f else 0.66f)
                                    .height(14.dp),
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f),
                                tonalElevation = 0.dp,
                            ) {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyConversationState(
    title: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "No messages yet",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Start a cleaner thread with $title using the composer below.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════
// SETTINGS SCREEN — Real features wired from codebase
// ═══════════════════════════════════════════════════════════

@Composable
private fun SettingsScreen(
    themeViewModel: SerafinaThemeViewModel,
    listState: LazyListState,
    archivedCount: Int,
    onBack: () -> Unit,
    onRequestDefaultSms: () -> Unit,
    onOpenArchivedChats: () -> Unit,
    onOpenSecurity: () -> Unit,
    isDefaultSmsApp: Boolean,
) {
    val themeState by themeViewModel.state.collectAsState()
    val reducedMotion = rememberReducedMotionEnabled()
    val settingsFlingBehavior = rememberMomentumFlingBehavior(enabled = !reducedMotion)
    val appearanceOptionState = rememberLazyListState()
    val appearanceOptionFlingBehavior = rememberMomentumFlingBehavior(enabled = !reducedMotion)
    val colorSchemeOptions = remember {
        buildList {
            add(SettingsChoiceOption(id = "dynamic", label = "Dynamic"))
            addAll(
                SerafinaPalette.entries.map { palette ->
                    SettingsChoiceOption(
                        id = palette.name,
                        label = palette.label,
                        accentColor = palette.seedColor,
                    )
                },
            )
        }
    }
    val themeOptions = remember {
        SerafinaThemeMode.entries.map { mode ->
            SettingsChoiceOption(id = mode.name, label = mode.label)
        }
    }
    val selectedColorSchemeId = if (themeState.dynamicColorEnabled) {
        "dynamic"
    } else {
        themeState.selectedPalette.name
    }
    val colorSchemeLabel = if (themeState.dynamicColorEnabled) {
        "Dynamic"
    } else {
        themeState.selectedPalette.label
    }

    Scaffold(
        topBar = {
            SettingsTopBar(onBack = onBack)
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            flingBehavior = settingsFlingBehavior,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .elasticOverscroll(
                    enabled = !reducedMotion,
                    state = listState,
                ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item(key = "general_header") { SettingsSectionHeader("General") }
            item(key = "general_card") {
                SettingsGroupCard {
                    SettingsRow(
                        icon = if (isDefaultSmsApp) Icons.Rounded.CheckCircle else Icons.Outlined.Sms,
                        title = if (isDefaultSmsApp) "Default SMS app" else "Set as default",
                        subtitle = if (isDefaultSmsApp) "Pulse is your default SMS app" else "Tap to set Pulse as default",
                        onClick = onRequestDefaultSms,
                    )
                    SettingsGroupDivider()
                    SettingsRow(
                        icon = Icons.Rounded.Archive,
                        title = "Archived chats",
                        subtitle = if (archivedCount == 0) "No archived chats" else "$archivedCount archived chats",
                        onClick = onOpenArchivedChats,
                    )
                    SettingsGroupDivider()
                    SettingsRow(
                        icon = Icons.Rounded.Fingerprint,
                        title = "Security & Biometric",
                        subtitle = "Fingerprint, password",
                        onClick = onOpenSecurity,
                    )
                }
            }
            item(key = "appearance_header") { SettingsSectionHeader("Appearance") }
            item(key = "appearance_card") {
                SettingsAppearanceCard(
                    colorSchemeLabel = colorSchemeLabel,
                    selectedColorSchemeId = selectedColorSchemeId,
                    colorSchemeOptions = colorSchemeOptions,
                    appearanceOptionState = appearanceOptionState,
                    appearanceOptionFlingBehavior = appearanceOptionFlingBehavior,
                    reducedMotion = reducedMotion,
                    themeMode = themeState.themeMode,
                    themeOptions = themeOptions,
                    blackThemeEnabled = themeState.blackThemeEnabled,
                    onSelectColorScheme = { optionId ->
                        if (optionId == "dynamic") {
                            if (!themeState.dynamicColorEnabled) {
                                themeViewModel.toggleDynamicColor()
                            }
                        } else {
                            if (themeState.dynamicColorEnabled) {
                                themeViewModel.toggleDynamicColor()
                            }
                            SerafinaPalette.entries.firstOrNull { it.name == optionId }?.let(themeViewModel::selectPalette)
                        }
                    },
                    onSelectThemeMode = { optionId ->
                        SerafinaThemeMode.entries.firstOrNull { it.name == optionId }?.let(themeViewModel::selectThemeMode)
                    },
                    onToggleBlackTheme = { themeViewModel.setBlackThemeEnabled(!themeState.blackThemeEnabled) },
                )
            }
            item(key = "bottom_spacer") { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ── Settings sub-components ──

@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .height(72.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp))
}

@Composable
private fun SettingsGroupCard(content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) { Column { content() } }
}

@Composable
private fun SettingsGroupDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, subtitle: String? = null, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsAppearanceCard(
    colorSchemeLabel: String,
    selectedColorSchemeId: String,
    colorSchemeOptions: List<SettingsChoiceOption>,
    appearanceOptionState: LazyListState,
    appearanceOptionFlingBehavior: androidx.compose.foundation.gestures.FlingBehavior,
    reducedMotion: Boolean,
    themeMode: SerafinaThemeMode,
    themeOptions: List<SettingsChoiceOption>,
    blackThemeEnabled: Boolean,
    onSelectColorScheme: (String) -> Unit,
    onSelectThemeMode: (String) -> Unit,
    onToggleBlackTheme: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier.background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.06f),
                    ),
                ),
            ),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SettingsExpressiveRow(
                    icon = Icons.Outlined.Palette,
                    title = "Color scheme",
                    subtitle = colorSchemeLabel,
                    reducedMotion = reducedMotion,
                ) {
                    SettingsChoiceRail(
                        options = colorSchemeOptions,
                        selectedId = selectedColorSchemeId,
                        listState = appearanceOptionState,
                        flingBehavior = appearanceOptionFlingBehavior,
                        reducedMotion = reducedMotion,
                        onSelect = onSelectColorScheme,
                    )
                }
                SettingsExpressiveRow(
                    icon = Icons.Outlined.Contrast,
                    title = "Theme",
                    subtitle = themeMode.label,
                    reducedMotion = reducedMotion,
                ) {
                    SettingsChoiceRail(
                        options = themeOptions,
                        selectedId = themeMode.name,
                        reducedMotion = reducedMotion,
                        onSelect = onSelectThemeMode,
                    )
                }
                SettingsExpressiveToggleRow(
                    icon = Icons.Outlined.DarkMode,
                    title = "Black theme only",
                    subtitle = "Use pure black surfaces whenever the app is in dark mode.",
                    checked = blackThemeEnabled,
                    onToggle = onToggleBlackTheme,
                )
            }
        }
    }
}

@Composable
private fun SettingsExpressiveRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    reducedMotion: Boolean,
    controls: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.76f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f),
                shape = RoundedCornerShape(24.dp),
            )
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .animateContentSize(
                animationSpec = if (reducedMotion) {
                    tween(durationMillis = 0)
                } else {
                    spring(
                        stiffness = Spring.StiffnessMedium,
                        dampingRatio = Spring.DampingRatioNoBouncy,
                    )
                },
            ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsExpressiveIcon(icon = icon)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        controls()
    }
}

@Composable
private fun SettingsExpressiveToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    val trackColor = if (checked) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.24f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.76f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f),
                shape = RoundedCornerShape(24.dp),
            )
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsExpressiveIcon(icon = icon)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = trackColor,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = trackColor,
                uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
        )
    }
}

@Composable
private fun SettingsExpressiveIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsChoiceRail(
    options: List<SettingsChoiceOption>,
    selectedId: String,
    reducedMotion: Boolean,
    onSelect: (String) -> Unit,
    listState: LazyListState? = null,
    flingBehavior: androidx.compose.foundation.gestures.FlingBehavior? = null,
) {
    val resolvedListState = listState ?: rememberLazyListState()
    val resolvedFlingBehavior = flingBehavior ?: rememberSnapFlingBehavior(
        lazyListState = resolvedListState,
        snapPosition = SnapPosition.Start,
    )

    LazyRow(
        state = resolvedListState,
        flingBehavior = resolvedFlingBehavior,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.elasticOverscroll(
            enabled = !reducedMotion,
            state = resolvedListState,
            orientation = Orientation.Horizontal,
        ),
    ) {
        items(
            items = options,
            key = { option -> option.id },
            contentType = { "settings_choice" },
        ) { option ->
            SettingsChoicePill(
                option = option,
                selected = option.id == selectedId,
                reducedMotion = reducedMotion,
                onClick = { onSelect(option.id) },
            )
        }
    }
}

@Composable
private fun SettingsChoicePill(
    option: SettingsChoiceOption,
    selected: Boolean,
    reducedMotion: Boolean,
    onClick: () -> Unit,
) {
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.34f)
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.68f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)
    }
    val textColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.985f,
        animationSpec = if (reducedMotion) {
            tween(0)
        } else {
            spring(
                stiffness = Spring.StiffnessMedium,
                dampingRatio = Spring.DampingRatioNoBouncy,
            )
        },
        label = "settings_choice_scale",
    )

    Row(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        option.accentColor?.let { accentColor ->
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accentColor),
            )
        }
        Text(
            text = option.label,
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
        )
    }
}

// ═══════════════════════════════════════════════════════════
// SECURITY SETTINGS
// ═══════════════════════════════════════════════════════════

@Composable
private fun SecuritySettingsScreen(
    themeViewModel: SerafinaThemeViewModel,
    onBack: () -> Unit,
) {
    val themeState by themeViewModel.state.collectAsState()
    val context = LocalContext.current
    val reducedMotion = rememberReducedMotionEnabled()
    val securityListState = rememberLazyListState()
    val securityFlingBehavior = rememberMomentumFlingBehavior(enabled = !reducedMotion)
    var biometricToggleError by rememberSaveable { mutableStateOf<String?>(null) }
    fun requestFingerprintToggle() {
        if (themeState.fingerprintEnabled) {
            biometricToggleError = null
            themeViewModel.setFingerprintEnabled(false)
            return
        }
        val availability = checkBiometricAvailability(context)
        if (availability != BiometricAvailability.Available) {
            biometricToggleError = availability.lockScreenMessage()
            return
        }
        val activity = context.findFragmentActivity()
        if (activity == null) {
            biometricToggleError = "Biometric prompt could not be started."
            return
        }
        biometricToggleError = null
        showBiometricPrompt(
            activity = activity,
            title = "Enable biometric login",
            subtitle = "Authenticate to protect Pulse with biometrics",
        ) { result ->
            when (result) {
                is BiometricAuthResult.Success -> {
                    biometricToggleError = null
                    themeViewModel.setFingerprintEnabled(true)
                }
                is BiometricAuthResult.Cancelled -> Unit
                is BiometricAuthResult.Failed -> {
                    biometricToggleError = "Authentication failed. Try again."
                }
                is BiometricAuthResult.Error -> {
                    biometricToggleError = result.message
                }
            }
        }
    }

    Scaffold(
        topBar = { SettingsTopBar(onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        LazyColumn(
            state = securityListState,
            flingBehavior = securityFlingBehavior,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .elasticOverscroll(
                    enabled = !reducedMotion,
                    state = securityListState,
                ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item(key = "security_header") {
                SettingsSectionHeader("Security & Biometric")
            }
            item(key = "security_card") {
                SettingsGroupCard {
                    SecurityFingerprintRow(
                        enabled = themeState.fingerprintEnabled,
                        error = biometricToggleError,
                        onToggle = ::requestFingerprintToggle,
                    )
                    SettingsGroupDivider()
                    SecurityPasswordSection(
                        passwordSet = themeState.password.isNotEmpty(),
                        onSetPassword = { themeViewModel.setPassword(it) },
                        onClearPassword = { themeViewModel.clearPassword() },
                    )
                }
            }
            item(key = "security_bottom_spacer") { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun SecurityFingerprintRow(
    enabled: Boolean,
    error: String?,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("Fingerprint", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = error ?: if (enabled) "Biometric login active" else "Tap to enable biometric login",
                style = MaterialTheme.typography.bodySmall,
                color = if (error == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
        }
        Switch(checked = enabled, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun SecurityPasswordSection(
    passwordSet: Boolean,
    onSetPassword: (String) -> Unit,
    onClearPassword: () -> Unit,
) {
    var passwordInput by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var isEditing by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Password", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = if (passwordSet) "Alphanumeric password set" else "Set an alphanumeric password",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (isEditing || !passwordSet) {
            OutlinedTextField(
                value = passwordInput,
                onValueChange = { newValue -> passwordInput = newValue.filter { it.isLetterOrDigit() } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Enter password") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (passwordInput.isNotBlank()) { onSetPassword(passwordInput); passwordInput = ""; isEditing = false }
                }),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        )
                    }
                },
                shape = RoundedCornerShape(14.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(
                    onClick = { if (passwordInput.isNotBlank()) { onSetPassword(passwordInput); passwordInput = ""; isEditing = false } },
                    enabled = passwordInput.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("Save") }
                if (passwordSet && isEditing) {
                    FilledTonalButton(
                        onClick = { passwordInput = ""; isEditing = false },
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("Cancel") }
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = { isEditing = true }, shape = RoundedCornerShape(12.dp)) { Text("Change") }
                FilledTonalButton(onClick = { onClearPassword(); passwordInput = "" }, shape = RoundedCornerShape(12.dp)) { Text("Remove") }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// LOCK SCREEN — Biometric authentication gate
// ═══════════════════════════════════════════════════════════

@Composable
private fun LockScreen(
    availability: BiometricAvailability,
    onAuthenticated: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val reducedMotion = rememberReducedMotionEnabled()
    var authError by rememberSaveable { mutableStateOf<String?>(null) }

    // Show the biometric prompt as soon as the lock screen is shown
    LaunchedEffect(availability) {
        if (availability != BiometricAvailability.Available) {
            authError = availability.lockScreenMessage()
            return@LaunchedEffect
        }
        val activity = context.findFragmentActivity() ?: return@LaunchedEffect
        showBiometricPrompt(
            activity = activity,
            title = "Unlock Pulse",
            subtitle = "Authenticate to access your messages",
        ) { result ->
            when (result) {
                is BiometricAuthResult.Success -> onAuthenticated()
                is BiometricAuthResult.Cancelled -> onCancel()
                is BiometricAuthResult.Failed -> {
                    authError = "Authentication failed. Try again."
                }
                is BiometricAuthResult.Error -> {
                    authError = result.message
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Pulse is locked",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = authError ?: "Tap to authenticate",
                    style = MaterialTheme.typography.bodyMedium,
                    color = authError?.let { MaterialTheme.colorScheme.error }
                        ?: MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilledTonalButton(
                    onClick = {
                        val activity = context.findFragmentActivity() ?: return@FilledTonalButton
                        authError = null
                        if (checkBiometricAvailability(context) != BiometricAvailability.Available) {
                            authError = checkBiometricAvailability(context).lockScreenMessage()
                            return@FilledTonalButton
                        }
                        showBiometricPrompt(
                            activity = activity,
                            title = "Unlock Pulse",
                            subtitle = "Authenticate to access your messages",
                        ) { result ->
                            when (result) {
                                is BiometricAuthResult.Success -> onAuthenticated()
                                is BiometricAuthResult.Cancelled -> onCancel()
                                is BiometricAuthResult.Failed -> {
                                    authError = "Authentication failed. Try again."
                                }
                                is BiometricAuthResult.Error -> {
                                    authError = result.message
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Rounded.Fingerprint, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Authenticate")
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// HELPERS
// ═══════════════════════════════════════════════════════════

private fun Instant.toInboxTimestamp(): String = when {
    atZone(ZoneId.systemDefault()).toLocalDate() == java.time.LocalDate.now() ->
        INBOX_TIME_FORMATTER.format(atZone(ZoneId.systemDefault()))
    else -> INBOX_DATE_FORMATTER.format(atZone(ZoneId.systemDefault()))
}

private fun BiometricAvailability.lockScreenMessage(): String = when (this) {
    BiometricAvailability.Available -> "Tap to authenticate"
    BiometricAvailability.NoHardware -> "Strong biometric hardware is not available on this device."
    BiometricAvailability.HardwareUnavailable -> "Strong biometric hardware is temporarily unavailable."
    BiometricAvailability.NoneEnrolled -> "Enroll a strong biometric before using biometric login."
    BiometricAvailability.SecurityUpdateRequired -> "Install the required biometric sensor security update."
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

private sealed interface ConversationTimelineItem {
    val key: String
    val contentType: String

    data class DayDivider(
        override val key: String,
        val label: String,
    ) : ConversationTimelineItem {
        override val contentType: String = "conversation_day_divider"
    }

    data class UnreadDivider(
        val label: String,
        override val key: String,
    ) : ConversationTimelineItem {
        override val contentType: String = "conversation_unread_divider"
    }

    data class Message(
        val message: SystemSms,
    ) : ConversationTimelineItem {
        override val key: String = "conversation_message_${message.id}"
        override val contentType: String = "conversation_message"
    }
}

private fun List<SystemSms>.toConversationTimeline(): List<ConversationTimelineItem> {
    if (isEmpty()) return emptyList()

    val items = ArrayList<ConversationTimelineItem>(size + 4)
    var lastDate: LocalDate? = null
    val unreadMessages = count { it.isInbound && !it.read }
    val firstUnreadMessageId = firstOrNull { it.isInbound && !it.read }?.id

    for (message in this) {
        val localDate = message.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
        if (localDate != lastDate) {
            items += ConversationTimelineItem.DayDivider(
                key = "conversation_day_${localDate}",
                label = localDate.toConversationDayLabel(),
            )
            lastDate = localDate
        }
        if (message.id == firstUnreadMessageId) {
            items += ConversationTimelineItem.UnreadDivider(
                key = "conversation_unread_${message.id}",
                label = if (unreadMessages == 1) "1 unread message"
                else "$unreadMessages unread messages",
            )
        }
        items += ConversationTimelineItem.Message(message)
    }

    return items
}

private fun LocalDate.toConversationDayLabel(today: LocalDate = LocalDate.now()): String = when (this) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    else -> CONVERSATION_DAY_FORMATTER.format(this)
}

private fun Instant.toConversationTime(): String =
    BUBBLE_TIME_FORMATTER.format(atZone(ZoneId.systemDefault()))

private fun String.toAvatarInitials(): String =
    trim()
        .split(" ")
        .filter(String::isNotBlank)
        .take(2)
        .joinToString("") { it.take(1).uppercase() }
        .ifBlank { take(2).uppercase().ifBlank { "#" } }

private fun String.isDirectAddressCandidate(): Boolean {
    if (isBlank()) return false
    return any(Char::isDigit) || contains('@') || any { it == '+' }
}

private fun String.toConversationCategoryLabel(): String =
    if (any(Char::isLetter)) "Business SMS" else "Personal SMS"

private fun String.toConversationMetaLabel(
    totalMessages: Int,
    unreadCount: Int,
    importantCount: Int,
): String {
    val parts = buildList {
        add(toConversationCategoryLabel())
        add("$totalMessages messages")
        if (unreadCount > 0) add("$unreadCount unread")
        if (importantCount > 0) add("$importantCount kept")
    }
    return parts.joinToString(" · ")
}

private val INBOX_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val INBOX_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")
private val CONVERSATION_DAY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")
private val BUBBLE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
