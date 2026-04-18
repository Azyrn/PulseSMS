package com.skeler.pulse.design.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import com.skeler.pulse.design.theme.LocalReduceMotion
import kotlinx.coroutines.delay

/**
 * Three-stage shape-morph animation states for the send button.
 */
private enum class SendMorphStage {
    /** Idle squircle — ready to send. */
    IDLE,
    /** Sending circle — in-flight. */
    SENDING,
    /** Success checkmark star — confirmed. */
    SUCCESS,
}

/**
 * An expressive send button that animates through three [RoundedPolygon] keyframes:
 *
 * 1. **Idle** → squircle (4 vertices, large rounding)
 * 2. **Sending** → circle (smooth transition, stiffness=600, damping=0.7)
 * 3. **Success** → star/checkmark shape (stiffness=300, damping=0.5)
 * 4. Returns to **Idle**
 *
 * The shape is drawn via Canvas using Morph.toPath(progress).asComposePath().
 *
 * @param onClick Called when the button is tapped.
 * @param enabled Whether the button is interactive.
 * @param isSending Whether a send is currently in-flight (triggers animation).
 * @param isSuccess Whether the send succeeded (triggers success keyframe).
 */
@Composable
fun SendButtonMorph(
    onClick: () -> Unit,
    enabled: Boolean,
    isSending: Boolean,
    isSuccess: Boolean,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = LocalReduceMotion.current

    // Pre-instantiate polygons in remember blocks — never inside draw loops
    val idlePolygon = remember {
        RoundedPolygon(
            numVertices = 4,
            rounding = CornerRounding(0.4f),
        )
    }
    val circlePolygon = remember { RoundedPolygon.circle() }
    val successPolygon = remember {
        RoundedPolygon.star(
            numVerticesPerRadius = 6,
            innerRadius = 0.7f,
            rounding = CornerRounding(0.2f),
        )
    }

    val morphIdleToCircle = remember { Morph(idlePolygon, circlePolygon) }
    val morphCircleToSuccess = remember { Morph(circlePolygon, successPolygon) }
    val morphSuccessToIdle = remember { Morph(successPolygon, idlePolygon) }

    // Determine current stage
    var stageIndex by remember { mutableIntStateOf(0) } // 0=idle, 1=sending, 2=success

    val progress = remember { Animatable(0f) }

    LaunchedEffect(isSending, isSuccess) {
        if (isSending && stageIndex == 0) {
            // Idle → Sending
            stageIndex = 1
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = if (reduceMotion) {
                    spring(stiffness = Spring.StiffnessHigh, dampingRatio = Spring.DampingRatioNoBouncy)
                } else {
                    spring(stiffness = 600f, dampingRatio = 0.7f)
                },
            )
        }
        if (isSuccess && stageIndex == 1) {
            // Sending → Success
            delay(300)
            stageIndex = 2
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = if (reduceMotion) {
                    spring(stiffness = Spring.StiffnessHigh, dampingRatio = Spring.DampingRatioNoBouncy)
                } else {
                    spring(stiffness = 300f, dampingRatio = 0.5f)
                },
            )
            // Success → Idle
            delay(600)
            stageIndex = 0
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = spring(stiffness = 400f, dampingRatio = 0.8f),
            )
            progress.snapTo(0f)
        }
        if (!isSending && !isSuccess && stageIndex != 0) {
            // Reset to idle
            stageIndex = 0
            progress.snapTo(0f)
        }
    }

    val activeMorph = when (stageIndex) {
        1 -> morphIdleToCircle
        2 -> morphCircleToSuccess
        else -> morphSuccessToIdle
    }
    val activeProgress = progress.value

    val fillColor = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    }

    val iconColor = if (enabled) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    }

    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(48.dp)
            .drawWithCache {
                val morphPath = Path()
                val androidPath = activeMorph.toPath(progress = activeProgress)
                val composePath = androidPath.asComposePath()

                // Transform from [-1, 1] to canvas coordinates
                val matrix = Matrix()
                matrix.scale(size.width / 2f, size.height / 2f)
                matrix.translate(1f, 1f)
                composePath.transform(matrix)

                onDrawBehind {
                    drawPath(composePath, color = fillColor, style = Fill)
                }
            },
    ) {
        // Icon is drawn as an overlay — the shape morph is the background.
        // In a production build this would be a vector icon drawn with
        // alpha animation tied to the checkmark frame.
    }
}
