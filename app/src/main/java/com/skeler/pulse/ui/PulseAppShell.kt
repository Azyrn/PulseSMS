@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.skeler.pulse.ui

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skeler.pulse.design.component.SerafinaAvatar
import com.skeler.pulse.design.theme.SerafinaPalette
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

private enum class PulseScreen { Inbox, Conversation, Settings }

private enum class InboxFilter(val label: String) {
    All("All"), Personal("Personal"), Business("Business"), OTP("OTP"),
}

@Composable
fun PulseAppShell(
    inboxState: RealInboxState,
    conversationState: RealConversationState,
    onOpenConversation: (String) -> Unit,
    onSendMessage: (String, String) -> Unit,
    themeViewModel: SerafinaThemeViewModel,
    onRequestDefaultSms: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var currentScreen by rememberSaveable { mutableStateOf(PulseScreen.Inbox) }
    var activeAddress by rememberSaveable { mutableStateOf("") }
    val reducedMotion = rememberReducedMotionEnabled()
    val inboxListState = rememberLazyListState()
    val inboxFilterState = rememberLazyListState()
    val settingsListState = rememberLazyListState()

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                if (reducedMotion) {
                    fadeIn(tween(durationMillis = 0)) togetherWith fadeOut(tween(durationMillis = 0))
                } else {
                val dur = 250
                when {
                    targetState == PulseScreen.Conversation && initialState == PulseScreen.Inbox ->
                        (slideInHorizontally(tween(dur)) { it / 4 } + fadeIn(tween(dur)))
                            .togetherWith(slideOutHorizontally(tween(dur)) { -it / 4 } + fadeOut(tween(dur)))
                    targetState == PulseScreen.Inbox && initialState == PulseScreen.Conversation ->
                        (slideInHorizontally(tween(dur)) { -it / 4 } + fadeIn(tween(dur)))
                            .togetherWith(slideOutHorizontally(tween(dur)) { it / 4 } + fadeOut(tween(dur)))
                    else -> fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                }
                }
            },
            label = "screen_transition",
        ) { screen ->
            when (screen) {
                PulseScreen.Inbox -> RealInboxScreen(
                    threads = inboxState.threads,
                    loading = inboxState.loading,
                    listState = inboxListState,
                    filterState = inboxFilterState,
                    onOpenConversation = { address ->
                        activeAddress = address
                        onOpenConversation(address)
                        currentScreen = PulseScreen.Conversation
                    },
                    onOpenSettings = { currentScreen = PulseScreen.Settings },
                )
                PulseScreen.Conversation -> RealConversationScreen(
                    address = activeAddress,
                    messages = if (conversationState.address == activeAddress) {
                        conversationState.messages
                    } else {
                        emptyList()
                    },
                    onBack = { currentScreen = PulseScreen.Inbox },
                    onSend = { body -> onSendMessage(activeAddress, body) },
                )
                PulseScreen.Settings -> SettingsScreen(
                    themeViewModel = themeViewModel,
                    listState = settingsListState,
                    onBack = { currentScreen = PulseScreen.Inbox },
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
    threads: List<SmsThread>,
    loading: Boolean,
    listState: LazyListState,
    filterState: LazyListState,
    onOpenConversation: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var selectedFilter by remember { mutableIntStateOf(0) }
    val reducedMotion = rememberReducedMotionEnabled()
    val listFlingBehavior = rememberSmoothFlingBehavior(enabled = !reducedMotion)
    val filterFlingBehavior = rememberSmoothFlingBehavior(enabled = !reducedMotion)

    val filteredThreads = remember(threads, selectedFilter) {
        when (InboxFilter.entries[selectedFilter]) {
            InboxFilter.All -> threads
            InboxFilter.OTP -> threads.filter { t ->
                t.snippet.contains("code", true) || t.snippet.contains("OTP", true) ||
                    t.snippet.contains("verification", true) || t.snippet.contains("verify", true)
            }
            InboxFilter.Business -> threads.filter { t -> t.address.any { it.isLetter() } }
            InboxFilter.Personal -> threads.filter { t -> t.address.all { it.isDigit() || it == '+' || it == ' ' } }
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
            if (loading) {
                item { Text("Loading messages…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else if (filteredThreads.isEmpty()) {
                item { Text("No messages", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(
                    items = filteredThreads,
                    key = { it.address },
                    contentType = { "inbox_thread" },
                ) { thread ->
                    val itemModifier = motionAnimateItemModifier(reducedMotion)
                        .then(rememberEntranceModifier(thread.address, reducedMotion))
                    SmsThreadCard(
                        thread = thread,
                        onClick = { onOpenConversation(thread.address) },
                        modifier = itemModifier,
                    )
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
    val initials = thread.address.take(2).uppercase()
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            SerafinaAvatar(imageUrl = null, initials = initials, hasUnread = thread.unreadCount > 0, size = 48.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = thread.address,
                    style = if (thread.unreadCount > 0) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    else MaterialTheme.typography.titleMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(thread.snippet, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(thread.timestamp.toInboxTimestamp(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (thread.unreadCount > 0) {
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
private fun RealConversationScreen(
    address: String,
    messages: List<SystemSms>,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val reducedMotion = rememberReducedMotionEnabled()
    val listFlingBehavior = rememberSmoothFlingBehavior(enabled = !reducedMotion)
    var draft by rememberSaveable(address) { mutableStateOf("") }
    var previousMessageCount by remember(address) { mutableIntStateOf(0) }

    val timelineItems = remember(messages) { messages.toConversationTimeline() }
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
                            initials = address.toAvatarInitials(),
                            hasUnread = messages.lastOrNull()?.let { it.isInbound && !it.read } == true,
                            size = 40.dp,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = address,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${address.toConversationCategoryLabel()} · ${messages.size} messages",
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
            ConversationComposer(
                draft = draft,
                onDraftChange = { draft = it },
                onSend = {
                    val message = draft.trim()
                    if (message.isEmpty()) return@ConversationComposer
                    onSend(message)
                    draft = ""
                },
            )
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
                        address = address,
                        messageCount = messages.size,
                        latestTimestamp = messages.lastOrNull()?.timestamp,
                        modifier = rememberEntranceModifier("conversation_header_$address", reducedMotion),
                    )
                }

                if (timelineItems.isEmpty()) {
                    item(key = "conversation_empty") {
                        EmptyConversationState(
                            address = address,
                            modifier = rememberEntranceModifier("conversation_empty_$address", reducedMotion),
                        )
                    }
                } else {
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

                            is ConversationTimelineItem.Message -> {
                                ConversationMessageBubble(
                                    message = item.message,
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

@Composable
private fun ConversationOverviewCard(
    address: String,
    messageCount: Int,
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
                initials = address.toAvatarInitials(),
                size = 52.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = address,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${address.toConversationCategoryLabel()} · ${messageCount.coerceAtLeast(0)} total",
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
    modifier: Modifier = Modifier,
) {
    val isOutbound = message.isOutbound
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
                shape = bubbleShape,
                color = if (isOutbound) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = if (isOutbound) 0.dp else 1.dp,
            ) {
                Text(
                    text = message.body.ifBlank { " " },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isOutbound) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = message.timestamp.toConversationTime(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ConversationComposer(
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canSend by remember(draft) {
        derivedStateOf { draft.isNotBlank() }
    }
    val sendWidth by animateDpAsState(
        targetValue = if (canSend) 104.dp else 56.dp,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioMediumBouncy,
        ),
        label = "conversation_send_width",
    )
    val sendScale by animateFloatAsState(
        targetValue = if (canSend) 1f else 0.94f,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioNoBouncy,
        ),
        label = "conversation_send_scale",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
                textStyle = MaterialTheme.typography.bodyLarge,
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
                placeholder = {
                    Text(
                        text = "Write a message",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                maxLines = 5,
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedIndicatorColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    disabledIndicatorColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            )

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = sendScale
                        scaleY = sendScale
                    }
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        color = if (canSend) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                    .clickable(enabled = canSend, onClick = onSend)
                    .padding(horizontal = 14.dp, vertical = 14.dp)
                    .width(sendWidth),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Send message",
                        tint = if (canSend) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (canSend) {
                        Text(
                            text = "Send",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyConversationState(
    address: String,
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
                text = "Start a cleaner thread with $address using the composer below.",
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
    val paletteListState = rememberLazyListState()
    val paletteFlingBehavior = rememberMomentumFlingBehavior(enabled = !reducedMotion)

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
                SettingsGroupCard {
                    SettingsToggleRow(icon = Icons.Outlined.Palette, label = "Dynamic color", checked = themeState.dynamicColorEnabled, onCheckedChange = { themeViewModel.toggleDynamicColor() })
                    SettingsGroupDivider()
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text("Color palette", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(10.dp))
                        LazyRow(
                            state = paletteListState,
                            flingBehavior = paletteFlingBehavior,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.elasticOverscroll(
                                enabled = !reducedMotion,
                                state = paletteListState,
                                orientation = Orientation.Horizontal,
                            ),
                        ) {
                            items(
                                count = SerafinaPalette.entries.size,
                                key = { index -> "palette_${SerafinaPalette.entries[index].name}" },
                                contentType = { "palette_chip" },
                            ) { index ->
                                val palette = SerafinaPalette.entries[index]
                                val isSelected = themeState.selectedPalette == palette
                                Column(
                                    modifier = motionAnimateItemModifier(reducedMotion)
                                        .then(rememberEntranceModifier(palette.name, reducedMotion)),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(palette.seedColor)
                                        .then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                                        .clickable { themeViewModel.selectPalette(palette) })
                                    Text(palette.label, style = MaterialTheme.typography.labelSmall, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
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
private fun SettingsToggleRow(icon: ImageVector, label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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

    for (message in this) {
        val localDate = message.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
        if (localDate != lastDate) {
            items += ConversationTimelineItem.DayDivider(
                key = "conversation_day_${localDate}",
                label = localDate.toConversationDayLabel(),
            )
            lastDate = localDate
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

private fun String.toConversationCategoryLabel(): String =
    if (any(Char::isLetter)) "Business SMS" else "Personal SMS"

private val INBOX_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val INBOX_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")
private val CONVERSATION_DAY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")
private val BUBBLE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
