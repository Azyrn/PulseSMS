@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)

package com.skeler.pulse.ui

import com.skeler.pulse.InboxAccessState
import com.skeler.pulse.PulseLaunchRequest
import com.skeler.pulse.contact.displayNameFor
import com.skeler.pulse.shouldHandleLaunchRequest
import com.skeler.pulse.shouldHandleOpenNewChatRequest

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.MarkunreadMailbox
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val DESTINATION_INBOX = "inbox"
private const val DESTINATION_NEW_CHAT = "new_chat"
private const val DESTINATION_CONVERSATION = "conversation"
private const val DESTINATION_SETTINGS = "settings"

private enum class InboxFilter(val label: String) {
    All("All"), Personal("Personal"), Business("Business"), OTP("OTP"),
}

private data class SettingsChoiceOption(
    val id: String,
    val label: String,
    val accentColor: Color? = null,
)

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
    onToggleImportantMessage: (Long) -> Unit,
    themeViewModel: SerafinaThemeViewModel,
    onRequestDefaultSms: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var backStack by rememberSaveable { mutableStateOf(listOf(DESTINATION_INBOX)) }
    var activeAddress by rememberSaveable { mutableStateOf("") }
    var activeConversationTitle by rememberSaveable { mutableStateOf("") }
    var activeSubscriptionId by rememberSaveable { mutableStateOf<Int?>(null) }
    var conversationDraftSeed by rememberSaveable { mutableStateOf("") }
    var newChatQuery by rememberSaveable { mutableStateOf("") }
    var lastHandledNewChatRequestKey by rememberSaveable { mutableIntStateOf(0) }
    val context = LocalContext.current
    val currentScreen = backStack.lastOrNull() ?: DESTINATION_INBOX
    val reducedMotion = rememberReducedMotionEnabled()
    val inboxListState = rememberLazyListState()
    val inboxFilterState = rememberLazyListState()
    val settingsListState = rememberLazyListState()
    val newChatListState = rememberLazyListState()

    LaunchedEffect(launchRequest, accessState) {
        if (!shouldHandleLaunchRequest(launchRequest, accessState)) return@LaunchedEffect
        val request = launchRequest ?: return@LaunchedEffect
        val requestedAddress = request.conversationAddress
        if (requestedAddress.isNotBlank()) {
            activeAddress = requestedAddress
            activeConversationTitle = request.conversationTitle.ifBlank { displayNameFor(context, requestedAddress) }
            activeSubscriptionId = null
            conversationDraftSeed = request.draftBody
            onOpenConversation(requestedAddress, null)
            backStack = listOf(DESTINATION_INBOX, DESTINATION_CONVERSATION)
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

    BackHandler(enabled = currentScreen != DESTINATION_INBOX) {
        backStack = backStack.dropLast(1).ifEmpty { listOf(DESTINATION_INBOX) }
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                if (reducedMotion) {
                    fadeIn(tween(durationMillis = 0)) togetherWith fadeOut(tween(durationMillis = 0))
                } else {
                val dur = 250
                when {
                    targetState == DESTINATION_CONVERSATION && initialState == DESTINATION_INBOX ->
                        (slideInHorizontally(tween(dur)) { it } + fadeIn(tween(dur)))
                            .togetherWith(slideOutHorizontally(tween(dur)) { -it } + fadeOut(tween(dur)))
                    targetState == DESTINATION_INBOX && initialState == DESTINATION_CONVERSATION ->
                        (slideInHorizontally(tween(dur)) { -it } + fadeIn(tween(dur)))
                            .togetherWith(slideOutHorizontally(tween(dur)) { it } + fadeOut(tween(dur)))
                    targetState == DESTINATION_NEW_CHAT && initialState == DESTINATION_INBOX ->
                        (slideInHorizontally(tween(dur)) { it } + fadeIn(tween(dur)))
                            .togetherWith(slideOutHorizontally(tween(dur)) { -it } + fadeOut(tween(dur)))
                    targetState == DESTINATION_INBOX && initialState == DESTINATION_NEW_CHAT ->
                        (slideInHorizontally(tween(dur)) { -it } + fadeIn(tween(dur)))
                            .togetherWith(slideOutHorizontally(tween(dur)) { it } + fadeOut(tween(dur)))
                    targetState == DESTINATION_CONVERSATION && initialState == DESTINATION_NEW_CHAT ->
                        (slideInHorizontally(tween(dur)) { it } + fadeIn(tween(dur)))
                            .togetherWith(slideOutHorizontally(tween(dur)) { -it } + fadeOut(tween(dur)))
                    targetState == DESTINATION_NEW_CHAT && initialState == DESTINATION_CONVERSATION ->
                        (slideInHorizontally(tween(dur)) { -it } + fadeIn(tween(dur)))
                            .togetherWith(slideOutHorizontally(tween(dur)) { it } + fadeOut(tween(dur)))
                    else -> fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                }
                }
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
                            onOpenSettings = {
                                backStack = listOf(DESTINATION_INBOX, DESTINATION_SETTINGS)
                            },
                            onOpenNewChat = onRequestNewChat,
                            onRefreshInbox = smsViewModel::refreshInbox,
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
                            conversationDraftSeed = ""
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
                            backStack = backStack.dropLast(1).ifEmpty { listOf(DESTINATION_INBOX) }
                        },
                        onSend = { body ->
                            onSendMessage(activeAddress, body, activeSubscriptionId)
                        },
                        onRetrySend = smsViewModel::retrySend,
                        onClearSendState = smsViewModel::clearSendState,
                        onDraftConsumed = { conversationDraftSeed = "" },
                        onToggleImportantMessage = onToggleImportantMessage,
                    )
                }

                DESTINATION_SETTINGS -> SettingsScreen(
                    themeViewModel = themeViewModel,
                    listState = settingsListState,
                    onBack = {
                        backStack = backStack.dropLast(1).ifEmpty { listOf(DESTINATION_INBOX) }
                    },
                    onRequestDefaultSms = onRequestDefaultSms,
                )
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
    onOpenSettings: () -> Unit,
    onOpenNewChat: () -> Unit,
    onRefreshInbox: () -> Unit,
) {
    var selectedFilter by remember { mutableIntStateOf(0) }
    val reducedMotion = rememberReducedMotionEnabled()
    val listFlingBehavior = rememberSmoothFlingBehavior(enabled = !reducedMotion)
    val filterFlingBehavior = rememberSmoothFlingBehavior(enabled = !reducedMotion)

    val filteredThreads = remember(state.threads, selectedFilter) {
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
                actions = {
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
                            selected = selectedFilter == index, onClick = { selectedFilter = index },
                            label = { Text(filter.label) }, shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }
            }
            when {
                state.loading -> {
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
                state.threads.isEmpty() -> {
                    item(key = "inbox_empty") {
                        InboxEmptyStateCard(onOpenNewChat = onOpenNewChat)
                    }
                }
                filteredThreads.isEmpty() -> {
                    item(key = "inbox_filtered_empty") {
                        InboxFilteredEmptyStateCard(
                            activeFilter = InboxFilter.entries[selectedFilter].label,
                            onShowAll = { selectedFilter = InboxFilter.All.ordinal },
                        )
                    }
                }
                else -> {
                    items(
                        items = filteredThreads,
                        key = { it.address },
                        contentType = { "inbox_thread" },
                    ) { thread ->
                        val itemModifier = motionAnimateItemModifier(reducedMotion)
                            .then(rememberEntranceModifier(thread.address, reducedMotion))
                        SmsThreadCard(
                            thread = thread,
                            onClick = { onOpenConversation(thread.address, thread.threadId) },
                            modifier = itemModifier,
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val displayName = remember(thread.address) { displayNameFor(context, thread.address) }
    val initials = displayName.toAvatarInitials()
    val hasUnread = thread.unreadCount > 0
    val containerColor by animateColorAsState(
        targetValue = if (hasUnread) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        label = "thread_card_container",
    )
    val outlineColor by animateColorAsState(
        targetValue = if (hasUnread) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f)
        },
        label = "thread_card_outline",
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
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = semanticsLabel
            }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
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
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = displayName,
                    style = if (hasUnread) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    else MaterialTheme.typography.titleMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
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
    messages: List<SystemSms>,
    loading: Boolean,
    importantMessageIds: Set<Long>,
    sendState: SendState,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    onRetrySend: () -> Unit,
    onClearSendState: () -> Unit,
    onDraftConsumed: () -> Unit,
    onToggleImportantMessage: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val reducedMotion = rememberReducedMotionEnabled()
    val listFlingBehavior = rememberSmoothFlingBehavior(enabled = !reducedMotion)
    var draft by rememberSaveable(address) { mutableStateOf("") }
    var previousMessageCount by remember(address) { mutableIntStateOf(0) }

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
    val isNearEnd by remember(listState) {
        derivedStateOf { listState.isNearListEnd() }
    }

    LaunchedEffect(address) {
        previousMessageCount = messages.size
        if (timelineItems.isNotEmpty()) {
            listState.scrollToItem(timelineItems.lastIndex)
        }
    }

    LaunchedEffect(messages.size, timelineItems.size) {
        if (timelineItems.isEmpty()) {
            previousMessageCount = 0
            return@LaunchedEffect
        }

        val listGrew = messages.size > previousMessageCount
        if (listGrew && isNearEnd) {
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
                        sendState = sendState,
                        onRetrySend = onRetrySend,
                    )
                    ConversationComposer(
                        draft = draft,
                        sendState = sendState,
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
                                        onToggleImportant = { onToggleImportantMessage(item.message.id) },
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
    onToggleImportant: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isOutbound = message.isOutbound
    val isUnread = message.isInbound && !message.read
    val bubbleShape = RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 24.dp,
        bottomStart = if (isOutbound) 24.dp else 10.dp,
        bottomEnd = if (isOutbound) 10.dp else 24.dp,
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isOutbound) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 320.dp),
            horizontalAlignment = if (isOutbound) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Surface(
                modifier = Modifier.then(
                    if (isUnread && !isOutbound) {
                        Modifier.border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.42f),
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
                tonalElevation = if (isOutbound) 0.dp else 1.dp,
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
                FilledTonalIconButton(
                    onClick = onToggleImportant,
                    modifier = Modifier.size(30.dp),
                ) {
                    Icon(
                        imageVector = if (isImportant) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = if (isImportant) "Remove important marker" else "Keep message marked",
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationComposer(
    draft: String,
    sendState: SendState,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val isSending = sendState is SendState.Sending
    val canSend by remember(draft, isSending) {
        derivedStateOf { draft.isNotBlank() && !isSending }
    }
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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .graphicsLayer { translationY = containerLift },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        // ── Text input pill ──
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

        // ── Send button (hero moment) ──
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
        targetValue = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
        label = "read_only_notice_border",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(28.dp),
            )
            .semantics {
                contentDescription = "Read only business sender. Replies are disabled."
            },
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier.background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f),
                    ),
                ),
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Outlined.Sms,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Read only",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "This sender doesn't accept replies",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Business sender",
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.76f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "SMS",
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
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
    onBack: () -> Unit,
    onRequestDefaultSms: () -> Unit,
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
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.Close, contentDescription = "Close") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
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
                    SettingsRow(icon = Icons.Outlined.Sms, title = "Default SMS app", subtitle = "Tap to set Pulse as default", onClick = onRequestDefaultSms)
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
            .animateContentSize(),
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
    val trackColor by animateColorAsState(
        targetValue = if (checked) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.24f)
        },
        label = "settings_toggle_track",
    )

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
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.34f)
        },
        label = "settings_choice_background",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.68f)
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)
        },
        label = "settings_choice_border",
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "settings_choice_text",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.985f,
        animationSpec = if (reducedMotion) tween(0) else spring(stiffness = Spring.StiffnessMediumLow),
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
// HELPERS
// ═══════════════════════════════════════════════════════════

private fun Instant.toInboxTimestamp(): String = when {
    atZone(ZoneId.systemDefault()).toLocalDate() == java.time.LocalDate.now() ->
        INBOX_TIME_FORMATTER.format(atZone(ZoneId.systemDefault()))
    else -> INBOX_DATE_FORMATTER.format(atZone(ZoneId.systemDefault()))
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
