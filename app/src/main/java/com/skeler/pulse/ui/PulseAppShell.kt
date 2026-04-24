@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.skeler.pulse.ui

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DarkMode
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skeler.pulse.design.component.SerafinaAvatar
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

private enum class PulseScreen { Inbox, Conversation, Settings }

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
    val reducedMotion = rememberReducedMotionEnabled()
    val canSend by remember(draft) {
        derivedStateOf { draft.isNotBlank() }
    }
    val fieldInteractionSource = remember { MutableInteractionSource() }
    val isFocused by fieldInteractionSource.collectIsFocusedAsState()
    val sendInteractionSource = remember { MutableInteractionSource() }
    val isSendPressed by sendInteractionSource.collectIsPressedAsState()
    val isActivated by remember(draft, isFocused) {
        derivedStateOf { draft.isNotBlank() || isFocused }
    }
    val quickDuration = if (reducedMotion) 0 else 180
    val exitDuration = if (reducedMotion) 0 else 120
    val containerOffsetY by animateFloatAsState(
        targetValue = if (isFocused) -3f else 0f,
        animationSpec = tween(durationMillis = quickDuration),
        label = "conversation_composer_offset",
    )
    val accentHeight by animateDpAsState(
        targetValue = if (isFocused) 30.dp else 18.dp,
        animationSpec = tween(durationMillis = quickDuration),
        label = "conversation_composer_accent_height",
    )
    val outerBorderColor by animateColorAsState(
        targetValue = if (isFocused) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)
        },
        animationSpec = tween(durationMillis = quickDuration),
        label = "conversation_composer_border",
    )
    val fieldBorderColor by animateColorAsState(
        targetValue = if (isFocused) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)
        },
        animationSpec = tween(durationMillis = quickDuration),
        label = "conversation_field_border",
    )
    val placeholderAlpha by animateFloatAsState(
        targetValue = if (draft.isEmpty()) 1f else 0f,
        animationSpec = tween(durationMillis = exitDuration),
        label = "conversation_placeholder_alpha",
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (isActivated) 1f else 0f,
        animationSpec = tween(durationMillis = quickDuration),
        label = "conversation_label_alpha",
    )
    val labelTranslationY by animateFloatAsState(
        targetValue = if (isActivated) 0f else 8f,
        animationSpec = tween(durationMillis = quickDuration),
        label = "conversation_label_translation",
    )
    val sendWidth by animateDpAsState(
        targetValue = if (canSend) 112.dp else 58.dp,
        animationSpec = spring(
            stiffness = if (reducedMotion) Spring.StiffnessHigh else Spring.StiffnessMedium,
            dampingRatio = Spring.DampingRatioNoBouncy,
        ),
        label = "conversation_send_width",
    )
    val sendScale by animateFloatAsState(
        targetValue = when {
            isSendPressed && canSend -> 0.96f
            canSend -> 1f
            else -> 0.94f
        },
        animationSpec = tween(durationMillis = quickDuration),
        label = "conversation_send_scale",
    )
    val sendContainerColor by animateColorAsState(
        targetValue = if (canSend) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = tween(durationMillis = quickDuration),
        label = "conversation_send_container",
    )
    val sendContentColor by animateColorAsState(
        targetValue = if (canSend) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = quickDuration),
        label = "conversation_send_content",
    )
    val sendIconOffsetX by animateFloatAsState(
        targetValue = if (canSend) 0f else 6f,
        animationSpec = tween(durationMillis = quickDuration),
        label = "conversation_send_icon_offset",
    )
    val composerShape = RoundedCornerShape(30.dp)
    val fieldShape = RoundedCornerShape(26.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .graphicsLayer { translationY = containerOffsetY }
            .border(width = 1.dp, color = outerBorderColor, shape = composerShape),
        shape = composerShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = if (canSend) 0.10f else 0.04f),
                        ),
                    ),
                ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .size(width = 4.dp, height = accentHeight)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.90f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.72f),
                                ),
                            ),
                        ),
                )

                BasicTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    modifier = Modifier.weight(1f),
                    interactionSource = fieldInteractionSource,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
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
                    maxLines = 5,
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(fieldShape)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.22f))
                                .border(width = 1.dp, color = fieldBorderColor, shape = fieldShape)
                                .heightIn(min = 58.dp)
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                        ) {
                            Text(
                                text = "Message",
                                modifier = Modifier.graphicsLayer {
                                    alpha = labelAlpha
                                    translationY = labelTranslationY
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.88f),
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = if (isActivated) 16.dp else 0.dp),
                            ) {
                                if (draft.isEmpty()) {
                                    Text(
                                        text = "Write a message",
                                        modifier = Modifier.graphicsLayer { alpha = placeholderAlpha },
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        ),
                                    )
                                }
                                innerTextField()
                            }
                        }
                    },
                )

                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = sendScale
                            scaleY = sendScale
                        }
                        .clip(RoundedCornerShape(28.dp))
                        .background(sendContainerColor)
                        .clickable(
                            interactionSource = sendInteractionSource,
                            enabled = canSend,
                            onClick = onSend,
                        )
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                        .width(sendWidth),
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedContent(
                        targetState = canSend,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(durationMillis = quickDuration)) togetherWith
                                fadeOut(animationSpec = tween(durationMillis = exitDuration))
                        },
                        label = "conversation_send_content",
                    ) { isEnabled ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = "Send message",
                                modifier = Modifier.graphicsLayer { translationX = sendIconOffsetX },
                                tint = sendContentColor,
                            )
                            if (isEnabled) {
                                Text(
                                    text = "Send",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = sendContentColor,
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
