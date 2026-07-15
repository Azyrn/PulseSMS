package com.skeler.pulse.ui
import android.content.ClipData
import android.content.ClipboardManager
import android.provider.Telephony
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skeler.pulse.R
import com.skeler.pulse.design.component.SerafinaAvatar
import com.skeler.pulse.design.component.SerafinaProgressIndicator
import com.skeler.pulse.design.component.StatusPill
import com.skeler.pulse.design.util.elasticOverscroll
import com.skeler.pulse.design.util.isNearListEnd
import com.skeler.pulse.design.util.motionAnimateItemModifier
import com.skeler.pulse.design.util.rememberEntranceModifier
import com.skeler.pulse.design.util.rememberReducedMotionEnabled
import com.skeler.pulse.design.util.rememberSmoothFlingBehavior
import com.skeler.pulse.design.util.scrollToItemSmoothly
import com.skeler.pulse.sms.OtpCodeExtractor
import com.skeler.pulse.sms.SystemSms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Instant


@Composable
internal fun ConversationMessageBubble(
    message: SystemSms,
    isImportant: Boolean,
    isContextMenuOpen: Boolean,
    onLongPress: () -> Unit,
    onDismissMenu: () -> Unit,
    onCopy: () -> Unit,
    onCopyCode: (String) -> Unit,
    onDelete: () -> Unit,
    onBlock: () -> Unit,
    onForward: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val isOutbound = message.isOutbound
    val isUnread = message.isInbound && !message.read
    val hasFailedDelivery = message.hasFailedDelivery()
    val colors = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bubbleShape = RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 24.dp,
        bottomStart = if (isOutbound) 24.dp else 10.dp,
        bottomEnd = if (isOutbound) 10.dp else 24.dp,
    )
    val bubbleScale by animateFloatAsState(
        targetValue = if (isPressed || isContextMenuOpen) 0.985f else 1f,
        animationSpec = conversationPressAnimationSpec(reducedMotion),
        label = "message_bubble_press_scale",
    )
    val bubbleElevation by animateDpAsState(
        targetValue = if (isContextMenuOpen) 6.dp else if (isPressed) 2.dp else if (isOutbound) 0.dp else 1.dp,
        animationSpec = conversationPressAnimationSpec(reducedMotion),
        label = "message_bubble_press_elevation",
    )
    val bubbleOutlineColor by animateColorAsState(
        targetValue = when {
            hasFailedDelivery -> colors.error.copy(alpha = ConversationVisualTokens.failedBubbleOutlineAlpha)
            isContextMenuOpen -> colors.primary.copy(alpha = ConversationVisualTokens.activeBubbleOutlineAlpha)
            isPressed -> colors.primary.copy(alpha = ConversationVisualTokens.pressedBubbleOutlineAlpha)
            isUnread -> colors.tertiary.copy(alpha = ConversationVisualTokens.unreadBubbleOutlineAlpha)
            else -> conversationRestingBubbleOutlineColor(colors, isOutbound)
        },
        label = "message_bubble_press_outline",
    )
    val bubbleContainerColor by animateColorAsState(
        targetValue = conversationBubbleContainerColor(
            colors = colors,
            isOutbound = isOutbound,
            isUnread = isUnread,
            hasFailedDelivery = hasFailedDelivery,
        ),
        label = "message_bubble_container",
    )
    val messageText = message.body.ifBlank { " " }
    val messageLinkText = remember(messageText, colors, hasFailedDelivery, isOutbound) {
        messageText.toConversationMessageLinks(
            linkColor = when {
                hasFailedDelivery -> colors.error
                isOutbound -> colors.primary
                else -> colors.primary
            },
        )
    }
    val copyableCode = remember(message.body) { copyableMessageCode(message.body) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isOutbound) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = ConversationVisualTokens.messageMaxWidth)
                .graphicsLayer {
                    scaleX = bubbleScale
                    scaleY = bubbleScale
                }
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {},
                    onLongClick = onLongPress,
                ),
        ) {
            Column(
                horizontalAlignment = if (isOutbound) Alignment.End else Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Surface(
                    modifier = Modifier.border(
                        width = ConversationVisualTokens.bubbleOutlineWidth,
                        color = bubbleOutlineColor,
                        shape = bubbleShape,
                    ),
                    shape = bubbleShape,
                    color = bubbleContainerColor,
                    tonalElevation = bubbleElevation,
                    shadowElevation = bubbleElevation,
                ) {
                    Text(
                        text = messageLinkText,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (isUnread) FontWeight.Medium else FontWeight.Normal,
                        ),
                        color = when {
                            hasFailedDelivery -> colors.onErrorContainer
                            isOutbound -> colors.onPrimaryContainer
                            else -> colors.onSurface
                        },
                    )
                }
                copyableCode?.let { code ->
                    CopyCodeButton(
                        code = code,
                        onClick = { onCopyCode(code) },
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (hasFailedDelivery) {
                        StatusPill(
                            label = stringResource(R.string.conversation_message_failed),
                            containerColor = colors.errorContainer,
                            contentColor = colors.onErrorContainer,
                        )
                    }
                    if (isImportant) {
                        StatusPill(
                            label = stringResource(R.string.conversation_message_kept),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    Text(
                        text = message.timestamp.toConversationTime(),
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            hasFailedDelivery -> colors.error
                            isUnread -> colors.tertiary
                            else -> colors.onSurfaceVariant
                        },
                    )
                }
            }

            SerafinaContextMenu(
                expanded = isContextMenuOpen,
                onDismissRequest = onDismissMenu,
                shadowElevation = 0.dp,
            ) {
                SerafinaContextMenuItem(
                    text = stringResource(R.string.conversation_action_copy),
                    icon = Icons.Rounded.ContentCopy,
                    onClick = onCopy,
                )
                SerafinaContextMenuItem(
                    text = stringResource(R.string.conversation_action_forward),
                    icon = Icons.AutoMirrored.Rounded.ArrowForward,
                    onClick = onForward,
                )
                if (shouldShowMessageBlockAction(isOutbound)) {
                    SerafinaContextMenuItem(
                        text = stringResource(R.string.conversation_action_block),
                        icon = Icons.Rounded.Block,
                        contentColor = MaterialTheme.colorScheme.error,
                        onClick = onBlock,
                    )
                }
                SerafinaContextMenuDivider()
                SerafinaContextMenuItem(
                    text = stringResource(R.string.conversation_action_delete),
                    icon = Icons.Rounded.Delete,
                    contentColor = MaterialTheme.colorScheme.error,
                    onClick = onDelete,
                )
            }
        }
    }
}

@Composable
internal fun CopyCodeButton(
    code: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val copyCodeContentDescription = stringResource(R.string.conversation_copy_code_content_description, code)

    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .semantics {
                contentDescription = copyCodeContentDescription
            },
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.ContentCopy,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.conversation_action_copy_code),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

internal fun copyableMessageCode(body: String): String? =
    OtpCodeExtractor.extractCode(body)

internal fun String.toConversationMessageLinks(linkColor: Color): AnnotatedString {
    val targets = MessageLinkDetector.detectTargets(this)
    if (targets.isEmpty()) return AnnotatedString(this)

    val linkStyles = TextLinkStyles(
        style = SpanStyle(
            color = linkColor,
            textDecoration = TextDecoration.Underline,
            fontWeight = FontWeight.SemiBold,
        ),
        pressedStyle = SpanStyle(
            color = linkColor.copy(alpha = 0.74f),
        ),
    )
    return AnnotatedString.Builder(this).apply {
        targets.forEach { target ->
            addLink(
                url = LinkAnnotation.Url(
                    url = target.uri,
                    styles = linkStyles,
                ),
                start = target.start,
                end = target.end,
            )
        }
    }.toAnnotatedString()
}
