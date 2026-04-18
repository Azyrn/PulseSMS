package com.skeler.pulse.messaging.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skeler.pulse.contracts.messaging.ComposerTransition
import com.skeler.pulse.contracts.messaging.ConversationSyncState
import com.skeler.pulse.contracts.messaging.DeliveryIndicator
import com.skeler.pulse.contracts.messaging.MessageDirection
import com.skeler.pulse.contracts.messaging.MessageRenderItem
import com.skeler.pulse.contracts.messaging.MessagingState
import com.skeler.pulse.contracts.messaging.RowSyncState
import com.skeler.pulse.contracts.messaging.SendBlockReason
import com.skeler.pulse.contracts.messaging.SendEligibility
import com.skeler.pulse.design.component.BubbleShape
import com.skeler.pulse.messaging.model.MessagingIntent
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MessagingScreen(
    state: MessagingState,
    onIntent: (MessagingIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val isSendEnabled by remember(state.composer.draft.text, state.composer.eligibility) {
        derivedStateOf {
            state.composer.draft.text.isNotBlank() && state.composer.eligibility is SendEligibility.Allowed
        }
    }
    val banner by remember(state.sync, state.composer.eligibility, state.surfaceError) {
        derivedStateOf {
            state.surfaceError?.message?.let {
                ConversationBanner(title = "Message not sent", detail = it, isError = true)
            } ?: state.composer.eligibility.toBanner() ?: state.sync.toBanner()
        }
    }

    LaunchedEffect(state.conversationId, state.timeline.items.size) {
        val lastIndex = state.timeline.items.lastIndex
        if (lastIndex >= 0) listState.scrollToItem(lastIndex)
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Banner
        AnimatedVisibility(visible = banner != null, enter = fadeIn(), exit = fadeOut()) {
            banner?.let { msg ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = if (msg.isError) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(msg.title, style = MaterialTheme.typography.labelLarge,
                            color = if (msg.isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer)
                        Text(msg.detail, style = MaterialTheme.typography.bodySmall,
                            color = if (msg.isError) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f))
                    }
                }
            }
        }

        // Messages or empty state
        if (state.timeline.items.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("💬", fontSize = 40.sp)
                    Text("No messages yet", style = MaterialTheme.typography.titleMedium)
                    Text("Send the first message below", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(items = state.timeline.items, key = MessageRenderItem::id) { item ->
                    MessageBubble(item = item)
                }
            }
        }

        // Composer
        Surface(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding(),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                OutlinedTextField(
                    value = state.composer.draft.text,
                    onValueChange = { onIntent(MessagingIntent.DraftChanged(it)) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    maxLines = 5,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    enabled = state.composer.eligibility is SendEligibility.Allowed,
                )
                FilledTonalIconButton(
                    onClick = { onIntent(MessagingIntent.SendPressed) },
                    enabled = isSendEnabled,
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) {
                    Text("↑", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(item: MessageRenderItem) {
    val isOutgoing = item.direction == MessageDirection.OUTBOUND

    Row(
        modifier = Modifier.fillMaxWidth().graphicsLayer { },
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 300.dp),
            horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Surface(
                shape = BubbleShape(isUser = isOutgoing),
                color = if (isOutgoing) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Text(
                    text = item.bodyPreview,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isOutgoing) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.sentAt?.toBubbleTime() ?: "Now",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = item.status.delivery.toStatusLabel(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = item.status.delivery.toStatusColor(),
                )
            }
            item.status.sync.failureDetail()?.let { detail ->
                Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// ── Helpers ──

private data class ConversationBanner(val title: String, val detail: String, val isError: Boolean)

private fun SendEligibility.toBanner(): ConversationBanner? = when (this) {
    SendEligibility.Allowed -> null
    is SendEligibility.Blocked -> ConversationBanner(reason.toBlockedTitle(), reason.toBlockedDetail(), true)
}

private fun ConversationSyncState.toBanner(): ConversationBanner? = when (this) {
    ConversationSyncState.Idle, ConversationSyncState.Syncing, ConversationSyncState.UpToDate -> null
    is ConversationSyncState.Backoff -> ConversationBanner("Retry scheduled", "We'll keep trying to deliver your messages.", false)
    is ConversationSyncState.Conflict -> ConversationBanner("Sync conflict", "A delivery conflict was detected.", true)
    is ConversationSyncState.Failed -> ConversationBanner("Sync failed", error.message, true)
}

private fun SendBlockReason.toBlockedTitle(): String = when (this) {
    SendBlockReason.SenderVerificationPending -> "Verification pending"
    SendBlockReason.RecipientVerificationPending -> "Recipient pending"
    SendBlockReason.TenDlcRegistrationPending -> "Registration pending"
    SendBlockReason.MissingIdentityVerification -> "Identity check required"
    SendBlockReason.MissingEncryptionMaterial -> "Encryption unavailable"
    SendBlockReason.RateLimited -> "Sending paused"
    SendBlockReason.Offline -> "Offline"
}

private fun SendBlockReason.toBlockedDetail(): String = when (this) {
    SendBlockReason.SenderVerificationPending -> "Business verification in progress."
    SendBlockReason.RecipientVerificationPending -> "Recipient needs verification."
    SendBlockReason.TenDlcRegistrationPending -> "10DLC registration in progress."
    SendBlockReason.MissingIdentityVerification -> "Identity verification required."
    SendBlockReason.MissingEncryptionMaterial -> "Encryption keys unavailable."
    SendBlockReason.RateLimited -> "Rate limit hit. Auto-retrying."
    SendBlockReason.Offline -> "No network connection."
}

@Composable
private fun DeliveryIndicator.toStatusColor() = when (this) {
    DeliveryIndicator.Pending -> MaterialTheme.colorScheme.onSurfaceVariant
    DeliveryIndicator.Queued -> MaterialTheme.colorScheme.tertiary
    DeliveryIndicator.Sent, DeliveryIndicator.Delivered, DeliveryIndicator.Read -> MaterialTheme.colorScheme.primary
    is DeliveryIndicator.Failed -> MaterialTheme.colorScheme.error
}

private fun DeliveryIndicator.toStatusLabel(): String = when (this) {
    DeliveryIndicator.Pending -> "Sending…"
    DeliveryIndicator.Queued -> "Retrying"
    DeliveryIndicator.Sent -> "Sent"
    DeliveryIndicator.Delivered -> "Delivered"
    DeliveryIndicator.Read -> "Read"
    is DeliveryIndicator.Failed -> "Not sent"
}

private fun RowSyncState.failureDetail(): String? = when (this) {
    RowSyncState.Idle, RowSyncState.Syncing -> null
    is RowSyncState.Failed -> error.message
}

private fun Instant.toBubbleTime(): String = BUBBLE_TIME_FORMATTER.format(atZone(ZoneId.systemDefault()))
private val BUBBLE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
