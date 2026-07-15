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
internal fun ConversationComposer(
    draft: String,
    sendState: SendState,
    simOptions: List<NewChatSimOption>,
    selectedSimKey: String?,
    onSimOptionClick: (NewChatSimOption) -> Unit,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val isSending = sendState is SendState.Sending
    val canSend by remember(draft, isSending) {
        derivedStateOf { draft.isNotBlank() && !isSending }
    }
    val selectedSim = remember(simOptions, selectedSimKey) {
        simOptions.firstOrNull { option -> option.key == selectedSimKey } ?: simOptions.firstOrNull()
    }
    val characterCounter = remember(draft) { smsCharacterCounterOrNull(draft) }
    val fieldInteractionSource = remember { MutableInteractionSource() }
    val isFocused by fieldInteractionSource.collectIsFocusedAsState()
    val sendInteractionSource = remember { MutableInteractionSource() }
    val isSendPressed by sendInteractionSource.collectIsPressedAsState()
    val colors = MaterialTheme.colorScheme
    val textSelectionColors = remember(colors.primary) {
        TextSelectionColors(
            handleColor = colors.primary,
            backgroundColor = colors.primary.copy(alpha = 0.28f),
        )
    }
    val containerLift by animateFloatAsState(
        targetValue = if (isFocused) -2f else 0f,
        animationSpec = if (reducedMotion) tween(0) else spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioNoBouncy,
        ),
        label = "composer_lift",
    )

    // ── Send button animations (the hero moment) ──
    val sendScale by animateFloatAsState(
        targetValue = when {
            isSendPressed && canSend -> 0.92f
            canSend -> 1f
            else -> 0.88f
        },
        animationSpec = conversationPressAnimationSpec(reducedMotion),
        label = "send_scale",
    )
    val capsuleBorderColor by animateColorAsState(
        targetValue = if (isFocused) {
            colors.primary.copy(alpha = 0.46f)
        } else {
            colors.outlineVariant.copy(alpha = 0.62f)
        },
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else 180),
        label = "composer_capsule_border",
    )
    val sendContainerColor by animateColorAsState(
        targetValue = if (canSend) {
            colors.primary
        } else {
            colors.surfaceContainerHigh.copy(alpha = ConversationComposerTokens.surfaceAlpha)
        },
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else 200),
        label = "send_container",
    )
    val sendBorderColor by animateColorAsState(
        targetValue = if (canSend) {
            colors.primary.copy(alpha = 0.48f)
        } else {
            colors.outlineVariant.copy(alpha = 0.48f)
        },
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else 200),
        label = "send_border",
    )
    val sendContentColor by animateColorAsState(
        targetValue = if (canSend) {
            colors.onPrimary
        } else {
            colors.onSurfaceVariant.copy(alpha = ConversationComposerTokens.disabledSendIconAlpha)
        },
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else 200),
        label = "send_content",
    )
    val capsuleShape = ConversationPillShape

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = containerLift }
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            colors.primary.copy(alpha = if (isFocused) 0.18f else 0.10f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.86f, size.height * 0.50f),
                        radius = size.height * 1.35f,
                    ),
                )
            }
            .padding(
                start = ConversationComposerTokens.outerHorizontalPadding,
                top = ConversationComposerTokens.outerTopPadding,
                end = ConversationComposerTokens.outerHorizontalPadding,
                bottom = ConversationComposerTokens.outerBottomPadding,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ConversationComposerTokens.contentSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(capsuleShape)
                    .background(conversationComposerCapsuleBrush(isFocused))
                    .border(
                        width = ConversationComposerTokens.borderWidth,
                        color = capsuleBorderColor,
                        shape = capsuleShape,
                    )
                    .heightIn(min = ConversationComposerTokens.capsuleMinHeight)
                    .padding(
                        horizontal = ConversationComposerTokens.capsuleHorizontalPadding,
                        vertical = ConversationComposerTokens.capsuleVerticalPadding,
                    ),
                horizontalArrangement = Arrangement.spacedBy(ConversationComposerTokens.contentSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ConversationSimSelector(
                    simOptions = simOptions,
                    selectedSim = selectedSim,
                    enabled = !isSending,
                    onSimOptionClick = onSimOptionClick,
                )
                CompositionLocalProvider(LocalTextSelectionColors provides textSelectionColors) {
                    BasicTextField(
                        value = draft,
                        onValueChange = onDraftChange,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = ConversationComposerTokens.textFieldMinHeight),
                        enabled = !isSending,
                        interactionSource = fieldInteractionSource,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = colors.onSurface,
                        ),
                        cursorBrush = SolidColor(colors.primary),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Send,
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (canSend) {
                                    onSend()
                                }
                            },
                        ),
                        maxLines = 5,
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = ConversationComposerTokens.textFieldHorizontalPadding,
                                        vertical = ConversationComposerTokens.textFieldVerticalPadding,
                                    ),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                if (draft.isBlank()) {
                                    Text(
                                        text = if (isSending) {
                                            stringResource(R.string.conversation_composer_sending)
                                        } else {
                                            stringResource(R.string.conversation_composer_placeholder)
                                        },
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = colors.onSurfaceVariant.copy(
                                            alpha = ConversationComposerTokens.placeholderAlpha,
                                        ),
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )
                }
                characterCounter?.let { counter ->
                    Text(
                        text = "${counter.remainingCharacters} / ${counter.segmentCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(ConversationComposerTokens.sendButtonSize)
                    .graphicsLayer {
                        scaleX = sendScale
                        scaleY = sendScale
                    }
                    .clip(ConversationPillShape)
                    .background(sendContainerColor)
                    .border(
                        width = ConversationComposerTokens.borderWidth,
                        color = sendBorderColor,
                        shape = ConversationPillShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(
                    onClick = {
                        if (canSend) {
                            onSend()
                        }
                    },
                    enabled = canSend,
                    modifier = Modifier.fillMaxSize(),
                    interactionSource = sendInteractionSource,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = stringResource(R.string.conversation_send_message_content_description),
                        modifier = Modifier
                            .size(ConversationComposerTokens.sendIconSize)
                            .offset(x = ConversationComposerTokens.sendIconHorizontalOffset),
                        tint = sendContentColor,
                    )
                }
            }
        }
    }
}
