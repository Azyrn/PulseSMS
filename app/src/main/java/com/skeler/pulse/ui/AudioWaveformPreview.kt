package com.skeler.pulse.ui

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
) {
    val context = LocalContext.current
    var state by remember(uri) { mutableStateOf<WaveformUiState>(WaveformUiState.Loading) }

    LaunchedEffect(uri) {
        val data = WaveformGenerator.generate(context, uri, targetBars)
        state = if (data != null) WaveformUiState.Ready(data) else WaveformUiState.Error
    }

    val primary = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

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
                    color = onSurfaceVariant,
                )
            }
        }
        is WaveformUiState.Ready -> {
            val amplitudes = s.amplitudes
            if (amplitudes.isEmpty()) {
                MicFallback(modifier, primary, surfaceVariant)
            } else {
                WaveformCanvas(
                    amplitudes = amplitudes,
                    modifier = modifier,
                    activeColor = primary,
                    inactiveColor = primary.copy(alpha = 0.18f),
                    progress = progress,
                )
            }
        }
        is WaveformUiState.Error -> {
            MicFallback(modifier, primary, surfaceVariant)
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
) {
    Canvas(modifier = modifier.clip(RoundedCornerShape(8.dp))) {
        val barCount = amplitudes.size
        val w = size.width
        val h = size.height
        val midY = h / 2f
        val maxBarHeight = h * 0.90f

        val barWidth = w / barCount
        val gap = barWidth * 0.22f
        val drawWidth = (barWidth - gap).coerceAtLeast(1f)
        val radius = CornerRadius(drawWidth / 2f)

        val progressIndex = if (progress != null) (progress * barCount).toInt().coerceIn(0, barCount) else barCount

        for (i in amplitudes.indices) {
            val amp = amplitudes[i].coerceIn(0f, 1f)
            val barHeight = amp * maxBarHeight
            if (barHeight < 0.3f) continue

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
