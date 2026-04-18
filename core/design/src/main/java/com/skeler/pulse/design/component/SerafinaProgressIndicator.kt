@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.skeler.pulse.design.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kotlin.math.sin

/**
 * A sine-modulated wavy progress indicator used during retry / recovery states.
 *
 * The amplitude oscillates via `sin(phase) * 0.3 + 0.7`, creating a pulsing
 * wave effect that communicates ongoing background work.
 *
 * @param modifier Layout modifier.
 * @param progress Optional determinate progress lambda (null = indeterminate).
 */
@Composable
fun SerafinaProgressIndicator(
    modifier: Modifier = Modifier,
    progress: (() -> Float)? = null,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wavy_phase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wavy_phase_value",
    )

    if (progress != null) {
        // Determinate: amplitude is (progress: Float) -> Float
        LinearWavyProgressIndicator(
            progress = progress,
            modifier = modifier,
            amplitude = { _ -> sin(phase.toDouble()).toFloat() * 0.3f + 0.7f },
        )
    } else {
        // Indeterminate: amplitude is a plain Float
        val amplitudeValue = sin(phase.toDouble()).toFloat() * 0.3f + 0.7f
        LinearWavyProgressIndicator(
            modifier = modifier,
            amplitude = amplitudeValue,
        )
    }
}
