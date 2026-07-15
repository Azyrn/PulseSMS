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



@Composable
internal fun SmsThreadCard(
    thread: SmsThread,
    isPinned: Boolean,
    isArchived: Boolean,
    isContextMenuOpen: Boolean,
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
    val displayName = remember(thread.address) { displayNameFor(context, thread.address) }
    val initials = displayName.toAvatarInitials()
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
                SerafinaAvatar(imageUrl = null, initials = initials, hasUnread = hasUnread, size = 48.dp)
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
                        if (isPinned) {
                            StatusPill(
                                label = "Pinned",
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
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

        SerafinaContextMenu(
            expanded = isContextMenuOpen,
            onDismissRequest = onDismissMenu,
        ) {
            SerafinaContextMenuItem(
                text = if (isPinned) "Unpin" else "Pin",
                icon = if (isPinned) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                onClick = {
                    onDismissMenu()
                    onTogglePinned()
                },
            )
            SerafinaContextMenuItem(
                text = if (isArchived) "Unarchive" else "Archive",
                icon = Icons.Rounded.Archive,
                onClick = {
                    onDismissMenu()
                    onToggleArchived()
                },
            )
            SerafinaContextMenuItem(
                text = if (hasUnread) "Mark as read" else "Mark as unread",
                icon = Icons.Rounded.MarkunreadMailbox,
                onClick = {
                    onDismissMenu()
                    onToggleUnread()
                },
            )
            SerafinaContextMenuItem(
                text = "Block",
                icon = Icons.Rounded.Block,
                contentColor = MaterialTheme.colorScheme.error,
                onClick = {
                    onDismissMenu()
                    onBlock()
                },
            )
            SerafinaContextMenuDivider()
            SerafinaContextMenuItem(
                text = "Delete",
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
                Text("Delete conversation?")
            },
            text = {
                Text("Delete the conversation with $displayName from this device? This can't be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        shouldShowDeleteConfirmation = false
                        onDelete()
                    },
                ) {
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { shouldShowDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}
