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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
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
import com.skeler.pulse.sms.MessageAutomationPreferences
import kotlinx.coroutines.launch


@Composable
internal fun SecuritySettingsScreen(
    themeViewModel: SerafinaThemeViewModel,
    onBack: () -> Unit,
) {
    val themeState by themeViewModel.state.collectAsState()
    val context = LocalContext.current
    val reducedMotion = rememberReducedMotionEnabled()
    val securityListState = rememberLazyListState()
    val securityFlingBehavior = rememberMomentumFlingBehavior(enabled = !reducedMotion)
    var biometricToggleError by rememberSaveable { mutableStateOf<String?>(null) }
    fun requestFingerprintToggle() {
        if (themeState.fingerprintEnabled) {
            biometricToggleError = null
            themeViewModel.setFingerprintEnabled(false)
            return
        }
        val availability = checkBiometricAvailability(context)
        if (availability != BiometricAvailability.Available) {
            biometricToggleError = availability.lockScreenMessage(context.resources)
            return
        }
        val activity = context.findFragmentActivity()
        if (activity == null) {
            biometricToggleError = context.getString(R.string.security_biometric_prompt_error)
            return
        }
        biometricToggleError = null
        showBiometricPrompt(
            activity = activity,
            title = context.getString(R.string.security_biometric_prompt_title),
            subtitle = context.getString(R.string.security_biometric_prompt_subtitle),
        ) { result ->
            when (result) {
                is BiometricAuthResult.Success -> {
                    biometricToggleError = null
                    themeViewModel.setFingerprintEnabled(true)
                }
                is BiometricAuthResult.Cancelled -> Unit
                is BiometricAuthResult.Failed -> {
                    biometricToggleError = context.getString(R.string.security_fingerprint_auth_failed)
                }
                is BiometricAuthResult.Error -> {
                    biometricToggleError = result.message
                }
            }
        }
    }

    Scaffold(
        topBar = { SettingsTopBar(onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        LazyColumn(
            state = securityListState,
            flingBehavior = securityFlingBehavior,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .elasticOverscroll(
                    enabled = !reducedMotion,
                    state = securityListState,
                ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item(key = "security_header") {
                SettingsSectionHeader(stringResource(R.string.security_header_title))
            }
            item(key = "security_card") {
                SettingsGroupCard {
                    SecurityFingerprintRow(
                        enabled = themeState.fingerprintEnabled,
                        error = biometricToggleError,
                        onToggle = ::requestFingerprintToggle,
                    )
                    SettingsGroupDivider()
                    SecurityPasswordSection(
                        passwordSet = themeState.password.isNotEmpty(),
                        onSetPassword = { themeViewModel.setPassword(it) },
                        onClearPassword = { themeViewModel.clearPassword() },
                    )
                }
            }
            item(key = "security_bottom_spacer") { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
internal fun SecurityFingerprintRow(
    enabled: Boolean,
    error: String?,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.security_fingerprint_title), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = error ?: if (enabled) stringResource(R.string.security_fingerprint_active) else stringResource(R.string.security_fingerprint_inactive),
                style = MaterialTheme.typography.bodySmall,
                color = if (error == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
        }
        Switch(checked = enabled, onCheckedChange = { onToggle() })
    }
}

@Composable
internal fun SecurityPasswordSection(
    passwordSet: Boolean,
    onSetPassword: (String) -> Unit,
    onClearPassword: () -> Unit,
) {
    var passwordInput by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var isEditing by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.security_password_title), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = if (passwordSet) stringResource(R.string.security_password_set) else stringResource(R.string.security_password_not_set),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (isEditing || !passwordSet) {
            OutlinedTextField(
                value = passwordInput,
                onValueChange = { newValue -> passwordInput = newValue.filter { it.isLetterOrDigit() } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.security_password_enter)) },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (passwordInput.isNotBlank()) { onSetPassword(passwordInput); passwordInput = ""; isEditing = false }
                }),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = if (passwordVisible) stringResource(R.string.security_password_hide) else stringResource(R.string.security_password_show),
                        )
                    }
                },
                shape = RoundedCornerShape(14.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(
                    onClick = { if (passwordInput.isNotBlank()) { onSetPassword(passwordInput); passwordInput = ""; isEditing = false } },
                    enabled = passwordInput.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                ) { Text(stringResource(R.string.action_save)) }
                if (passwordSet && isEditing) {
                    FilledTonalButton(
                        onClick = { passwordInput = ""; isEditing = false },
                        shape = RoundedCornerShape(12.dp),
                    ) { Text(stringResource(R.string.action_cancel)) }
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = { isEditing = true }, shape = RoundedCornerShape(12.dp)) { Text(stringResource(R.string.action_change)) }
                FilledTonalButton(onClick = { onClearPassword(); passwordInput = "" }, shape = RoundedCornerShape(12.dp)) { Text(stringResource(R.string.action_remove)) }
            }
        }
    }
}
