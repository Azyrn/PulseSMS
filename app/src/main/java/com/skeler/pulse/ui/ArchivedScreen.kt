@file:OptIn(ExperimentalMaterial3Api::class)

package com.skeler.pulse.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MarkunreadMailbox
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import androidx.activity.compose.BackHandler
import com.skeler.pulse.R
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
    drafts: Map<String, String> = emptyMap(),
    scheduledAddresses: Set<String> = emptySet(),
    listState: LazyListState,
    onBack: () -> Unit,
    onOpenConversation: (String, Long?) -> Unit,
    onRefreshInbox: () -> Unit,
    onTogglePinned: (Long) -> Unit,
    onSetPinned: (Long, Boolean) -> Unit,
    onToggleArchived: (Long) -> Unit,
    onSetArchived: (Long, Boolean) -> Unit,
    onSetThreadUnread: (Long?, String, Boolean) -> Unit,
    onBlockThread: (String) -> Unit,
    onDeleteThread: (Long?, String) -> Unit,
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val listFlingBehavior = rememberSmoothFlingBehavior(enabled = !reducedMotion)
    var contextMenuThreadId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedThreadIds by rememberSaveable { mutableStateOf<Set<Long>>(emptySet()) }
    var showBatchDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    val isSelectionMode = selectedThreadIds.isNotEmpty()
    val selectedThreads = remember(selectedThreadIds, threads) {
        threads.filter { it.threadId in selectedThreadIds }
    }
    val allFilteredSelected = remember(selectedThreadIds, threads) {
        threads.isNotEmpty() && threads.all { it.threadId in selectedThreadIds }
    }
    val allPinned = remember(selectedThreads, pinnedThreadIds) {
        selectedThreads.isNotEmpty() && selectedThreads.all { it.threadId in pinnedThreadIds }
    }

    val shouldDismissMenu by remember {
        derivedStateOf {
            val id = contextMenuThreadId ?: return@derivedStateOf false
            threads.none { it.threadId == id }
        }
    }
    LaunchedEffect(shouldDismissMenu) {
        if (shouldDismissMenu) contextMenuThreadId = null
    }

    if (isSelectionMode) {
        BackHandler { selectedThreadIds = emptySet() }
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                val selectedLabel = context.resources.getQuantityString(
                    R.plurals.conversation_selected_count,
                    selectedThreadIds.size,
                    selectedThreadIds.size,
                )
                Surface(
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .statusBarsPadding()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { selectedThreadIds = emptySet() }) {
                            Icon(Icons.Rounded.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(
                            selectedLabel,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = {
                            selectedThreadIds = if (allFilteredSelected) {
                                emptySet()
                            } else {
                                threads.map { it.threadId }.toSet()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = if (allFilteredSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = {
                            selectedThreadIds.forEach { onSetArchived(it, false) }
                            selectedThreadIds = emptySet()
                        }) {
                            Icon(Icons.Rounded.Unarchive, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(onClick = { showBatchDeleteConfirmation = true }) {
                            Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        }
                        var showOverflow by remember { mutableStateOf(false) }
                        IconButton(onClick = { showOverflow = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Box {
                            DropdownMenu(
                                expanded = showOverflow,
                                onDismissRequest = { showOverflow = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(if (allPinned) R.string.thread_unpin else R.string.thread_pin)) },
                                    onClick = {
                                        showOverflow = false
                                        val pinned = !allPinned
                                        selectedThreadIds.forEach { onSetPinned(it, pinned) }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (allPinned) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                            contentDescription = null,
                                        )
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.thread_mark_read)) },
                                    onClick = {
                                        showOverflow = false
                                        selectedThreads.forEach { onSetThreadUnread(it.threadId, it.address, false) }
                                        selectedThreadIds = emptySet()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.MarkunreadMailbox, contentDescription = null)
                                    },
                                )
                            }
                        }
                    }
                }
            } else {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings_archived_chats), style = MaterialTheme.typography.headlineMedium) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = stringResource(R.string.action_back))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            }
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
                            title = stringResource(R.string.settings_no_archived_chats),
                            body = stringResource(R.string.archived_empty_body),
                            statusLabel = stringResource(R.string.archived_empty_status),
                            actionLabel = stringResource(R.string.archived_back_to_inbox),
                            icon = Icons.Rounded.Archive,
                            onAction = onBack,
                        )
                    }
                }
                else -> {
                    items(
                        items = threads,
                        key = { it.threadId },
                        contentType = { "archived_thread" },
                    ) { thread ->
                        val itemModifier = motionAnimateItemModifier(reducedMotion)
                            .then(rememberEntranceModifier("archived_${thread.threadId}_${thread.address}", reducedMotion))
                        val isMenuOpenForThread = !isSelectionMode && contextMenuThreadId == thread.threadId
                        SmsThreadCard(
                            thread = thread,
                            isPinned = thread.threadId in pinnedThreadIds,
                            isArchived = thread.threadId in archivedThreadIds,
                            isContextMenuOpen = isMenuOpenForThread,
                            isSelected = thread.threadId in selectedThreadIds,
                            draft = drafts[thread.address].orEmpty(),
                            scheduled = thread.address in scheduledAddresses,
                            onClick = {
                                if (isSelectionMode) {
                                    selectedThreadIds = if (thread.threadId in selectedThreadIds) {
                                        selectedThreadIds - thread.threadId
                                    } else {
                                        selectedThreadIds + thread.threadId
                                    }
                                } else {
                                    onOpenConversation(thread.address, thread.threadId)
                                }
                            },
                            onLongPress = {
                                if (isSelectionMode) {
                                    selectedThreadIds = if (thread.threadId in selectedThreadIds) {
                                        selectedThreadIds - thread.threadId
                                    } else {
                                        selectedThreadIds + thread.threadId
                                    }
                                } else {
                                    contextMenuThreadId = null
                                    selectedThreadIds = setOf(thread.threadId)
                                }
                            },
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

    if (showBatchDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirmation = false },
            title = { Text(stringResource(R.string.thread_delete_title)) },
            text = {
                Text(context.resources.getQuantityString(
                    R.plurals.conversation_delete_body,
                    selectedThreadIds.size,
                    selectedThreadIds.size,
                ))
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedThreads.forEach { onDeleteThread(it.threadId, it.address) }
                    showBatchDeleteConfirmation = false
                    selectedThreadIds = emptySet()
                }) {
                    Text(stringResource(R.string.thread_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirmation = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}
