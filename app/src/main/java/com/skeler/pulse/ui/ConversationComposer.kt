package com.skeler.pulse.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Telephony
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.Stop
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
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
import androidx.core.content.ContextCompat
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
import coil.compose.AsyncImage
import com.skeler.pulse.sms.SystemSms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.time.Instant


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConversationComposer(
    draft: String,
    sendState: SendState,
    simOptions: List<NewChatSimOption>,
    selectedSimKey: String?,
    selectedImageUris: List<Uri> = emptyList(),
    onSimOptionClick: (NewChatSimOption) -> Unit,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onImageSelected: (List<Uri>) -> Unit = {},
    onImagePickFromGallery: () -> Unit = {},
    onTakePhoto: () -> Unit = {},
    onVoiceRecorded: (Uri) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val isSending = sendState is SendState.Sending
    val canSend by remember(draft, isSending, selectedImageUris) {
        derivedStateOf { (draft.isNotBlank() || selectedImageUris.isNotEmpty()) && !isSending }
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
    val capsuleShape = ConversationCapsuleShape
    val context = LocalContext.current
    val hasCounter = characterCounter?.let { c ->
        c.segmentCount > 1 || c.remainingCharacters < 20
    } == true
    val counterText = characterCounter?.let { c ->
        if (c.segmentCount > 1) {
            "${c.remainingCharacters} / ${c.segmentCount}"
        } else {
            "${c.remainingCharacters}"
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = ConversationComposerTokens.outerHorizontalPadding,
                top = ConversationComposerTokens.outerTopPadding,
                end = ConversationComposerTokens.outerHorizontalPadding,
                bottom = ConversationComposerTokens.outerBottomPadding,
            ),
    ) {
        if (hasCounter) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = ConversationComposerTokens.attachmentButtonSize +
                            ConversationComposerTokens.contentSpacing,
                        bottom = 2.dp,
                    ),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = counterText!!,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 1,
                )
            }
        }
        var showAttachmentMenu by remember { mutableStateOf(false) }
        if (selectedImageUris.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                selectedImageUris.forEach { uri ->
                    Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp))) {
                        AsyncImage(
                            model = uri,
                            contentDescription = stringResource(R.string.conversation_attach_content_description),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                        IconButton(
                            onClick = { onImageSelected(selectedImageUris - uri) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(22.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                                contentDescription = stringResource(R.string.action_remove),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ConversationComposerTokens.contentSpacing),
            verticalAlignment = Alignment.Bottom,
        ) {
            IconButton(
                onClick = { showAttachmentMenu = true },
                enabled = !isSending,
                modifier = Modifier
                    .size(ConversationComposerTokens.attachmentButtonSize),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.conversation_attach_content_description),
                    modifier = Modifier.size(ConversationComposerTokens.attachmentIconSize),
                    tint = colors.onSurfaceVariant.copy(alpha = ConversationComposerTokens.inactiveAlpha),
                )
            }
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
                if (simOptions.size > 1) {
                    ConversationSimSelector(
                        simOptions = simOptions,
                        selectedSim = selectedSim,
                        enabled = !isSending,
                        onSimOptionClick = onSimOptionClick,
                    )
                }
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
        if (showAttachmentMenu) {
            var selectedAttachmentTab by remember { mutableStateOf(AttachmentTab.GALLERY) }
            val mediaPermission = if (Build.VERSION.SDK_INT >= 33) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            val hasMediaPermission = remember {
                ContextCompat.checkSelfPermission(context, mediaPermission) == PackageManager.PERMISSION_GRANTED
            }
            var mediaPermissionGranted by remember { mutableStateOf(hasMediaPermission) }
            val mediaPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { granted -> mediaPermissionGranted = granted }
            val recentPhotos by produceState<List<Uri>>(
                initialValue = emptyList(),
                key1 = showAttachmentMenu,
                key2 = mediaPermissionGranted,
            ) {
                if (!mediaPermissionGranted) return@produceState
                value = withContext(Dispatchers.IO) {
                    val uris = mutableListOf<Uri>()
                    try {
                        context.contentResolver.query(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            arrayOf(MediaStore.Images.Media._ID),
                            null, null,
                            "${MediaStore.Images.Media.DATE_ADDED} DESC"
                        )?.use { cursor ->
                            while (cursor.moveToNext()) {
                                val id = cursor.getLong(0)
                                uris.add(
                                    Uri.withAppendedPath(
                                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                        id.toString()
                                    )
                                )
                            }
                        }
                    } catch (_: SecurityException) {}
                    uris
                }
            }
            ModalBottomSheet(
                onDismissRequest = { showAttachmentMenu = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        // Left: tab buttons
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 12.dp),
                        ) {
                            AttachmentOption(
                                icon = Icons.Rounded.CameraAlt,
                                label = stringResource(R.string.attachment_camera),
                                selected = selectedAttachmentTab == AttachmentTab.CAMERA,
                                iconOnly = true,
                                onClick = { selectedAttachmentTab = AttachmentTab.CAMERA },
                            )
                            AttachmentOption(
                                icon = Icons.Rounded.Photo,
                                label = stringResource(R.string.attachment_gallery),
                                selected = selectedAttachmentTab == AttachmentTab.GALLERY,
                                iconOnly = true,
                                onClick = { selectedAttachmentTab = AttachmentTab.GALLERY },
                            )
                            AttachmentOption(
                                icon = Icons.Rounded.Mic,
                                label = stringResource(R.string.attachment_voice),
                                selected = selectedAttachmentTab == AttachmentTab.VOICE,
                                iconOnly = true,
                                onClick = { selectedAttachmentTab = AttachmentTab.VOICE },
                            )
                        }
                        // Right: tab content
                        var pendingGallerySelection by remember(showAttachmentMenu) { mutableStateOf(emptySet<Uri>()) }
                        when (selectedAttachmentTab) {
                            AttachmentTab.CAMERA -> {
                                CameraPreviewContent(
                                    onPhotoTaken = { uri ->
                                        onImageSelected(selectedImageUris + uri)
                                        showAttachmentMenu = false
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(max = 320.dp),
                                )
                            }
                            AttachmentTab.VOICE -> {
                                VoiceRecordingContent(
                                    onVoiceRecorded = { uri ->
                                        showAttachmentMenu = false
                                        onVoiceRecorded(uri)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(max = 320.dp),
                                )
                            }
                            AttachmentTab.GALLERY -> {
                                Column(modifier = Modifier.weight(1f)) {
                                    if (!mediaPermissionGranted) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                        ) {
                                                Text(
                                                    text = stringResource(R.string.attachment_allow_access),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                                Spacer(Modifier.height(8.dp))
                                                FilledTonalButton(
                                                    onClick = { mediaPermissionLauncher.launch(mediaPermission) },
                                                ) {
                                                    Text(stringResource(R.string.attachment_grant_permission))
                                                }
                                        }
                                    } else if (recentPhotos.isNotEmpty()) {
                                        LazyVerticalGrid(
                                            columns = GridCells.Fixed(3),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 240.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            recentPhotos.forEach { uri ->
                                                item(key = uri.toString()) {
                                                    Box(modifier = Modifier.aspectRatio(1f)) {
                                                        AsyncImage(
                                                            model = uri,
                                                            contentDescription = null,
                                                            modifier = Modifier
                                                                .matchParentSize()
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .clickable {
                                                                    pendingGallerySelection = if (uri in pendingGallerySelection) {
                                                                        pendingGallerySelection - uri
                                                                    } else {
                                                                        pendingGallerySelection + uri
                                                                    }
                                                                },
                                                            contentScale = ContentScale.Crop,
                                                        )
                                                        if (uri in pendingGallerySelection) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .matchParentSize()
                                                                    .background(Color.Black.copy(alpha = 0.3f))
                                                                    .clip(RoundedCornerShape(4.dp)),
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Rounded.CheckCircle,
                                                                    contentDescription = stringResource(R.string.attachment_selected),
                                                                    modifier = Modifier
                                                                        .align(Alignment.TopEnd)
                                                                        .padding(3.dp)
                                                                        .size(20.dp),
                                                                    tint = MaterialTheme.colorScheme.primary,
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 100.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = stringResource(R.string.attachment_no_recent_photos),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    if (pendingGallerySelection.isNotEmpty()) {
                                        Spacer(Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                            horizontalArrangement = Arrangement.Center,
                                        ) {
                                            FilledTonalButton(
                                                onClick = {
                                                    onImageSelected(selectedImageUris + pendingGallerySelection.toList())
                                                    showAttachmentMenu = false
                                                },
                                            ) {
                                                Text(stringResource(R.string.attachment_gallery_add, pendingGallerySelection.size))
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class AttachmentTab { CAMERA, GALLERY, VOICE }

@Composable
private fun AttachmentOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean = false,
    iconOnly: Boolean = false,
    onClick: () -> Unit,
) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            )
            .padding(if (iconOnly) 10.dp else 24.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(if (iconOnly) 28.dp else 40.dp),
            tint = tint,
        )
        if (selected) {
            Spacer(modifier = Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .width(if (iconOnly) 18.dp else 24.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
        if (!iconOnly) {
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = tint,
            )
        }
    }
}

@Composable
private fun CameraPreviewContent(
    onPhotoTaken: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val hasCameraPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    var cameraPermissionGranted by remember { mutableStateOf(hasCameraPermission) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> cameraPermissionGranted = granted }

    if (!cameraPermissionGranted) {
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.camera_permission_required),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(
                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            ) {
                Text(stringResource(R.string.attachment_grant_permission))
            }
        }
        return
    }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProvider = ProcessCameraProvider.getInstance(ctx).get()

                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    imageCapture = capture

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        capture,
                    )

                    previewView
                },
                modifier = Modifier.fillMaxSize(),
            )
            IconButton(
                onClick = {
                    val capture = imageCapture ?: return@IconButton
                    val photoFile = createCameraImageFile(context)
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                    capture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                onPhotoTaken(Uri.fromFile(photoFile))
                            }
                            override fun onError(exception: ImageCaptureException) {
                                Log.e("CameraPreview", "Photo capture failed", exception)
                            }
                        },
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.85f)),
            ) {
                Icon(
                    imageVector = Icons.Rounded.CameraAlt,
                    contentDescription = stringResource(R.string.attachment_take_photo),
                    modifier = Modifier.size(28.dp),
                    tint = Color.Black,
                )
            }
        }
    }
}

private fun createCameraImageFile(context: android.content.Context): java.io.File {
    val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
    val imageDir = java.io.File(context.cacheDir, "camera_photos")
    imageDir.mkdirs()
    return java.io.File(imageDir, "MMS_$timeStamp.jpg")
}

private sealed interface VoiceRecordingUiState {
    data object Idle : VoiceRecordingUiState
    data class Recording(
        val startMs: Long = System.currentTimeMillis(),
        val amplitudes: List<Float> = emptyList(),
    ) : VoiceRecordingUiState
    data class Preview(val file: java.io.File) : VoiceRecordingUiState
}

@Composable
private fun VoiceRecordingContent(
    onVoiceRecorded: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var state by remember { mutableStateOf<VoiceRecordingUiState>(VoiceRecordingUiState.Idle) }
    var audioFile by remember { mutableStateOf<java.io.File?>(null) }
    val recorder = remember { mutableStateOf<MediaRecorder?>(null) }

    val hasRecordPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }
    var recordPermissionGranted by remember { mutableStateOf(hasRecordPermission) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> recordPermissionGranted = granted }

    LaunchedEffect(state) {
        if (state is VoiceRecordingUiState.Recording) {
            val s = state as VoiceRecordingUiState.Recording
            var amps = s.amplitudes
            while (true) {
                delay(50)
                if (state !is VoiceRecordingUiState.Recording) break
                val amp = recorder.value?.maxAmplitude ?: 0
                val normalized = (amp.toFloat() / 32767f).coerceIn(0f, 1f)
                amps = (amps + normalized).takeLast(120)
                state = s.copy(amplitudes = amps)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val rec = recorder.value
            if (rec != null) {
                try { rec.stop() } catch (_: Exception) {}
                rec.release()
            }
            recorder.value = null
            val s = state
            if (s is VoiceRecordingUiState.Preview) {
                s.file.delete()
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (val s = state) {
            is VoiceRecordingUiState.Idle -> {
                if (!recordPermissionGranted) {
                    Text(
                        text = stringResource(R.string.voice_recording_permission_required),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    FilledTonalButton(
                        onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    ) {
                        Text(stringResource(R.string.attachment_grant_permission))
                    }
                } else {
                    Spacer(Modifier.height(16.dp))
                    IconButton(
                        onClick = {
                            val file = createVoiceFile(context)
                            audioFile = file
                            try {
                                val rec = MediaRecorder().apply {
                                    setAudioSource(MediaRecorder.AudioSource.MIC)
                                    setOutputFormat(MediaRecorder.OutputFormat.AMR_NB)
                                    setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                                    setOutputFile(file.absolutePath)
                                    prepare()
                                    start()
                                }
                                recorder.value = rec
                                state = VoiceRecordingUiState.Recording()
                            } catch (e: Exception) {
                                Log.e("VoiceRecording", "Failed to start recording", e)
                                file.delete()
                                audioFile = null
                            }
                        },
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = stringResource(R.string.voice_recording_start),
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.voice_recording_start),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            is VoiceRecordingUiState.Recording -> {
                val elapsedMs = System.currentTimeMillis() - s.startMs
                Spacer(Modifier.height(8.dp))
                Text(
                    text = formatRecordingDuration(elapsedMs),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .padding(horizontal = 24.dp),
                ) {
                    LiveRecordingWaveform(
                        amplitudes = s.amplitudes,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = {
                            val rec = recorder.value
                            try { rec?.stop() } catch (_: Exception) {}
                            rec?.release()
                            recorder.value = null
                            val file = audioFile
                            if (file != null && file.exists() && file.length() > 0) {
                                state = VoiceRecordingUiState.Preview(file)
                            } else {
                                audioFile?.delete()
                                audioFile = null
                                state = VoiceRecordingUiState.Idle
                            }
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Stop,
                            contentDescription = stringResource(R.string.voice_recording_stop),
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            is VoiceRecordingUiState.Preview -> {
                val uri = Uri.fromFile(s.file)
                var isPlaying by remember { mutableStateOf(false) }
                var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
                var durationMs by remember { mutableIntStateOf(0) }
                var currentPositionMs by remember { mutableIntStateOf(0) }
                val progress = if (durationMs > 0) {
                    (currentPositionMs.toFloat() / durationMs).coerceIn(0f, 1f)
                } else 0f

                DisposableEffect(uri) {
                    onDispose {
                        mediaPlayer?.release()
                        mediaPlayer = null
                    }
                }

                LaunchedEffect(isPlaying) {
                    while (isActive) {
                        if (isPlaying) {
                            mediaPlayer?.let { mp ->
                                currentPositionMs = try { mp.currentPosition } catch (_: Exception) { 0 }
                            }
                            delay(33)
                        } else {
                            delay(100)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconButton(
                        onClick = {
                            if (isPlaying) {
                                mediaPlayer?.pause()
                                isPlaying = false
                            } else {
                                val player = mediaPlayer
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
                                        Log.e("VoicePreview", "Failed to create MediaPlayer", e)
                                    }
                                } else {
                                    player.seekTo(0)
                                    currentPositionMs = 0
                                    player.start()
                                    isPlaying = true
                                }
                            }
                        },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) stringResource(R.string.voice_message_stop) else stringResource(R.string.voice_message_play),
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    AudioWaveformPreview(
                        uri = uri,
                        activeColor = MaterialTheme.colorScheme.primary,
                        inactiveColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        targetBars = 56,
                        progress = if (isPlaying || currentPositionMs > 0) progress else null,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = formatVoiceDuration(
                            if (isPlaying || currentPositionMs > 0) currentPositionMs else 0
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                    Text(
                        text = formatVoiceDuration(durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledTonalIconButton(
                        onClick = {
                            s.file.delete()
                            audioFile = null
                            state = VoiceRecordingUiState.Idle
                        },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = stringResource(R.string.action_cancel),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    IconButton(
                        onClick = {
                            onVoiceRecorded(uri)
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = stringResource(R.string.quick_compose_send_action),
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun LiveRecordingWaveform(
    amplitudes: List<Float>,
    modifier: Modifier = Modifier,
) {
    val color = MaterialTheme.colorScheme.error
    Canvas(modifier = modifier.clip(RoundedCornerShape(8.dp))) {
        if (amplitudes.isEmpty()) return@Canvas
        val barCount = amplitudes.size
        val w = size.width
        val h = size.height
        val midY = h / 2f
        val maxBarHeight = h * 0.96f

        val barWidth = w / barCount
        val gap = barWidth * 0.22f
        val drawWidth = (barWidth - gap).coerceAtLeast(1f)
        val radius = CornerRadius(drawWidth / 2f)

        for (i in amplitudes.indices) {
            val amp = amplitudes[i].coerceIn(0f, 1f)
            val barHeight = maxOf(amp * maxBarHeight, 1f)
            val x = i * barWidth + gap / 2f
            drawRoundRect(
                color = color,
                topLeft = Offset(x, midY - barHeight / 2f),
                size = Size(drawWidth, barHeight),
                cornerRadius = radius,
            )
        }
    }
}

private fun createVoiceFile(context: android.content.Context): java.io.File {
    val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
    val voiceDir = java.io.File(context.cacheDir, "voice_messages")
    voiceDir.mkdirs()
    return java.io.File(voiceDir, "voice_$timeStamp.amr")
}

private fun formatRecordingDuration(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}

private fun formatVoiceDuration(ms: Int): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}
