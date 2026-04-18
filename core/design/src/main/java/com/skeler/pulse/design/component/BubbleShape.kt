package com.skeler.pulse.design.component

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Custom message-bubble [Shape] with a tail on one corner.
 *
 * - **User (outbound):** tail on topRight (4dp), other corners 32dp.
 *   Color recommendation: `surfaceBright`.
 * - **Other (inbound):** tail on topLeft (4dp), other corners 32dp.
 *   Color recommendation: `surfaceContainer`.
 *
 * The "tail" is simply a reduced corner radius that creates an
 * asymmetric feel — matching messaging app conventions where the
 * pointed corner indicates bubble origin direction.
 */
class BubbleShape(private val isUser: Boolean) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val tailRadius = with(density) { 4.dp.toPx() }
        val mainRadius = with(density) { 32.dp.toPx() }

        val roundRect = if (isUser) {
            // User bubble: tail on top-right
            RoundRect(
                left = 0f,
                top = 0f,
                right = size.width,
                bottom = size.height,
                topLeftCornerRadius = CornerRadius(mainRadius),
                topRightCornerRadius = CornerRadius(tailRadius),
                bottomRightCornerRadius = CornerRadius(mainRadius),
                bottomLeftCornerRadius = CornerRadius(mainRadius),
            )
        } else {
            // Other bubble: tail on top-left
            RoundRect(
                left = 0f,
                top = 0f,
                right = size.width,
                bottom = size.height,
                topLeftCornerRadius = CornerRadius(tailRadius),
                topRightCornerRadius = CornerRadius(mainRadius),
                bottomRightCornerRadius = CornerRadius(mainRadius),
                bottomLeftCornerRadius = CornerRadius(mainRadius),
            )
        }

        val path = Path().apply { addRoundRect(roundRect) }
        return Outline.Generic(path)
    }
}
