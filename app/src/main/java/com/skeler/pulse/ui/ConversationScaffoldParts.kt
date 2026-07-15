package com.skeler.pulse.ui

import androidx.compose.foundation.background
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skeler.pulse.R
import com.skeler.pulse.design.component.SerafinaAvatar
import com.skeler.pulse.design.component.StatusPill
import com.skeler.pulse.design.util.motionAnimateItemModifier
import com.skeler.pulse.design.util.rememberEntranceModifier
import com.skeler.pulse.sms.SystemSms

private val ConversationCallButtonSize = 36.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConversationTopBar(
    title: String,
    address: String,
    messages: List<SystemSms>,
    unreadCount: Int,
    importantCount: Int,
    avatarColors: ConversationAvatarColors,
    onBack: () -> Unit,
    onCallAddress: () -> Unit,
) {
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
                    SerafinaAvatar(
                        imageUrl = null,
                        initials = title.toAvatarInitials(),
                        hasUnread = hasUnreadMessages,
                        size = 42.dp,
                        containerColor = avatarColors.containerColor,
                        contentColor = avatarColors.contentColor,
                    )
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
                        Text(
                            text = address.toConversationMetaLabel(
                                totalMessages = messages.size,
                                unreadCount = unreadCount,
                                importantCount = importantCount,
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = topBarContentColor.copy(alpha = 0.78f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = topBarChromeContainerColor,
            scrolledContainerColor = colors.surface.copy(
                alpha = ConversationVisualTokens.topBarSurfaceAlpha,
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
    onRetrySend: () -> Unit,
    onSimOptionClick: (NewChatSimOption) -> Unit,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
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
                onSimOptionClick = onSimOptionClick,
                onDraftChange = onDraftChange,
                onSend = onSend,
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
    contextMenuMessageId: Long?,
    reducedMotion: Boolean,
    onToggleContextMenu: (Long) -> Unit,
    onDismissMenu: () -> Unit,
    onCopy: (SystemSms) -> Unit,
    onCopyCode: (String) -> Unit,
    onDelete: (SystemSms) -> Unit,
    onBlock: () -> Unit,
    onForward: (SystemSms) -> Unit,
) {
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
            items = timelineItems,
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
                    isContextMenuOpen = contextMenuMessageId == item.message.id,
                    onLongPress = { onToggleContextMenu(item.message.id) },
                    onDismissMenu = onDismissMenu,
                    onCopy = { onCopy(item.message) },
                    onCopyCode = onCopyCode,
                    onDelete = { onDelete(item.message) },
                    onBlock = onBlock,
                    onForward = { onForward(item.message) },
                    modifier = motionAnimateItemModifier(reducedMotion)
                        .then(rememberEntranceModifier(item.key, reducedMotion)),
                )
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
