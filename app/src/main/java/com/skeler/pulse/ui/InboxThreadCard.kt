package com.skeler.pulse.ui
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.MarkunreadMailbox
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.zIndex
import com.skeler.pulse.InboxAccessState
import com.skeler.pulse.R
import com.skeler.pulse.contact.contactLookupIntent
import com.skeler.pulse.contact.contactPhotoUriFor
import com.skeler.pulse.contact.displayNameFor
import com.skeler.pulse.contact.formatAddressForDisplay
import com.skeler.pulse.design.component.SerafinaAvatar
import com.skeler.pulse.design.component.SerafinaProgressIndicator
import com.skeler.pulse.design.util.elasticOverscroll
import com.skeler.pulse.design.util.motionAnimateItemModifier
import com.skeler.pulse.design.util.rememberEntranceModifier
import com.skeler.pulse.design.util.rememberReducedMotionEnabled
import com.skeler.pulse.design.util.rememberSmoothFlingBehavior
import com.skeler.pulse.sms.SmsThread
import coil.compose.AsyncImage



@Composable
internal fun SmsThreadCard(
    thread: SmsThread,
    isPinned: Boolean,
    isArchived: Boolean,
    isContextMenuOpen: Boolean,
    emoji: String?,
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
    onEmojiClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val reducedMotion = rememberReducedMotionEnabled()
    val displayName = remember(thread.address) { displayNameFor(context, thread.address) }
    val formattedAddress = remember(thread.address) { formatAddressForDisplay(thread.address) }
    val showAddress = displayName != formattedAddress
    val initials = displayName.toAvatarInitials()
    val photoUri = remember(thread.address) { contactPhotoUriFor(context, thread.address) }
    val hasUnread = thread.unreadCount > 0
    var shouldShowDeleteConfirmation by rememberSaveable(thread.threadId, thread.address) {
        mutableStateOf(false)
    }
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
                    }
                    if (showAddress) {
                        Text(
                            text = formattedAddress,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (thread.lastMmsPartUri != null) {
                        if (thread.lastMmsContentType?.startsWith("audio/") == true) {
                            AudioWaveformPreview(
                                uri = thread.lastMmsPartUri,
                                modifier = Modifier.size(48.dp),
                            )
                        } else {
                            AsyncImage(
                                model = thread.lastMmsPartUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop,
                            )
                        }
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
                    if (isPinned) {
                        Icon(
                            imageVector = Icons.Rounded.PushPin,
                            contentDescription = "Pinned",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
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
                    Text(
                        text = emoji ?: "😊",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (emoji != null) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onEmojiClick)
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                    )
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
