@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.skeler.pulse.design.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.skeler.pulse.design.theme.LocalReduceMotion

/**
 * Toolbar state for [SerafinaDockedToolbar].
 */
sealed interface ToolbarState {
    data object Collapsed : ToolbarState
    data object Expanded : ToolbarState
}

/**
 * An expressive docked toolbar that animates between [Collapsed] and [Expanded] states.
 *
 * - **Collapsed** (56dp): shows a row of 4 action icon buttons.
 * - **Expanded** (72dp): shows a text field + send action.
 *
 * Transition is triggered by the TextField gaining/losing focus.
 *
 * @param text Current draft text.
 * @param onTextChange Called when the user types.
 * @param onSendClick Called when the send button is tapped.
 * @param isSendEnabled Whether the send button is active.
 * @param cameraIcon Icon for camera action.
 * @param galleryIcon Icon for gallery action.
 * @param voiceIcon Icon for voice action.
 * @param emojiIcon Icon for emoji action.
 * @param sendIcon Icon for send action.
 */
@Composable
fun SerafinaDockedToolbar(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isSendEnabled: Boolean,
    cameraIcon: ImageVector,
    galleryIcon: ImageVector,
    voiceIcon: ImageVector,
    emojiIcon: ImageVector,
    sendIcon: ImageVector,
    modifier: Modifier = Modifier,
    placeholder: String = "Message",
) {
    val reduceMotion = LocalReduceMotion.current
    var toolbarState by remember { mutableStateOf<ToolbarState>(ToolbarState.Collapsed) }

    val targetHeight = when (toolbarState) {
        ToolbarState.Collapsed -> 56.dp
        ToolbarState.Expanded -> 72.dp
    }

    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = if (reduceMotion) {
            spring(
                stiffness = Spring.StiffnessHigh,
                dampingRatio = Spring.DampingRatioNoBouncy,
            )
        } else {
            spring(
                stiffness = 400f,
                dampingRatio = 0.6f,
            )
        },
        label = "toolbar_height",
    )

    BottomAppBar(
        modifier = modifier.height(animatedHeight),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 2.dp,
    ) {
        when (toolbarState) {
            ToolbarState.Collapsed -> {
                // 4 action buttons row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { /* Camera */ }) {
                        Icon(cameraIcon, contentDescription = "Camera")
                    }
                    IconButton(onClick = { /* Gallery */ }) {
                        Icon(galleryIcon, contentDescription = "Gallery")
                    }
                    IconButton(onClick = { /* Voice */ }) {
                        Icon(voiceIcon, contentDescription = "Voice")
                    }
                    IconButton(onClick = { /* Emoji */ }) {
                        Icon(emojiIcon, contentDescription = "Emoji")
                    }
                }
            }

            ToolbarState.Expanded -> {
                // Text field + send button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        BasicTextField(
                            value = text,
                            onValueChange = onTextChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { state ->
                                    toolbarState = if (state.isFocused) {
                                        ToolbarState.Expanded
                                    } else if (text.isBlank()) {
                                        ToolbarState.Collapsed
                                    } else {
                                        ToolbarState.Expanded
                                    }
                                },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            singleLine = false,
                            maxLines = 4,
                            decorationBox = { innerTextField ->
                                if (text.isEmpty()) {
                                    Text(
                                        text = placeholder,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                innerTextField()
                            },
                        )
                    }
                    IconButton(
                        onClick = onSendClick,
                        enabled = isSendEnabled,
                    ) {
                        Icon(
                            imageVector = sendIcon,
                            contentDescription = "Send",
                            tint = if (isSendEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }
    }
}
