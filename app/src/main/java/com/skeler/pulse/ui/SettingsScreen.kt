package com.skeler.pulse.ui

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.skeler.pulse.R
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
import com.skeler.pulse.sms.EncryptionPreferences
import com.skeler.pulse.sms.MessageAutomationPreferences
import com.skeler.pulse.sms.MmsPreferences
import com.skeler.pulse.sms.NotificationPreferences
import com.skeler.pulse.sms.QuickComposeNotificationManager
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
    onExportBackup: (Uri) -> Unit = {},
    onImportBackup: (Uri) -> Unit = {},
    isDefaultSmsApp: Boolean,
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val themeState by themeViewModel.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val messageAutomationPreferences = remember(context) {
        MessageAutomationPreferences(context.applicationContext)
    }
    val autoCopyOtpCodes by messageAutomationPreferences.autoCopyOtpCodes.collectAsState(initial = false)
    val notificationPreferences = remember(context) {
        NotificationPreferences(context.applicationContext)
    }
    val quickReplyEnabled by notificationPreferences.quickReplyEnabled.collectAsState(initial = true)
    val quickComposeEnabled by notificationPreferences.quickComposeEnabled.collectAsState(initial = false)
    val reducedMotion = rememberReducedMotionEnabled()
    val settingsFlingBehavior = rememberMomentumFlingBehavior(enabled = !reducedMotion)
    val appearanceOptionState = rememberLazyListState()
    val appearanceOptionFlingBehavior = rememberMomentumFlingBehavior(enabled = !reducedMotion)
    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/xml")
    ) { uri ->
        if (uri != null) onExportBackup(uri)
    }
    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onImportBackup(uri)
    }

    val colorSchemeOptions = remember {
        buildList {
            add(SettingsChoiceOption(id = "dynamic", label = context.getString(R.string.settings_dynamic)))
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
            val label = when (mode) {
                SerafinaThemeMode.System -> context.getString(R.string.theme_mode_system)
                SerafinaThemeMode.Light -> context.getString(R.string.theme_mode_light)
                SerafinaThemeMode.Dark -> context.getString(R.string.theme_mode_dark)
            }
            SettingsChoiceOption(id = mode.name, label = label)
        }
    }
    val selectedColorSchemeId = if (themeState.dynamicColorEnabled) {
        "dynamic"
    } else {
        themeState.selectedPalette.name
    }
    val colorSchemeLabel = if (themeState.dynamicColorEnabled) {
        stringResource(R.string.settings_dynamic)
    } else {
        themeState.selectedPalette.label
    }
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SettingsTopBar(onBack = onBack, title = stringResource(R.string.settings_title))
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
            item(key = "general_header") { SettingsSectionHeader(stringResource(R.string.settings_general)) }
            item(key = "general_card") {
                SettingsGroupCard {
                    SettingsRow(
                        icon = if (isDefaultSmsApp) Icons.Rounded.CheckCircle else Icons.Outlined.Sms,
                        title = if (isDefaultSmsApp) stringResource(R.string.settings_default_sms_app) else stringResource(R.string.settings_set_as_default),
                        subtitle = if (isDefaultSmsApp) context.getString(R.string.settings_default_sms_subtitle) else context.getString(R.string.settings_tap_to_set_default),
                        onClick = onRequestDefaultSms,
                    )
                    SettingsGroupDivider()
                    SettingsRow(
                        icon = Icons.Rounded.Archive,
                        title = stringResource(R.string.settings_archived_chats),
                        subtitle = if (archivedCount == 0) context.getString(R.string.settings_no_archived_chats) else context.resources.getQuantityString(R.plurals.settings_archived_count, archivedCount, archivedCount),
                        onClick = onOpenArchivedChats,
                    )
                    SettingsGroupDivider()
                    SettingsRow(
                        icon = Icons.Rounded.Fingerprint,
                        title = stringResource(R.string.settings_security),
                        subtitle = context.getString(R.string.settings_security_subtitle),
                        onClick = onOpenSecurity,
                    )
                    SettingsGroupDivider()
                    SettingsRow(
                        icon = Icons.Rounded.Block,
                        title = stringResource(R.string.settings_blocked_numbers),
                        subtitle = if (blockedCount == 0) context.getString(R.string.settings_no_blocked_senders) else context.resources.getQuantityString(R.plurals.settings_blocked_count, blockedCount, blockedCount),
                        onClick = onOpenBlockedNumbers,
                    )
                    SettingsGroupDivider()
                    SettingsToggleRow(
                        icon = Icons.Rounded.ContentCopy,
                        title = stringResource(R.string.settings_auto_copy_codes),
                        subtitle = context.getString(R.string.settings_auto_copy_subtitle),
                        checked = autoCopyOtpCodes,
                        onToggle = {
                            coroutineScope.launch {
                                messageAutomationPreferences.setAutoCopyOtpCodesEnabled(!autoCopyOtpCodes)
                            }
                        },
                    )
                    SettingsGroupDivider()
                    val encryptionPreferences = remember(context) {
                        EncryptionPreferences(context.applicationContext)
                    }
                    val encryptionEnabled by encryptionPreferences.encryptionEnabled.collectAsState(initial = false)
                    var showEncryptionWarningDialog by remember { mutableStateOf(false) }
                    SettingsToggleRow(
                        icon = Icons.Rounded.Lock,
                        title = stringResource(R.string.settings_encryption),
                        subtitle = context.getString(R.string.settings_encryption_subtitle),
                        checked = encryptionEnabled,
                        onToggle = {
                            if (encryptionEnabled) {
                                coroutineScope.launch {
                                    encryptionPreferences.setEncryptionEnabled(false)
                                }
                            } else {
                                showEncryptionWarningDialog = true
                            }
                        },
                    )
                    if (showEncryptionWarningDialog) {
                        AlertDialog(
                            onDismissRequest = { showEncryptionWarningDialog = false },
                            title = { Text(stringResource(R.string.encryption_warning_title)) },
                            text = { Text(stringResource(R.string.encryption_warning_body)) },
                            dismissButton = {
                                TextButton(onClick = { showEncryptionWarningDialog = false }) {
                                    Text(stringResource(R.string.encryption_warning_cancel))
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    showEncryptionWarningDialog = false
                                    coroutineScope.launch {
                                        encryptionPreferences.setEncryptionEnabled(true)
                                    }
                                }) {
                                    Text(stringResource(R.string.encryption_warning_confirm))
                                }
                            },
                        )
                    }
                    SettingsGroupDivider()
                    val batteryOptDisabled = remember { mutableStateOf(checkBatteryOpt(context)) }
                    val batteryOptLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.StartActivityForResult()
                    ) {
                        batteryOptDisabled.value = checkBatteryOpt(context)
                    }
                    val lifecycle = LocalLifecycleOwner.current.lifecycle
                    DisposableEffect(lifecycle) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME) {
                                batteryOptDisabled.value = checkBatteryOpt(context)
                            }
                        }
                        lifecycle.addObserver(observer)
                        onDispose { lifecycle.removeObserver(observer) }
                    }
                    SettingsRow(
                        icon = Icons.Rounded.Bolt,
                        title = stringResource(R.string.settings_battery_optimization),
                        subtitle = if (batteryOptDisabled.value) {
                            context.getString(R.string.settings_battery_optimization_disabled)
                        } else {
                            context.getString(R.string.settings_battery_optimization_enabled)
                        },
                        onClick = {
                            val intent = android.content.Intent(
                                android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            ).apply {
                                data = android.net.Uri.parse("package:${context.packageName}")
                            }
                            batteryOptLauncher.launch(intent)
                        },
                    )
                    SettingsGroupDivider()
                    SettingsToggleRow(
                        icon = Icons.AutoMirrored.Filled.Reply,
                        title = stringResource(R.string.settings_quick_reply),
                        subtitle = context.getString(R.string.settings_quick_reply_subtitle),
                        checked = quickReplyEnabled,
                        onToggle = {
                            coroutineScope.launch {
                                notificationPreferences.setQuickReplyEnabled(!quickReplyEnabled)
                            }
                        },
                    )
                    SettingsGroupDivider()
                    SettingsToggleRow(
                        icon = Icons.AutoMirrored.Filled.Reply,
                        title = stringResource(R.string.settings_quick_compose),
                        subtitle = context.getString(R.string.settings_quick_compose_subtitle),
                        checked = quickComposeEnabled,
                        onToggle = {
                            coroutineScope.launch {
                                val newValue = !quickComposeEnabled
                                notificationPreferences.setQuickComposeEnabled(newValue)
                                if (newValue) {
                                    QuickComposeNotificationManager.show(context.applicationContext)
                                } else {
                                    QuickComposeNotificationManager.hide(context.applicationContext)
                                }
                            }
                        },
                    )
                    SettingsGroupDivider()
                    SettingsRow(
                        icon = Icons.Outlined.Language,
                        title = stringResource(R.string.settings_language),
                        subtitle = localeDisplayName(context, themeState.selectedLocale),
                        onClick = { showLanguageDialog = true },
                    )
                    SettingsGroupDivider()
                    SettingsChoiceRow(
                        icon = Icons.Outlined.Palette,
                        title = stringResource(R.string.settings_color_scheme),
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
                        title = stringResource(R.string.settings_theme),
                        subtitle = when (themeState.themeMode) {
                            SerafinaThemeMode.System -> context.getString(R.string.theme_mode_system)
                            SerafinaThemeMode.Light -> context.getString(R.string.theme_mode_light)
                            SerafinaThemeMode.Dark -> context.getString(R.string.theme_mode_dark)
                        },
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
                        title = stringResource(R.string.settings_black_theme),
                        subtitle = context.getString(R.string.settings_black_theme_subtitle),
                        checked = themeState.blackThemeEnabled,
                        onToggle = { themeViewModel.setBlackThemeEnabled(!themeState.blackThemeEnabled) },
                    )
                }
            }
            item(key = "data_header") { SettingsSectionHeader(stringResource(R.string.backup_and_restore)) }
            item(key = "data_card") {
                SettingsGroupCard {
                    SettingsRow(
                        icon = Icons.Rounded.Upload,
                        title = stringResource(R.string.settings_backup_export),
                        subtitle = context.getString(R.string.settings_backup_export_subtitle),
                        onClick = { exportBackupLauncher.launch("PulseSMS_backup_${System.currentTimeMillis()}.xml") },
                    )
                    SettingsGroupDivider()
                    SettingsRow(
                        icon = Icons.Rounded.Download,
                        title = stringResource(R.string.settings_backup_import),
                        subtitle = context.getString(R.string.settings_backup_import_subtitle),
                        onClick = { importBackupLauncher.launch(arrayOf("text/xml", "text/*")) },
                    )
                    SettingsGroupDivider()
                }
            }
            item(key = "mms_header") { SettingsSectionHeader(stringResource(R.string.settings_mms)) }
            item(key = "mms_card") {
                val mmsPreferences = remember(context) {
                    MmsPreferences(context.applicationContext)
                }
                val mmsImageSizeKb by mmsPreferences.maxImageSizeKb.collectAsState(initial = MmsPreferences.DEFAULT_MAX_IMAGE_SIZE_KB)
                val sizeOptions = remember {
                    listOf(150, 300, 500, 750, 1000, MmsPreferences.UNLIMITED)
                }
                SettingsGroupCard {
                    SettingsChoiceRow(
                        icon = Icons.Outlined.Sms,
                        title = stringResource(R.string.settings_mms_image_size),
                        subtitle = mmsSizeLabel(context, mmsImageSizeKb),
                    ) {
                        SettingsChoiceRail(
                            options = sizeOptions.map { value ->
                                SettingsChoiceOption(
                                    id = value.toString(),
                                    label = mmsSizeLabel(context, value),
                                )
                            },
                            selectedId = mmsImageSizeKb.toString(),
                            reducedMotion = reducedMotion,
                            onSelect = { optionId ->
                                coroutineScope.launch {
                                    mmsPreferences.setMaxImageSizeKb(optionId.toIntOrNull() ?: MmsPreferences.DEFAULT_MAX_IMAGE_SIZE_KB)
                                }
                            },
                        )
                    }
                }
            }
            item(key = "bottom_spacer") { Spacer(Modifier.height(32.dp)) }
        }
    }

    if (showLanguageDialog) {
        LanguagePickerDialog(
            currentLocale = themeState.selectedLocale,
            onDismiss = { showLanguageDialog = false },
            onSelect = { locale ->
                showLanguageDialog = false
                themeViewModel.setSelectedLocaleAndRecreate(locale)
                activity?.recreate()
            },
        )
    }

}

@Composable
private fun LanguagePickerDialog(
    currentLocale: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val systemLabel = stringResource(R.string.settings_language_system)
    val englishLabel = stringResource(R.string.settings_language_english)
    val frenchLabel = stringResource(R.string.settings_language_french)
    val options = remember(systemLabel, englishLabel, frenchLabel) {
        listOf(
            "system" to systemLabel,
            "en" to englishLabel,
            "fr" to frenchLabel,
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_language)) },
        text = {
            Column {
                options.forEach { (locale, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(locale) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = locale == currentLocale,
                            onClick = { onSelect(locale) },
                        )
                        Spacer(Modifier.size(12.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
    )
}

private fun localeDisplayName(context: android.content.Context, locale: String): String = when (locale) {
    "en" -> context.getString(R.string.settings_language_english_label)
    "fr" -> context.getString(R.string.settings_language_french_label)
    else -> context.getString(R.string.settings_language_system_label)
}

private fun mmsSizeLabel(context: android.content.Context, sizeKb: Int): String = when (sizeKb) {
    1000 -> context.getString(R.string.settings_mms_size_mb)
    -1 -> context.getString(R.string.settings_mms_size_unlimited)
    else -> context.getString(R.string.settings_mms_size_kb, sizeKb)
}

private fun checkBatteryOpt(context: android.content.Context): Boolean {
    val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

// ── Settings sub-components ──
