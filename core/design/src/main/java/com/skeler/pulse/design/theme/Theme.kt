@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.skeler.pulse.design.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.rememberDynamicColorScheme

// ═══════════════════════════════════════════════════════════
// Accessibility — Reduced Motion
// ═══════════════════════════════════════════════════════════

/**
 * When `true`, all expressive springs are replaced with
 * `spring(stiffness = StiffnessHigh, dampingRatio = NoBouncy)`
 * and morph overshoot is disabled.
 *
 * Read via `LocalReduceMotion.current` inside any composable.
 */
val LocalReduceMotion = staticCompositionLocalOf { false }

// ═══════════════════════════════════════════════════════════
// SerafinaAppTheme
// ═══════════════════════════════════════════════════════════

/**
 * Root theme composable for the Pulse SMS app.
 *
 * Implements a three-tier color fallback:
 * 1. **Tier 1** (API 31+, `dynamicColorEnabled`): wallpaper-based dynamic color
 * 2. **Tier 2** (user-chosen palette): `rememberDynamicColorScheme` from seed
 * 3. **Tier 3**: static brand-fallback [SerafinaLightColorScheme] / [SerafinaDarkColorScheme]
 *
 * Wraps content in [MaterialExpressiveTheme] with [MotionScheme.expressive].
 */
@Composable
fun SerafinaAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeState: SerafinaThemeState = SerafinaThemeState(),
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = resolveColorScheme(
        darkTheme = darkTheme,
        dynamicColorEnabled = themeState.dynamicColorEnabled,
        palette = themeState.selectedPalette,
    )

    val motionScheme = if (reduceMotion) {
        MotionScheme.standard()
    } else {
        MotionScheme.expressive()
    }

    CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            typography = SerafinaTypography,
            shapes = SerafinaShapes,
            motionScheme = motionScheme,
            content = content,
        )
    }
}

// ── Color resolution ──

@Composable
private fun resolveColorScheme(
    darkTheme: Boolean,
    dynamicColorEnabled: Boolean,
    palette: SerafinaPalette,
): ColorScheme {
    val context = LocalContext.current

    // Tier 1: wallpaper-derived dynamic color on API 31+
    if (dynamicColorEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        return if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }

    // Tier 2: seed-based dynamic palette via MaterialKolor
    if (!dynamicColorEnabled) {
        return rememberDynamicColorScheme(
            seedColor = palette.seedColor,
            isDark = darkTheme,
        )
    }

    // Tier 3: static brand fallback (API < 31 with dynamicColor requested)
    return if (darkTheme) SerafinaDarkColorScheme else SerafinaLightColorScheme
}

// ── Legacy alias ──

/**
 * @deprecated Use [SerafinaAppTheme] instead.
 */
@Deprecated(
    message = "Use SerafinaAppTheme instead.",
    replaceWith = ReplaceWith("SerafinaAppTheme(content = content)"),
)
@Composable
fun PulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    SerafinaAppTheme(
        darkTheme = darkTheme,
        themeState = SerafinaThemeState(dynamicColorEnabled = dynamicColor),
        content = content,
    )
}
