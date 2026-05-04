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
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.rememberDynamicColorScheme

enum class SerafinaThemeMode(val label: String) {
    System("System default"),
    Light("Light"),
    Dark("Dark"),
}

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
 * Wraps content in [MaterialExpressiveTheme] with a calm [MotionScheme.standard].
 */
@Composable
fun SerafinaAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeState: SerafinaThemeState = SerafinaThemeState(),
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val resolvedDarkTheme = when (themeState.themeMode) {
        SerafinaThemeMode.System -> darkTheme
        SerafinaThemeMode.Light -> false
        SerafinaThemeMode.Dark -> true
    }

    val colorScheme = resolveColorScheme(
        darkTheme = resolvedDarkTheme,
        dynamicColorEnabled = themeState.dynamicColorEnabled,
        palette = themeState.selectedPalette,
        pureBlackEnabled = themeState.blackThemeEnabled,
    )

    val motionScheme = MotionScheme.standard()

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
    pureBlackEnabled: Boolean,
): ColorScheme {
    val context = LocalContext.current
    val baseScheme = when {
        dynamicColorEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            remember(context, darkTheme) {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
        !dynamicColorEnabled ->
            rememberDynamicColorScheme(
                seedColor = palette.seedColor,
                isDark = darkTheme,
            )
        else ->
            remember(darkTheme) {
                if (darkTheme) SerafinaDarkColorScheme else SerafinaLightColorScheme
            }
    }

    if (!darkTheme || !pureBlackEnabled) {
        return baseScheme
    }

    return remember(baseScheme) {
        baseScheme.copy(
            background = PureBlackBackground,
            surface = PureBlackBackground,
            surfaceDim = PureBlackBackground,
            surfaceBright = PureBlackContainerHigh,
            surfaceContainerLowest = PureBlackBackground,
            surfaceContainerLow = PureBlackContainerLow,
            surfaceContainer = PureBlackContainer,
            surfaceContainerHigh = PureBlackContainerHigh,
            surfaceContainerHighest = PureBlackContainerHighest,
        )
    }
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
