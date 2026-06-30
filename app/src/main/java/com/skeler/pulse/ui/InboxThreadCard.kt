package com.skeler.pulse.ui

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.MarkunreadMailbox
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skeler.pulse.R
import com.skeler.pulse.contact.contactLookupIntent
import com.skeler.pulse.contact.contactPhotoUriFor
import com.skeler.pulse.contact.displayNameFor
import com.skeler.pulse.contact.formatAddressForDisplay
import com.skeler.pulse.design.component.SerafinaAvatar
import com.skeler.pulse.design.util.rememberReducedMotionEnabled
import com.skeler.pulse.sms.SmsThread
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext



@Composable
internal fun SmsThreadCard(
    thread: SmsThread,
    isPinned: Boolean,
    isArchived: Boolean,
    isMuted: Boolean = false,
    isContextMenuOpen: Boolean,
    isSelected: Boolean = false,
    draft: String = "",
    scheduled: Boolean = false,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onDismissMenu: () -> Unit,
    onTogglePinned: () -> Unit,
    onToggleArchived: () -> Unit,
    onToggleUnread: () -> Unit,
    onBlock: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val reducedMotion = rememberReducedMotionEnabled()
    var displayName by remember(thread.address) { mutableStateOf(thread.address) }
    var photoUri by remember(thread.address) { mutableStateOf<Uri?>(null) }
    LaunchedEffect(thread.address) {
        val (name, uri) = withContext(Dispatchers.IO) {
            displayNameFor(context, thread.address) to contactPhotoUriFor(context, thread.address)
        }
        displayName = name
        photoUri = uri
    }
    val formattedAddress = remember(thread.address) { formatAddressForDisplay(thread.address) }
    val showAddress = displayName != formattedAddress
    val initials = remember(displayName) { displayName.toAvatarInitials() }
    val hasUnread = thread.unreadCount > 0
    val hasAudioMms = thread.lastMmsPartUri != null && thread.lastMmsContentType?.startsWith("audio/") == true
    val hasImageMms = thread.lastMmsPartUri != null && !hasAudioMms
    var shouldShowDeleteConfirmation by rememberSaveable(thread.threadId, thread.address) {
        mutableStateOf(false)
    }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val containerColor by animateColorAsState(
        targetValue = when {
            isContextMenuOpen || isSelected -> MaterialTheme.colorScheme.surfaceContainerHigh
            hasUnread -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        },
        label = "thread_card_container",
    )
    val outlineColor by animateColorAsState(
        targetValue = when {
            isContextMenuOpen || isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.44f)
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
    val threadOpenPrefix = stringResource(R.string.thread_open_prefix)
    val threadUnreadLabel = stringResource(R.string.thread_unread_count)
    val semanticsLabel = remember(displayName, thread.unreadCount, threadOpenPrefix, threadUnreadLabel) {
        buildString {
            append(threadOpenPrefix)
            append(displayName)
            if (thread.unreadCount > 0) {
                append(", ")
                append(threadUnreadLabel.format(thread.unreadCount))
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
            border = BorderStroke(1.dp, SolidColor(outlineColor)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top,
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .combinedClickable(
                                onClick = {
                                    contactLookupIntent(context, thread.address)
                                        ?.let { context.startActivity(it) }
                                },
                                onLongClick = onLongPress,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        SerafinaAvatar(imageUrl = photoUri?.toString(), initials = initials, hasUnread = hasUnread, size = 48.dp)
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = displayName,
                            style = if (hasUnread) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            else MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (isPinned) {
                            Icon(
                                imageVector = Icons.Rounded.PushPin,
                                contentDescription = stringResource(R.string.thread_pinned),
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            )
                        }
                        if (isMuted) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsOff,
                                contentDescription = stringResource(R.string.thread_muted),
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            )
                        }
                    }
                    if (showAddress) {
                        Text(
                            text = formattedAddress,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (hasAudioMms) {
                        AudioWaveformPreview(
                            uri = thread.lastMmsPartUri,
                            modifier = Modifier.size(48.dp),
                        )
                    } else {
                        if (draft.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.tertiary,
                                )
                                Text(
                                    text = draft,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    ),
                                    color = MaterialTheme.colorScheme.tertiary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        } else if (scheduled) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = stringResource(R.string.scheduled_messages_snippet),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        } else if (hasImageMms) {
                            AsyncImage(
                                model = thread.lastMmsPartUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
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
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = thread.timestamp.toInboxTimestamp(),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (hasUnread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Email,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                        Text(
                            text = thread.messageCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
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
                text = if (isPinned) stringResource(R.string.thread_unpin) else stringResource(R.string.thread_pin),
                icon = if (isPinned) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                onClick = {
                    onDismissMenu()
                    onTogglePinned()
                },
            )
            SerafinaContextMenuItem(
                text = if (isArchived) stringResource(R.string.thread_unarchive) else stringResource(R.string.thread_archive),
                icon = Icons.Rounded.Archive,
                onClick = {
                    onDismissMenu()
                    onToggleArchived()
                },
            )
            SerafinaContextMenuItem(
                text = if (hasUnread) stringResource(R.string.thread_mark_read) else stringResource(R.string.thread_mark_unread),
                icon = Icons.Rounded.MarkunreadMailbox,
                onClick = {
                    onDismissMenu()
                    onToggleUnread()
                },
            )
            SerafinaContextMenuItem(
                text = stringResource(R.string.thread_block),
                icon = Icons.Rounded.Block,
                contentColor = MaterialTheme.colorScheme.error,
                onClick = {
                    onDismissMenu()
                    onBlock()
                },
            )
            SerafinaContextMenuDivider()
            SerafinaContextMenuItem(
                text = stringResource(R.string.thread_delete),
                icon = Icons.Rounded.Delete,
                contentColor = MaterialTheme.colorScheme.error,
                onClick = {
                    onDismissMenu()
                    shouldShowDeleteConfirmation = true
                },
            )
        }
    }

    if (shouldShowDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { shouldShowDeleteConfirmation = false },
            title = {
                Text(stringResource(R.string.thread_delete_title))
            },
            text = {
                Text(stringResource(R.string.thread_delete_body, displayName))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        shouldShowDeleteConfirmation = false
                        onDelete()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.thread_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { shouldShowDeleteConfirmation = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}
