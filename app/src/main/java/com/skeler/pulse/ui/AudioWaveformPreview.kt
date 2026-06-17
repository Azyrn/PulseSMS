package com.skeler.pulse.ui

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skeler.pulse.R
import com.skeler.pulse.sms.WaveformGenerator

private sealed interface WaveformUiState {
    data object Loading : WaveformUiState
    data class Ready(val amplitudes: List<Float>) : WaveformUiState
    data object Error : WaveformUiState
}

@Composable
internal fun AudioWaveformPreview(
    uri: Uri,
    modifier: Modifier = Modifier,
    targetBars: Int = 56,
    progress: Float? = null,
    onSeek: ((Float) -> Unit)? = null,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = activeColor.copy(alpha = 0.18f),
) {
    val context = LocalContext.current
    var state by remember(uri) { mutableStateOf<WaveformUiState>(WaveformUiState.Loading) }

    LaunchedEffect(uri) {
        val data = WaveformGenerator.generate(context, uri, targetBars)
        state = if (data != null) WaveformUiState.Ready(data) else WaveformUiState.Error
    }

    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    when (val s = state) {
        is WaveformUiState.Loading -> {
            Box(
                modifier = modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = inactiveColor,
                )
            }
        }
        is WaveformUiState.Ready -> {
            val amplitudes = s.amplitudes
            if (amplitudes.isEmpty()) {
                MicFallback(modifier, activeColor, surfaceVariant)
            } else {
                WaveformCanvas(
                    amplitudes = amplitudes,
                    modifier = modifier,
                    activeColor = activeColor,
                    inactiveColor = inactiveColor,
                    progress = progress,
                    onSeek = onSeek,
                )
            }
        }
        is WaveformUiState.Error -> {
            MicFallback(modifier, activeColor, surfaceVariant)
        }
    }
}

@Composable
private fun WaveformCanvas(
    amplitudes: List<Float>,
    modifier: Modifier,
    activeColor: Color,
    inactiveColor: Color,
    progress: Float?,
    onSeek: ((Float) -> Unit)? = null,
) {
    val currentOnSeek = rememberUpdatedState(onSeek)
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (onSeek != null) {
                    Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            val w = this.size.width.toFloat()
                            val startX = down.position.x.coerceIn(0f, w)
                            currentOnSeek.value?.invoke(startX / w)
                            var lastX = startX
                            do {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (change.pressed) {
                                    change.consume()
                                    lastX = change.position.x.coerceIn(0f, w)
                                    currentOnSeek.value?.invoke(lastX / w)
                                } else {
                                    break
                                }
                            } while (true)
                        }
                    }
                } else Modifier
            )
    ) {
        val barCount = amplitudes.size
        val w = size.width
        val h = size.height
        val midY = h / 2f
        val maxBarHeight = h * 0.96f

        val barWidth = w / barCount
        val gap = barWidth * 0.22f
        val drawWidth = (barWidth - gap).coerceAtLeast(1f)
        val radius = CornerRadius(drawWidth / 2f)

        val progressIndex = if (progress != null) (progress * barCount).toInt().coerceIn(0, barCount) else barCount

        for (i in amplitudes.indices) {
            val amp = amplitudes[i].coerceIn(0f, 1f)
            val barHeight = maxOf(amp * maxBarHeight, 1f)
            val x = i * barWidth + gap / 2f
            val isPlayed = i < progressIndex
            val color = if (isPlayed) activeColor else inactiveColor

            drawRoundRect(
                color = color,
                topLeft = Offset(x, midY - barHeight / 2f),
                size = Size(drawWidth, barHeight),
                cornerRadius = radius,
            )
        }

        if (progress != null) {
            val cursorX = progress * w
            val cursorWidth = 2.dp.toPx()
            drawRoundRect(
                color = activeColor,
                topLeft = Offset(cursorX - cursorWidth / 2f, 0f),
                size = Size(cursorWidth, h),
                cornerRadius = CornerRadius(cursorWidth / 2f),
            )
        }
    }
}

@Composable
private fun MicFallback(
    modifier: Modifier,
    color: Color,
    background: Color,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Mic,
            contentDescription = stringResource(R.string.attachment_voice),
            modifier = Modifier.size(24.dp),
            tint = color,
        )
    }
}
