@file:OptIn(ExperimentalMaterial3Api::class)

package com.skeler.pulse.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import com.skeler.pulse.design.util.elasticOverscroll
import com.skeler.pulse.design.util.motionAnimateItemModifier
import com.skeler.pulse.design.util.rememberEntranceModifier
import com.skeler.pulse.design.util.rememberReducedMotionEnabled
import com.skeler.pulse.design.util.rememberSmoothFlingBehavior
import com.skeler.pulse.sms.SmsThread

@Composable
internal fun ArchivedChatsScreen(
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
    onBlockThread: (String) -> Unit,
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
                            onBlock = { onBlockThread(thread.address) },
                            onDelete = { onDeleteThread(thread.threadId, thread.address) },
                            modifier = if (isMenuOpenForThread) itemModifier.then(Modifier.zIndex(2f)) else itemModifier,
                        )
                    }
                }
            }

        }
    }
}
