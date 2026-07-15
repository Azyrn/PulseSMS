@file:OptIn(ExperimentalMaterial3Api::class)

package com.skeler.pulse.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.MarkunreadMailbox
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.skeler.pulse.InboxAccessState
import com.skeler.pulse.contact.displayNameFor
import com.skeler.pulse.design.component.SerafinaAvatar
import com.skeler.pulse.design.component.SerafinaProgressIndicator
import com.skeler.pulse.design.component.StatusPill
import com.skeler.pulse.design.util.elasticOverscroll
import com.skeler.pulse.design.util.motionAnimateItemModifier
import com.skeler.pulse.design.util.rememberEntranceModifier
import com.skeler.pulse.design.util.rememberReducedMotionEnabled
import com.skeler.pulse.design.util.rememberSmoothFlingBehavior
import com.skeler.pulse.sms.SmsThread

internal enum class InboxFilter(val label: String) {
    All("All"), Personal("Personal"), Business("Business"), OTP("OTP"),
}

private const val InboxBackgroundTintAlpha = 0.42f
private const val InboxBackgroundTintEndFraction = 0.34f
private val NewChatFabDefaultElevation = 1.dp
private val NewChatFabPressedElevation = 1.5.dp

@Composable
internal fun RealInboxScreen(
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
    onBlockThread: (String) -> Unit,
    onDeleteThread: (Long?, String) -> Unit,
) {
    var selectedFilter by rememberSaveable { mutableIntStateOf(0) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var contextMenuThreadId by rememberSaveable { mutableStateOf<Long?>(null) }
    val context = LocalContext.current
    val reducedMotion = rememberReducedMotionEnabled()
    val listFlingBehavior = rememberSmoothFlingBehavior(enabled = !reducedMotion)
    val filterFlingBehavior = rememberSmoothFlingBehavior(enabled = !reducedMotion)
    val colors = MaterialTheme.colorScheme
    val inboxBackdropBrush = colors.screenBackgroundBrush {
        Brush.verticalGradient(
            0f to colors.surfaceContainerLow.copy(alpha = InboxBackgroundTintAlpha),
            InboxBackgroundTintEndFraction to colors.surface,
            1f to colors.surface,
        )
    }

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
        modifier = Modifier.background(brush = inboxBackdropBrush),
        containerColor = Color.Transparent,
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
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = NewChatFabDefaultElevation,
                    pressedElevation = NewChatFabPressedElevation,
                    focusedElevation = NewChatFabDefaultElevation,
                    hoveredElevation = NewChatFabPressedElevation,
                ),
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = inboxBackdropBrush),
        ) {
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
                            onBlock = { onBlockThread(thread.address) },
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
