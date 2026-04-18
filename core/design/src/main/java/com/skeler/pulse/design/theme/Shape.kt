package com.skeler.pulse.design.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Serafina shape token scale.
 *
 * Maps to Material 3 shape slots:
 * - extraSmall (4dp) — small chips, badges
 * - small (8dp) — text fields, small cards
 * - medium (12dp) — standard cards, dialogs
 * - large (20dp / "largeIncreased") — inbox cards, sheets
 * - extraLarge (32dp / "extraLargeIncreased") — bottom sheets, modals
 */
val SerafinaShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** Full-circle shape for avatars (50% corner radius). */
val SerafinaFullShape = RoundedCornerShape(percent = 50)

// ── Legacy alias ──
@Deprecated(
    message = "Use SerafinaShapes instead.",
    replaceWith = ReplaceWith("SerafinaShapes"),
)
val PulseShapes = SerafinaShapes
