package com.skeler.pulse.design.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ────────────────────────────────────────────────────────────
// Google Sans Flex — Variable Font Family
// ────────────────────────────────────────────────────────────
//
// The blueprint calls for Google Sans Flex loaded via
//   Font(R.font.google_sans_flex, variationSettings = ...)
// with weight-axis instances at 400, 700, and 900.
//
// Since the .ttf file must be manually provided at
//   core/design/src/main/res/font/google_sans_flex.ttf
// and is not currently present, we fall back to the system
// sans-serif family which provides comparable glyph coverage
// with proper weight support on all API levels.
//
// To enable Google Sans Flex:
// 1. Place the .ttf at the path above.
// 2. Replace `FontFamily.SansSerif` with:
//    val GoogleSansFlexFamily = FontFamily(
//        Font(R.font.google_sans_flex, FontWeight.Normal,
//             variationSettings = FontVariation.Settings(
//                 FontVariation.weight(400)
//             )),
//        Font(R.font.google_sans_flex, FontWeight.Bold,
//             variationSettings = FontVariation.Settings(
//                 FontVariation.weight(700)
//             )),
//        Font(R.font.google_sans_flex, FontWeight.Black,
//             variationSettings = FontVariation.Settings(
//                 FontVariation.weight(900)
//             )),
//    )
// ────────────────────────────────────────────────────────────

private val SerafinaFontFamily: FontFamily = FontFamily.SansSerif

/**
 * Full 15-slot Material 3 typography scale for the Serafina design system.
 *
 * Per the blueprint:
 * - NO `defaultFontFamily` — every slot has an explicit `fontFamily`.
 * - `titleMedium` and `headlineMedium` use FontWeight.Bold + letterSpacing 0.2.sp
 *   (emphasized variants).
 */
val SerafinaTypography = Typography(
    // ── Display ──
    displayLarge = TextStyle(
        fontFamily = SerafinaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = SerafinaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = SerafinaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp,
    ),

    // ── Headline ──
    headlineLarge = TextStyle(
        fontFamily = SerafinaFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = SerafinaFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.2.sp,        // ← emphasized per blueprint
    ),
    headlineSmall = TextStyle(
        fontFamily = SerafinaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    ),

    // ── Title ──
    titleLarge = TextStyle(
        fontFamily = SerafinaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = SerafinaFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.2.sp,        // ← emphasized per blueprint
    ),
    titleSmall = TextStyle(
        fontFamily = SerafinaFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),

    // ── Body ──
    bodyLarge = TextStyle(
        fontFamily = SerafinaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = SerafinaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = SerafinaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),

    // ── Label ──
    labelLarge = TextStyle(
        fontFamily = SerafinaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = SerafinaFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = SerafinaFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)

// ── Legacy alias ──
@Deprecated(
    message = "Use SerafinaTypography instead.",
    replaceWith = ReplaceWith("SerafinaTypography"),
)
val PulseTypography = SerafinaTypography
