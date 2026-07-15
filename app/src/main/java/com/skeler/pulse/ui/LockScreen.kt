package com.skeler.pulse.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.skeler.pulse.design.theme.verifySecurityPassword
import com.skeler.pulse.security.auth.BiometricAuthResult
import com.skeler.pulse.security.auth.BiometricAvailability
import com.skeler.pulse.security.auth.showBiometricPrompt

// ═══════════════════════════════════════════════════════════
// LOCK SCREEN — Biometric authentication gate
// ═══════════════════════════════════════════════════════════

@Composable
internal fun LockScreen(
    biometricEnabled: Boolean,
    biometricAvailability: BiometricAvailability,
    passwordVerifierToken: String,
    onAuthenticated: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    var authError by rememberSaveable { mutableStateOf<String?>(null) }
    var passwordInput by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val passwordEnabled = passwordVerifierToken.isNotBlank()

    fun requestBiometricUnlock() {
        val activity = context.findFragmentActivity() ?: return
        authError = null
        if (biometricAvailability != BiometricAvailability.Available) {
            authError = biometricAvailability.lockScreenMessage()
            return
        }
        showBiometricPrompt(
            activity = activity,
            title = "Unlock Pulse",
            subtitle = "Authenticate to access your messages",
        ) { result ->
            when (result) {
                is BiometricAuthResult.Success -> onAuthenticated()
                is BiometricAuthResult.Cancelled -> onCancel()
                is BiometricAuthResult.Failed -> {
                    authError = "Authentication failed. Try again."
                }
                is BiometricAuthResult.Error -> {
                    authError = result.message
                }
            }
        }
    }

    fun requestPasswordUnlock() {
        if (!passwordEnabled || passwordInput.isBlank()) return
        if (verifySecurityPassword(passwordInput, passwordVerifierToken)) {
            passwordInput = ""
            authError = null
            onAuthenticated()
        } else {
            authError = "Incorrect password. Try again."
            passwordInput = ""
        }
    }

    var hasAutoPrompted by rememberSaveable { mutableStateOf(false) }

    // Show the biometric prompt once when the lock screen first appears
    LaunchedEffect(biometricEnabled, biometricAvailability) {
        if (!biometricEnabled || hasAutoPrompted) return@LaunchedEffect
        if (biometricAvailability != BiometricAvailability.Available) {
            authError = biometricAvailability.lockScreenMessage()
            return@LaunchedEffect
        }
        hasAutoPrompted = true
        requestBiometricUnlock()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Pulse is locked",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = authError ?: when {
                        biometricEnabled && passwordEnabled -> "Use fingerprint or password"
                        biometricEnabled -> "Use fingerprint to unlock"
                        passwordEnabled -> "Enter your password"
                        else -> "No unlock method is configured"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = authError?.let { MaterialTheme.colorScheme.error }
                        ?: MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (passwordEnabled) {
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { newValue -> passwordInput = newValue.filter { it.isLetterOrDigit() } },
                        modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth(),
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { requestPasswordUnlock() }),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                )
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                    )
                    Button(
                        onClick = { requestPasswordUnlock() },
                        enabled = passwordInput.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Rounded.Key, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Unlock")
                    }
                }
                if (biometricEnabled) {
                    FilledTonalButton(
                        onClick = { requestBiometricUnlock() },
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Rounded.Fingerprint, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Use fingerprint")
                    }
                }
            }
        }
    }
}
