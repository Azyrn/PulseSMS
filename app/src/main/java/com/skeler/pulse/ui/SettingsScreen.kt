package com.skeler.pulse.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.skeler.pulse.design.theme.SerafinaPalette
import com.skeler.pulse.design.theme.SerafinaThemeMode
import com.skeler.pulse.design.theme.SerafinaThemeViewModel
import com.skeler.pulse.design.util.elasticOverscroll
import com.skeler.pulse.design.util.rememberMomentumFlingBehavior
import com.skeler.pulse.design.util.rememberReducedMotionEnabled
import com.skeler.pulse.security.auth.BiometricAuthResult
import com.skeler.pulse.security.auth.BiometricAvailability
import com.skeler.pulse.security.auth.checkBiometricAvailability
import com.skeler.pulse.security.auth.showBiometricPrompt
import com.skeler.pulse.sms.MessageAutomationPreferences
import kotlinx.coroutines.launch

internal data class SettingsChoiceOption(
    val id: String,
    val label: String,
    val accentColor: Color? = null,
)

// ═══════════════════════════════════════════════════════════
// SETTINGS SCREEN — Real features wired from codebase
// ═══════════════════════════════════════════════════════════

@Composable
internal fun SettingsScreen(
    themeViewModel: SerafinaThemeViewModel,
    listState: LazyListState,
    archivedCount: Int,
    blockedCount: Int,
    onBack: () -> Unit,
    onRequestDefaultSms: () -> Unit,
    onOpenArchivedChats: () -> Unit,
    onOpenSecurity: () -> Unit,
    onOpenBlockedNumbers: () -> Unit,
    isDefaultSmsApp: Boolean,
) {
    val context = LocalContext.current
    val themeState by themeViewModel.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val messageAutomationPreferences = remember(context) {
        MessageAutomationPreferences(context.applicationContext)
    }
    val autoCopyOtpCodes by messageAutomationPreferences.autoCopyOtpCodes.collectAsState(initial = false)
    val reducedMotion = rememberReducedMotionEnabled()
    val settingsFlingBehavior = rememberMomentumFlingBehavior(enabled = !reducedMotion)
    val appearanceOptionState = rememberLazyListState()
    val appearanceOptionFlingBehavior = rememberMomentumFlingBehavior(enabled = !reducedMotion)
    val colorSchemeOptions = remember {
        buildList {
            add(SettingsChoiceOption(id = "dynamic", label = "Dynamic"))
            addAll(
                SerafinaPalette.entries.map { palette ->
                    SettingsChoiceOption(
                        id = palette.name,
                        label = palette.label,
                        accentColor = palette.seedColor,
                    )
                },
            )
        }
    }
    val themeOptions = remember {
        SerafinaThemeMode.entries.map { mode ->
            SettingsChoiceOption(id = mode.name, label = mode.label)
        }
    }
    val selectedColorSchemeId = if (themeState.dynamicColorEnabled) {
        "dynamic"
    } else {
        themeState.selectedPalette.name
    }
    val colorSchemeLabel = if (themeState.dynamicColorEnabled) {
        "Dynamic"
    } else {
        themeState.selectedPalette.label
    }

    Scaffold(
        topBar = {
            SettingsTopBar(onBack = onBack)
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            flingBehavior = settingsFlingBehavior,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .elasticOverscroll(
                    enabled = !reducedMotion,
                    state = listState,
                ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item(key = "general_header") { SettingsSectionHeader("General") }
            item(key = "general_card") {
                SettingsGroupCard {
                    SettingsRow(
                        icon = if (isDefaultSmsApp) Icons.Rounded.CheckCircle else Icons.Outlined.Sms,
                        title = if (isDefaultSmsApp) "Default SMS app" else "Set as default",
                        subtitle = if (isDefaultSmsApp) "Pulse is your default SMS app" else "Tap to set Pulse as default",
                        onClick = onRequestDefaultSms,
                    )
                    SettingsGroupDivider()
                    SettingsRow(
                        icon = Icons.Rounded.Archive,
                        title = "Archived chats",
                        subtitle = if (archivedCount == 0) "No archived chats" else "$archivedCount archived chats",
                        onClick = onOpenArchivedChats,
                    )
                    SettingsGroupDivider()
                    SettingsRow(
                        icon = Icons.Rounded.Fingerprint,
                        title = "Security & Biometric",
                        subtitle = "Fingerprint, password",
                        onClick = onOpenSecurity,
                    )
                    SettingsGroupDivider()
                    SettingsRow(
                        icon = Icons.Rounded.Block,
                        title = "Blocked numbers",
                        subtitle = if (blockedCount == 0) "No blocked senders" else "$blockedCount blocked senders",
                        onClick = onOpenBlockedNumbers,
                    )
                    SettingsGroupDivider()
                    SettingsToggleRow(
                        icon = Icons.Rounded.ContentCopy,
                        title = "Auto-copy codes",
                        subtitle = "Copy OTP and business verification codes from incoming SMS when detected.",
                        checked = autoCopyOtpCodes,
                        onToggle = {
                            coroutineScope.launch {
                                messageAutomationPreferences.setAutoCopyOtpCodesEnabled(!autoCopyOtpCodes)
                            }
                        },
                    )
                    SettingsGroupDivider()
                    SettingsChoiceRow(
                        icon = Icons.Outlined.Palette,
                        title = "Color scheme",
                        subtitle = colorSchemeLabel,
                    ) {
                        SettingsChoiceRail(
                            options = colorSchemeOptions,
                            selectedId = selectedColorSchemeId,
                            listState = appearanceOptionState,
                            flingBehavior = appearanceOptionFlingBehavior,
                            reducedMotion = reducedMotion,
                            onSelect = { optionId ->
                                if (optionId == "dynamic") {
                                    if (!themeState.dynamicColorEnabled) {
                                        themeViewModel.toggleDynamicColor()
                                    }
                                } else {
                                    if (themeState.dynamicColorEnabled) {
                                        themeViewModel.toggleDynamicColor()
                                    }
                                    SerafinaPalette.entries.firstOrNull { it.name == optionId }?.let(themeViewModel::selectPalette)
                                }
                            },
                        )
                    }
                    SettingsGroupDivider()
                    SettingsChoiceRow(
                        icon = Icons.Outlined.Contrast,
                        title = "Theme",
                        subtitle = themeState.themeMode.label,
                    ) {
                        SettingsChoiceRail(
                            options = themeOptions,
                            selectedId = themeState.themeMode.name,
                            reducedMotion = reducedMotion,
                            onSelect = { optionId ->
                                SerafinaThemeMode.entries.firstOrNull { it.name == optionId }?.let(themeViewModel::selectThemeMode)
                            },
                        )
                    }
                    SettingsGroupDivider()
                    SettingsToggleRow(
                        icon = Icons.Outlined.DarkMode,
                        title = "Black theme only",
                        subtitle = "Use pure black surfaces whenever the app is in dark mode.",
                        checked = themeState.blackThemeEnabled,
                        onToggle = { themeViewModel.setBlackThemeEnabled(!themeState.blackThemeEnabled) },
                    )
                }
            }
            item(key = "bottom_spacer") { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ── Settings sub-components ──
