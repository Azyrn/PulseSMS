package com.skeler.pulse.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skeler.pulse.R
import com.skeler.pulse.contact.contactLookupIntent
import com.skeler.pulse.contact.contactPhotoUriFor
import com.skeler.pulse.contact.formatAddressForDisplay
import com.skeler.pulse.design.component.SerafinaAvatar
import com.skeler.pulse.design.component.StatusPill
import com.skeler.pulse.design.util.motionAnimateItemModifier
import com.skeler.pulse.design.util.rememberEntranceModifier
import com.skeler.pulse.sms.SystemSms
import android.provider.Telephony
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val ConversationCallButtonSize = 36.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConversationTopBar(
    title: String,
    address: String,
    messages: List<SystemSms>,
    unreadCount: Int,
    importantCount: Int,
    totalMessageCount: Int,
    avatarColors: ConversationAvatarColors,
    onBack: () -> Unit,
    onCallAddress: () -> Unit,
) {
    val context = LocalContext.current
    val photoUri = remember(address) { contactPhotoUriFor(context, address) }
    val hasUnreadMessages = unreadCount > 0
    val colors = MaterialTheme.colorScheme
    val topBarChromeContainerColor = conversationTopBarChromeContainerColor(hasUnreadMessages)
    val titleContainerColor = conversationTopBarTitleContainerColor(colors, hasUnreadMessages)
    val topBarContentColor = conversationTopBarContentColor(colors, hasUnreadMessages)
    val shouldShowCallAction = shouldShowConversationCallAction(address)

    TopAppBar(
        modifier = Modifier.background(conversationTopBarBrush()),
        navigationIcon = {
            FilledTonalIconButton(
                onClick = onBack,
                modifier = Modifier.padding(start = 12.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBackIosNew,
                    contentDescription = androidx.compose.ui.res.stringResource(R.string.action_back),
                )
            }
        },
        title = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ConversationVisualTokens.topBarTitleShape,
                color = titleContainerColor,
                tonalElevation = 0.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clickable {
                                contactLookupIntent(context, address)
                                    ?.let { context.startActivity(it) }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        SerafinaAvatar(
                            imageUrl = photoUri?.toString(),
                            initials = title.toAvatarInitials(),
                            hasUnread = hasUnreadMessages,
                            size = 42.dp,
                            containerColor = avatarColors.containerColor,
                            contentColor = avatarColors.contentColor,
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = title,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = topBarContentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val formattedAddress = remember(address) { formatAddressForDisplay(address) }
                        if (title != formattedAddress) {
                            Text(
                                text = formattedAddress,
                                style = MaterialTheme.typography.labelSmall,
                                color = topBarContentColor.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        val resources = context.resources
                        val metaParts = buildList {
                            add(resources.getString(
                                R.string.conversation_messages_label,
                                if (totalMessageCount > 0) totalMessageCount else messages.size,
                            ))
                            if (unreadCount > 0) add(resources.getString(R.string.conversation_unread_label, unreadCount))
                            if (importantCount > 0) add(resources.getString(R.string.conversation_kept_label, importantCount))
                        }
                        val metaLabel = metaParts.joinToString(" · ")
                        Text(
                            text = metaLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = topBarContentColor.copy(alpha = 0.78f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = topBarChromeContainerColor,
            scrolledContainerColor = colors.surface.copy(
                alpha = ConversationVisualTokens.TOP_BAR_SURFACE_ALPHA,
            ),
            navigationIconContentColor = topBarContentColor,
            titleContentColor = topBarContentColor,
        ),
        actions = {
            if (shouldShowCallAction) {
                FilledTonalIconButton(
                    onClick = onCallAddress,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(ConversationCallButtonSize),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Call,
                        contentDescription = stringResource(R.string.action_call_contact, title),
                        tint = topBarContentColor,
                    )
                }
            }
        },
    )
}

@Composable
internal fun ConversationBottomBar(
    isReplyable: Boolean,
    draft: String,
    sendState: SendState,
    simOptions: List<NewChatSimOption>,
    selectedSimKey: String?,
    selectedImageUris: List<Uri>,
    onRetrySend: () -> Unit,
    onSimOptionClick: (NewChatSimOption) -> Unit,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onImageSelected: (List<Uri>) -> Unit = {},
    onImagePickFromGallery: () -> Unit = {},
    onTakePhoto: () -> Unit = {},
    onVoiceRecorded: (Uri) -> Unit = {},
    showAttachmentMenu: Boolean = false,
    onAttachmentMenuVisibilityChange: (Boolean) -> Unit = {},
) {
    if (isReplyable) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(conversationComposerScrimBrush())
                .navigationBarsPadding()
                .imePadding(),
        ) {
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
                simOptions = simOptions,
                selectedSimKey = selectedSimKey,
                selectedImageUris = selectedImageUris,
                onSimOptionClick = onSimOptionClick,
                onDraftChange = onDraftChange,
                onSend = onSend,
                onImageSelected = onImageSelected,
                onImagePickFromGallery = onImagePickFromGallery,
                onTakePhoto = onTakePhoto,
                onVoiceRecorded = onVoiceRecorded,
                showAttachmentMenu = showAttachmentMenu,
                onAttachmentMenuVisibilityChange = onAttachmentMenuVisibilityChange,
            )
        }
    } else {
        Box(modifier = Modifier.fillMaxWidth()) {
            ReadOnlyConversationNotice()
        }
    }
}

internal fun LazyListScope.conversationTimelineItems(
    title: String,
    address: String,
    messages: List<SystemSms>,
    unreadCount: Int,
    importantCount: Int,
    latestTimestamp: java.time.Instant?,
    avatarColors: ConversationAvatarColors,
    loading: Boolean,
    timelineItems: List<ConversationTimelineItem>,
    importantMessageIds: Set<Long>,
    messageReactions: Map<Long, String>,
    selectedMessages: Set<SystemSms>,
    reducedMotion: Boolean,
    onCopyCode: (String) -> Unit,
    onToggleMessageSelection: (SystemSms) -> Unit,
    onMessageEmojiClick: (Long) -> Unit,
    hasMoreMessages: Boolean = false,
    loadingMore: Boolean = false,
    onLoadMoreMessages: () -> Unit = {},
) {
    when {
        loading -> item(key = "conversation_loading", contentType = ConversationLoadingContentType) {
            ConversationLoadingSkeleton(
                modifier = rememberEntranceModifier("conversation_loading_$address", reducedMotion),
            )
        }

        timelineItems.isEmpty() -> item(key = "conversation_empty", contentType = ConversationEmptyContentType) {
            EmptyConversationState(
                title = title,
                modifier = rememberEntranceModifier("conversation_empty_$address", reducedMotion),
            )
        }

        else -> items(
            items = timelineItems.asReversed(),
            key = ConversationTimelineItem::key,
            contentType = ConversationTimelineItem::contentType,
        ) { item ->
            when (item) {
                is ConversationTimelineItem.DayDivider -> ConversationDayDivider(
                    item = item,
                    modifier = motionAnimateItemModifier(reducedMotion)
                        .then(rememberEntranceModifier(item.key, reducedMotion)),
                )
                is ConversationTimelineItem.UnreadDivider -> ConversationUnreadDivider(
                    item = item,
                    modifier = motionAnimateItemModifier(reducedMotion)
                        .then(rememberEntranceModifier(item.key, reducedMotion)),
                )
                is ConversationTimelineItem.Message -> ConversationMessageBubble(
                    message = item.message,
                    isImportant = item.message.id in importantMessageIds,
                    isSelected = item.message in selectedMessages,
                    isSelectionMode = selectedMessages.isNotEmpty(),
                    reaction = messageReactions[item.message.id],
                    onLongPress = { onToggleMessageSelection(item.message) },
                    onCopyCode = onCopyCode,
                    onToggleSelection = { onToggleMessageSelection(item.message) },
                    onEmojiClick = { onMessageEmojiClick(item.message.id) },
                    modifier = motionAnimateItemModifier(reducedMotion)
                        .then(rememberEntranceModifier(item.key, reducedMotion)),
                )
                is ConversationTimelineItem.ReactionCard -> ConversationReactionCard(
                    item = item,
                    modifier = motionAnimateItemModifier(reducedMotion)
                        .then(rememberEntranceModifier(item.key, reducedMotion)),
                )
            }
        }
    }

    if (hasMoreMessages && !loading) {
        item(
            key = "conversation_load_more",
            contentType = ConversationLoadMoreContentType,
        ) {
            ConversationLoadMoreItem(
                loadingMore = loadingMore,
                onLoadMore = onLoadMoreMessages,
                modifier = rememberEntranceModifier("conversation_load_more_$address", reducedMotion),
            )
        }
    }

    item(
        key = "conversation_header",
        contentType = ConversationHeaderContentType,
    ) {
        ConversationOverviewCard(
            title = title,
            address = address,
            messageCount = messages.size,
            unreadCount = unreadCount,
            importantCount = importantCount,
            latestTimestamp = latestTimestamp,
            avatarColors = avatarColors,
            modifier = rememberEntranceModifier("conversation_header_$address", reducedMotion),
        )
    }
}

@Composable
private fun ConversationLoadMoreItem(
    loadingMore: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth().padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (loadingMore) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.5f))
        } else {
            TextButton(onClick = onLoadMore) {
                Text(stringResource(R.string.conversation_load_older_messages))
            }
        }
    }
}

@Composable
private fun ConversationDayDivider(
    item: ConversationTimelineItem.DayDivider,
    modifier: Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = ConversationPillShape,
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

@Composable
internal fun ConversationReactionCard(
    item: ConversationTimelineItem.ReactionCard,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = ConversationPillShape,
            color = colors.surfaceContainerLow,
        ) {
            Text(
                text = "${item.emoji}  \u00ab ${item.referencedText} \u00bb",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun ConversationUnreadDivider(
    item: ConversationTimelineItem.UnreadDivider,
    modifier: Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart,
    ) {
        StatusPill(
            label = item.label,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun ConversationSelectionTopBar(
    selectedCount: Int,
    onClose: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onInfo: (() -> Unit)? = null,
) {
    CenterAlignedTopAppBar(
        title = { Text(pluralStringResource(R.plurals.conversation_selected_count, selectedCount, selectedCount)) },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close selection",
                )
            }
        },
        actions = {
            if (selectedCount == 1 && onInfo != null) {
                IconButton(onClick = onInfo) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = stringResource(R.string.message_info_show),
                    )
                }
            }
            if (selectedCount >= 1) {
                IconButton(onClick = onCopy) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = "Copy message",
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete selected",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MessageInfoSheet(
    message: SystemSms?,
    onDismiss: () -> Unit,
) {
    if (message == null) return
    var showSheet by remember { mutableStateOf(true) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showSheet = false
                onDismiss()
            },
            sheetState = sheetState,
        ) {
            MessageInfoContent(message = message)
        }
    }
}

@Composable
private fun MessageInfoContent(message: SystemSms) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val resources = context.resources
    val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy HH:mm")

    fun formatTimestamp(epochMillis: Long): String =
        dateFormatter.format(java.time.Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

    val deliveryDate by produceState<Long?>(initialValue = null, message.id, message.threadId) {
        if (message.isOutbound && message.threadId > 0L) {
            value = try {
                val cursor = context.contentResolver.query(
                    Telephony.Sms.CONTENT_URI,
                    arrayOf(Telephony.Sms.DATE),
                    "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.ADDRESS} = ? AND ${Telephony.Sms.DATE} > ?",
                    arrayOf(
                        message.threadId.toString(),
                        message.address,
                        message.date.toString(),
                    ),
                    "${Telephony.Sms.DATE} ASC",
                )
                cursor?.use {
                    if (it.moveToFirst()) it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE)) else null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    val typeLabel = when (message.type) {
        Telephony.Sms.MESSAGE_TYPE_INBOX -> R.string.message_type_inbox
        Telephony.Sms.MESSAGE_TYPE_SENT -> R.string.message_type_sent
        Telephony.Sms.MESSAGE_TYPE_DRAFT -> R.string.message_type_draft
        Telephony.Sms.MESSAGE_TYPE_OUTBOX -> R.string.message_type_outbox
        Telephony.Sms.MESSAGE_TYPE_FAILED -> R.string.message_type_failed
        else -> null
    }
    val protocolLabel = if (message.isMms) R.string.message_type_mms else R.string.message_type_sms
    val priorityLabel = when (message.priority) {
        2 -> R.string.message_priority_high
        1 -> R.string.message_priority_normal
        else -> null
    }

    Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp)) {
        Text(
            text = stringResource(R.string.message_info_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        InfoRow(label = stringResource(R.string.message_info_type), value = buildString {
            append(stringResource(protocolLabel))
            typeLabel?.let { append(" · ${stringResource(it)}") }
        })

        priorityLabel?.let {
            Spacer(modifier = Modifier.height(10.dp))
            InfoRow(label = stringResource(R.string.message_info_priority), value = stringResource(it))
        }

        Spacer(modifier = Modifier.height(10.dp))
        InfoRow(label = stringResource(R.string.message_info_from), value = message.fromAddress ?: message.address)
        Spacer(modifier = Modifier.height(10.dp))
        InfoRow(label = stringResource(R.string.message_info_to), value = message.toAddress ?: message.address)

        if (message.dateSent != null && message.dateSent > 0L) {
            Spacer(modifier = Modifier.height(10.dp))
            InfoRow(label = stringResource(R.string.message_info_sent), value = formatTimestamp(message.dateSent))
        } else if (message.isOutbound) {
            Spacer(modifier = Modifier.height(10.dp))
            InfoRow(label = stringResource(R.string.message_info_sent), value = formatTimestamp(message.date))
        }
        if (message.isOutbound && message.status != Telephony.Sms.STATUS_NONE && message.type == Telephony.Sms.MESSAGE_TYPE_SENT) {
            Spacer(modifier = Modifier.height(10.dp))
            val statusLabel = when (message.status) {
                Telephony.Sms.STATUS_COMPLETE -> R.string.message_status_delivered
                Telephony.Sms.STATUS_PENDING -> R.string.message_status_pending
                Telephony.Sms.STATUS_FAILED -> R.string.message_status_failed
                else -> R.string.message_status_unknown
            }
            InfoRow(label = stringResource(R.string.message_info_status), value = stringResource(statusLabel))
            if (message.status == Telephony.Sms.STATUS_COMPLETE && deliveryDate != null) {
                Spacer(modifier = Modifier.height(10.dp))
                InfoRow(label = stringResource(R.string.message_info_received), value = formatTimestamp(deliveryDate!!))
            }
        }
        if (message.isInbound) {
            Spacer(modifier = Modifier.height(10.dp))
            InfoRow(label = stringResource(R.string.message_info_received), value = formatTimestamp(message.date))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
