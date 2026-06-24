package com.skeler.pulse.ui
import android.content.ContentValues
import android.content.ClipData
import android.content.ClipboardManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Telephony
import android.util.Log
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.skeler.pulse.R
import com.skeler.pulse.design.component.SerafinaAvatar
import com.skeler.pulse.design.component.SerafinaProgressIndicator
import com.skeler.pulse.design.component.StatusPill
import com.skeler.pulse.design.util.elasticOverscroll

import com.skeler.pulse.design.util.motionAnimateItemModifier
import com.skeler.pulse.design.util.rememberEntranceModifier
import com.skeler.pulse.design.util.rememberReducedMotionEnabled
import com.skeler.pulse.design.util.rememberSmoothFlingBehavior
import com.skeler.pulse.design.util.scrollToItemSmoothly
import com.skeler.pulse.sms.OtpCodeExtractor
import coil.compose.AsyncImage
import com.skeler.pulse.sms.SystemSms
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Pause
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds
import java.time.Instant


@Composable
internal fun ConversationMessageBubble(
    message: SystemSms,
    isImportant: Boolean,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    reaction: String?,
    searchQuery: String = "",
    onLongPress: () -> Unit,
    onCopyCode: (String) -> Unit,
    onToggleSelection: () -> Unit,
    onEmojiClick: () -> Unit,
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
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = conversationPressAnimationSpec(reducedMotion),
        label = "message_bubble_press_scale",
    )
    val bubbleElevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else if (isOutbound) 0.dp else 1.dp,
        animationSpec = conversationPressAnimationSpec(reducedMotion),
        label = "message_bubble_press_elevation",
    )
    val bubbleOutlineColor by animateColorAsState(
        targetValue = when {
            isSelected -> colors.primary.copy(alpha = 0.8f)
            hasFailedDelivery -> colors.error.copy(alpha = ConversationVisualTokens.FAILED_BUBBLE_OUTLINE_ALPHA)
            isPressed -> colors.primary.copy(alpha = ConversationVisualTokens.PRESSED_BUBBLE_OUTLINE_ALPHA)
            isUnread -> colors.tertiary.copy(alpha = ConversationVisualTokens.UNREAD_BUBBLE_OUTLINE_ALPHA)
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
    val messageLinkText = remember(messageText, colors, hasFailedDelivery, isOutbound, searchQuery) {
        messageText.toConversationMessageLinks(
            linkColor = when {
                hasFailedDelivery -> colors.error
                isOutbound -> colors.primary
                else -> colors.primary
            },
            searchQuery = searchQuery,
        )
    }
    val copyableCode = remember(message.body) { copyableMessageCode(message.body) }

    val onClickAction: () -> Unit = if (isSelectionMode) ({ onToggleSelection() }) else ({})
    val onLongClickAction: (() -> Unit)? = if (isSelectionMode) ({ onToggleSelection() }) else onLongPress

    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClickAction,
                onLongClick = onLongClickAction,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection() },
                colors = CheckboxDefaults.colors(
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                ),
            )
            if (isOutbound) {
                Spacer(modifier = Modifier.weight(1f))
            }
        } else if (isOutbound) {
            Spacer(modifier = Modifier.weight(1f))
        }
        Box(
            modifier = Modifier
                .widthIn(max = ConversationVisualTokens.messageMaxWidth)
                .graphicsLayer {
                    scaleX = bubbleScale
                    scaleY = bubbleScale
                },
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
                    Column {
                        val partUri = message.mmsPartUri
                        if (partUri != null) {
                            if (message.mmsContentType?.startsWith("audio/") == true) {
                                VoiceMessagePlayer(
                                    uri = partUri,
                                    isOutbound = isOutbound,
                                )
                            } else {
                                ImageContent(
                                    uris = message.mmsPartUris,
                                    bubbleShape = bubbleShape,
                                    onLongClickAction = onLongClickAction,
                                )
                            }
                        }
                        if (messageText.isNotBlank()) {
                            Text(
                                text = messageLinkText,
                                modifier = Modifier.padding(
                                    horizontal = 16.dp,
                                    vertical = if (message.mmsPartUri != null) 8.dp else 12.dp,
                                ),
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
                    }
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
                    if (message.isDeliveryFailed()) {
                        StatusPill(
                            label = stringResource(R.string.conversation_message_not_delivered),
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
                    if (message.isSentAndDelivered()) {
                        Text(
                            text = stringResource(R.string.conversation_message_delivered),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    } else if (message.isSentPending()) {
                        Text(
                            text = stringResource(R.string.conversation_message_sent),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                    val showEmoji = reaction != null
                    Text(
                        text = if (showEmoji) reaction else "+",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (showEmoji) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onEmojiClick)
                            .padding(horizontal = if (reaction != null) 2.dp else 4.dp, vertical = 1.dp),
                    )
                }
            }
        }

        if (isSelectionMode && !isOutbound) {
            Spacer(modifier = Modifier.weight(1f))
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

internal fun String.toConversationMessageLinks(linkColor: Color, searchQuery: String = ""): AnnotatedString {
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
    val searchHighlightColor = Color(0xFFFFEB3B).copy(alpha = 0.4f)
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
        if (searchQuery.isNotBlank()) {
            val lowerText = this.toString().lowercase()
            val lowerQuery = searchQuery.lowercase()
            var index = 0
            while (true) {
                val found = lowerText.indexOf(lowerQuery, index)
                if (found < 0) break
                addStyle(
                    SpanStyle(background = searchHighlightColor),
                    start = found,
                    end = found + lowerQuery.length,
                )
                index = found + lowerQuery.length
            }
        }
    }.toAnnotatedString()
}

@Composable
private fun ImageContent(
    uris: List<Uri>,
    bubbleShape: RoundedCornerShape,
    onLongClickAction: (() -> Unit)?,
) {
    if (uris.isEmpty()) return
    if (uris.size == 1) {
        var showDialog by remember { mutableStateOf(false) }
        AsyncImage(
            model = uris[0],
            contentDescription = stringResource(R.string.mms_body_placeholder),
            modifier = Modifier
                .widthIn(max = 200.dp)
                .aspectRatio(1f)
                .clip(bubbleShape)
                .combinedClickable(
                    onClick = { showDialog = true },
                    onLongClick = onLongClickAction,
                ),
            contentScale = ContentScale.Crop,
        )
        if (showDialog) {
            MmsImageDialog(uris = uris, initialIndex = 0, onDismiss = { showDialog = false })
        }
    } else {
        val pagerState = rememberPagerState(pageCount = { uris.size })
        var showDialog by remember { mutableStateOf(false) }
        var dialogIndex by remember { mutableIntStateOf(0) }
        var selectedPage by remember { mutableIntStateOf(0) }

        Box {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .widthIn(max = 200.dp)
                    .aspectRatio(1f),
                pageSpacing = 2.dp,
            ) { page ->
                AsyncImage(
                    model = uris[page],
                    contentDescription = stringResource(R.string.mms_body_placeholder),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(bubbleShape)
                        .combinedClickable(
                            onClick = {
                                selectedPage = page
                                dialogIndex = page
                                showDialog = true
                            },
                            onLongClick = onLongClickAction,
                        ),
                    contentScale = ContentScale.Crop,
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                repeat(uris.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (pagerState.currentPage == index) 7.dp else 5.dp)
                            .clip(CircleShape)
                            .background(
                                if (pagerState.currentPage == index)
                                    Color.White
                                else
                                    Color.White.copy(alpha = 0.45f)
                            ),
                    )
                }
            }
        }
        if (showDialog) {
            MmsImageDialog(uris = uris, initialIndex = dialogIndex, onDismiss = { showDialog = false })
        }
    }
}

@Composable
private fun MmsImageDialog(
    uris: List<Uri>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { uris.size })
    var currentIndex by remember { mutableIntStateOf(initialIndex) }
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    LaunchedEffect(initialIndex) {
        if (uris.size > 1 && initialIndex != pagerState.currentPage) {
            pagerState.animateScrollToPage(initialIndex)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f)),
            contentAlignment = Alignment.Center,
        ) {
            if (uris.size == 1) {
                SingleImageViewer(
                    uri = uris[0],
                    scale = scale,
                    offsetX = offsetX,
                    offsetY = offsetY,
                    onScaleChange = { scale = it },
                    onOffsetChange = { x, y -> offsetX = x; offsetY = y },
                )
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    IconButton(
                        onClick = {
                            val savedUri = saveMmsImage(context, uris[0])
                            if (savedUri != null) {
                                android.widget.Toast
                                    .makeText(context, context.getString(R.string.mms_image_saved), android.widget.Toast.LENGTH_SHORT)
                                    .show()
                                try {
                                    val viewIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                        setDataAndType(savedUri, "image/jpeg")
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(viewIntent)
                                } catch (_: android.content.ActivityNotFoundException) {}
                            } else {
                                android.widget.Toast
                                    .makeText(context, context.getString(R.string.mms_image_save_failed), android.widget.Toast.LENGTH_SHORT)
                                    .show()
                            }
                            onDismiss()
                        },
                        modifier = Modifier
                            .padding(bottom = 24.dp)
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = stringResource(R.string.mms_image_download), tint = Color.White)
                    }
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    currentIndex = page
                    var pageScale by remember(page) { mutableStateOf(1f) }
                    var pageOffsetX by remember(page) { mutableStateOf(0f) }
                    var pageOffsetY by remember(page) { mutableStateOf(0f) }

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = uris[page],
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clipToBounds()
                                .pointerInput(pageScale) {
                                    if (pageScale > 1f) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            val newScale = (pageScale * zoom).coerceIn(1f, 5f)
                                            pageScale = newScale
                                            if (newScale > 1f) {
                                                pageOffsetX += pan.x
                                                pageOffsetY += pan.y
                                            } else {
                                                pageOffsetX = 0f
                                                pageOffsetY = 0f
                                            }
                                        }
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            if (pageScale > 1f) {
                                                pageScale = 1f
                                                pageOffsetX = 0f
                                                pageOffsetY = 0f
                                            } else {
                                                pageScale = 2.5f
                                            }
                                        }
                                    )
                                }
                                .graphicsLayer {
                                    scaleX = pageScale
                                    scaleY = pageScale
                                    translationX = pageOffsetX
                                    translationY = pageOffsetY
                                },
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        repeat(uris.size) { index ->
                            Box(
                                modifier = Modifier
                                    .size(if (pagerState.currentPage == index) 8.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (pagerState.currentPage == index)
                                            Color.White
                                        else
                                            Color.White.copy(alpha = 0.45f)
                                    ),
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            val uri = uris.getOrNull(currentIndex) ?: return@IconButton
                            val savedUri = saveMmsImage(context, uri)
                            if (savedUri != null) {
                                android.widget.Toast
                                    .makeText(context, context.getString(R.string.mms_image_saved), android.widget.Toast.LENGTH_SHORT)
                                    .show()
                                try {
                                    val viewIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                        setDataAndType(savedUri, "image/jpeg")
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(viewIntent)
                                } catch (_: android.content.ActivityNotFoundException) {}
                            } else {
                                android.widget.Toast
                                    .makeText(context, context.getString(R.string.mms_image_save_failed), android.widget.Toast.LENGTH_SHORT)
                                    .show()
                            }
                            onDismiss()
                        },
                        modifier = Modifier
                            .padding(bottom = 24.dp)
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = stringResource(R.string.mms_image_download), tint = Color.White)
                    }
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
            ) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_close), tint = Color.White)
            }
        }
    }
}

@Composable
private fun SingleImageViewer(
    uri: Uri,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    onScaleChange: (Float) -> Unit,
    onOffsetChange: (Float, Float) -> Unit,
) {
    AsyncImage(
        model = uri,
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                    onScaleChange(newScale)
                    if (newScale > 1f) {
                        onOffsetChange(offsetX + pan.x, offsetY + pan.y)
                    } else {
                        onOffsetChange(0f, 0f)
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            onScaleChange(1f)
                            onOffsetChange(0f, 0f)
                        } else {
                            onScaleChange(2.5f)
                        }
                    }
                )
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offsetX
                translationY = offsetY
            },
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun VoiceMessagePlayer(
    uri: Uri,
    isOutbound: Boolean,
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var durationMs by remember { mutableIntStateOf(0) }
    var currentPositionMs by remember { mutableIntStateOf(0) }

    DisposableEffect(uri) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    LaunchedEffect(uri) {
        try {
            val tempPlayer = MediaPlayer().apply {
                setDataSource(context, uri)
                prepare()
            }
            durationMs = tempPlayer.duration
            tempPlayer.release()
        } catch (_: Exception) { }
    }

    LaunchedEffect(isPlaying, uri) {
        while (isActive) {
            if (isPlaying) {
                mediaPlayer?.let { mp ->
                    currentPositionMs = try {
                        mp.currentPosition
                    } catch (e: Exception) {
                        0
                    }
                }
                delay(33.milliseconds)
            } else {
                delay(100.milliseconds)
            }
        }
    }

    val colors = MaterialTheme.colorScheme
    val tintColor = if (isOutbound) colors.onPrimaryContainer else colors.onSurface
    val waveActive = if (isOutbound) colors.onPrimaryContainer else colors.primary
    val waveInactive = if (isOutbound) colors.onPrimaryContainer.copy(alpha = 0.20f) else colors.primary.copy(alpha = 0.18f)
    val progress = if (durationMs > 0) {
        (currentPositionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    } else 0f

    Row(
        modifier = Modifier
            .widthIn(min = 160.dp, max = 220.dp)
            .padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        IconButton(
            onClick = {
                val player = mediaPlayer
                if (isPlaying) {
                    player?.pause()
                    isPlaying = false
                } else {
                    if (player == null) {
                        try {
                            val newPlayer = MediaPlayer().apply {
                                setDataSource(context, uri)
                                prepare()
                                durationMs = duration
                                setOnCompletionListener {
                                    isPlaying = false
                                    currentPositionMs = 0
                                    seekTo(0)
                                }
                            }
                            mediaPlayer = newPlayer
                            newPlayer.start()
                            isPlaying = true
                        } catch (e: Exception) {
                            Log.e("VoiceMessagePlayer", "Failed to create MediaPlayer", e)
                        }
                    } else {
                        player.seekTo(currentPositionMs)
                        player.start()
                        isPlaying = true
                    }
                }
            },
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = stringResource(
                    if (isPlaying) R.string.voice_message_stop else R.string.voice_message_play
                ),
                modifier = Modifier.size(22.dp),
                tint = tintColor,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            AudioWaveformPreview(
                uri = uri,
                activeColor = waveActive,
                inactiveColor = waveInactive,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                targetBars = 56,
                progress = progress,
                onSeek = { fraction ->
                    val newPos = (fraction * durationMs).toInt()
                    mediaPlayer?.seekTo(newPos)
                    currentPositionMs = newPos
                },
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatVoiceDuration(
                        if (isPlaying || currentPositionMs > 0) currentPositionMs else 0
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = tintColor.copy(alpha = 0.8f),
                )
                Text(
                    text = formatVoiceDuration(durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = tintColor.copy(alpha = 0.8f),
                )
            }
        }
    }
}

private fun formatVoiceDuration(ms: Int): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}

private fun saveMmsImage(context: android.content.Context, uri: Uri): Uri? {
    try {
        val resolver = context.contentResolver
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null

        val filename = "Pulse_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Pulse")
            }
        }
        val outputUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (outputUri != null) {
            resolver.openOutputStream(outputUri)?.use { output ->
                output.write(bytes)
            }
            return outputUri
        }
    } catch (e: Exception) {
        Log.e("MmsImageDialog", "Failed to save image", e)
    }
    return null
}
