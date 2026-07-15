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


internal val ConversationPillShape = RoundedCornerShape(999.dp)
internal const val ConversationHeaderContentType = "conversation_header"
internal const val ConversationLoadingContentType = "conversation_loading"
internal const val ConversationEmptyContentType = "conversation_empty"
internal const val ConversationTimelineStartLazyListIndex = 1

internal object ConversationVisualTokens {
    val timelineHorizontalPadding = 16.dp
    val timelineVerticalPadding = 12.dp
    val messageMaxWidth = 324.dp
    val overviewShape = RoundedCornerShape(28.dp)
    val topBarTitleShape = RoundedCornerShape(28.dp)
    val bubbleOutlineWidth = 1.dp
    val composerScrimStartY = 0.36f
    val backdropSurfaceStop = 0.32f
    const val topBarSurfaceAlpha = 0.88f
    const val activeBubbleOutlineAlpha = 0.46f
    const val pressedBubbleOutlineAlpha = 0.30f
    const val unreadBubbleOutlineAlpha = 0.42f
    const val failedBubbleOutlineAlpha = 0.52f
    const val restingOutboundOutlineAlpha = 0.18f
    const val restingInboundOutlineAlpha = 0.28f
    const val inboundBubbleAlpha = 0.82f
    const val unreadBubbleAlpha = 0.76f
    const val failedBubbleAlpha = 0.78f
    const val outboundBubbleAlpha = 0.90f
}

internal object ConversationComposerTokens {
    val outerHorizontalPadding = 14.dp
    val outerTopPadding = 6.dp
    val outerBottomPadding = 10.dp
    val contentSpacing = 8.dp
    val capsuleHorizontalPadding = 8.dp
    val capsuleVerticalPadding = 5.dp
    val capsuleMinHeight = 58.dp
    val textFieldMinHeight = 48.dp
    val textFieldHorizontalPadding = 4.dp
    val textFieldVerticalPadding = 12.dp
    val simBadgeMinWidth = 48.dp
    val simBadgeHeight = 36.dp
    val simBadgeHorizontalPadding = 10.dp
    val simBadgeContentSpacing = 5.dp
    val simIconSize = 14.dp
    val sendButtonSize = 52.dp
    val sendIconSize = 23.dp
    val sendIconHorizontalOffset = (-1).dp
    val borderWidth = 1.dp
    const val surfaceAlpha = 0.84f
    const val inactiveAlpha = 0.54f
    const val disabledSendIconAlpha = 0.38f
    const val placeholderAlpha = 0.52f
    const val focusedCapsuleAccentAlpha = 0.26f
    const val restingCapsuleAccentAlpha = 0.14f
}

internal fun <T> conversationPressAnimationSpec(reducedMotion: Boolean): FiniteAnimationSpec<T> =
    if (reducedMotion) {
        tween(durationMillis = 0)
    } else {
        spring(
            stiffness = Spring.StiffnessMedium,
            dampingRatio = Spring.DampingRatioNoBouncy,
        )
    }

@Composable
internal fun conversationBackdropBrush(): Brush {
    val colors = MaterialTheme.colorScheme
    return colors.screenBackgroundBrush {
        Brush.verticalGradient(
            0f to colors.surfaceContainerLowest,
            ConversationVisualTokens.backdropSurfaceStop to colors.surfaceContainerLow,
            1f to colors.surface,
        )
    }
}

@Composable
internal fun conversationTopBarBrush(): Brush {
    val colors = MaterialTheme.colorScheme
    return Brush.verticalGradient(
        0f to colors.surface.copy(alpha = ConversationVisualTokens.topBarSurfaceAlpha),
        1f to colors.surface.copy(alpha = 0.68f),
    )
}

internal fun conversationTopBarChromeContainerColor(hasUnreadMessages: Boolean): Color =
    Color.Transparent

internal fun conversationTopBarTitleContainerColor(
    colors: ColorScheme,
    hasUnreadMessages: Boolean,
): Color = if (hasUnreadMessages) {
    colors.tertiaryContainer.copy(alpha = 0.84f)
} else {
    Color.Transparent
}

internal fun conversationTopBarContentColor(
    colors: ColorScheme,
    hasUnreadMessages: Boolean,
): Color = if (hasUnreadMessages) {
    colors.onTertiaryContainer
} else {
    colors.onSurface
}

@Composable
internal fun conversationComposerScrimBrush(): Brush {
    val colors = MaterialTheme.colorScheme
    return Brush.verticalGradient(
        0f to Color.Transparent,
        ConversationVisualTokens.composerScrimStartY to colors.surface.copy(alpha = 0.82f),
        1f to colors.surface.copy(alpha = 0.96f),
    )
}

@Composable
internal fun conversationComposerCapsuleBrush(isFocused: Boolean): Brush {
    val colors = MaterialTheme.colorScheme
    val accentAlpha = if (isFocused) {
        ConversationComposerTokens.focusedCapsuleAccentAlpha
    } else {
        ConversationComposerTokens.restingCapsuleAccentAlpha
    }
    return Brush.horizontalGradient(
        0f to colors.surfaceContainerHighest.copy(alpha = ConversationComposerTokens.surfaceAlpha),
        0.62f to colors.primaryContainer.copy(alpha = accentAlpha),
        1f to colors.surfaceContainerHigh.copy(alpha = ConversationComposerTokens.surfaceAlpha),
    )
}

internal fun conversationBubbleContainerColor(
    colors: ColorScheme,
    isOutbound: Boolean,
    isUnread: Boolean,
    hasFailedDelivery: Boolean,
): Color = when {
    hasFailedDelivery -> colors.errorContainer.copy(alpha = ConversationVisualTokens.failedBubbleAlpha)
    isOutbound -> colors.primaryContainer.copy(alpha = ConversationVisualTokens.outboundBubbleAlpha)
    isUnread -> colors.tertiaryContainer.copy(alpha = ConversationVisualTokens.unreadBubbleAlpha)
    else -> colors.surfaceContainerLow.copy(alpha = ConversationVisualTokens.inboundBubbleAlpha)
}

internal fun conversationRestingBubbleOutlineColor(
    colors: ColorScheme,
    isOutbound: Boolean,
): Color = if (isOutbound) {
    colors.primary.copy(alpha = ConversationVisualTokens.restingOutboundOutlineAlpha)
} else {
    colors.outlineVariant.copy(alpha = ConversationVisualTokens.restingInboundOutlineAlpha)
}

internal data class ConversationAvatarColors(
    val containerColor: Color,
    val contentColor: Color,
)

internal fun shouldShowMessageBlockAction(isOutbound: Boolean): Boolean = !isOutbound

internal fun SystemSms.hasFailedDelivery(): Boolean = type == Telephony.Sms.MESSAGE_TYPE_FAILED

internal fun draftAfterSendState(currentDraft: String, sendState: SendState): String = when (sendState) {
    is SendState.Sent -> ""
    is SendState.Failed -> currentDraft.ifBlank { sendState.body }
    else -> currentDraft
}

internal fun shouldConfirmDiscardDraft(draft: String): Boolean = draft.isNotBlank()

internal fun conversationTimelineLazyListIndex(timelineIndex: Int): Int =
    timelineIndex + ConversationTimelineStartLazyListIndex
