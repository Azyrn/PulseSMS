package com.skeler.pulse.security.auth

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Result of a biometric authentication attempt.
 */
sealed class BiometricAuthResult {
    data object Success : BiometricAuthResult()
    data object Cancelled : BiometricAuthResult()
    data object Failed : BiometricAuthResult()
    data class Error(val message: String) : BiometricAuthResult()
}

/**
 * Checks whether biometric authentication is available on this device
 * and whether the user has enrolled credentials.
 */
fun checkBiometricAvailability(context: Context): BiometricAvailability {
    val biometricManager = BiometricManager.from(context)
    val result = biometricManager.canAuthenticate(BIOMETRIC_PROMPT_AUTHENTICATORS)
    return when (result) {
        BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.Available
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NoHardware
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAvailability.HardwareUnavailable
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NoneEnrolled
        BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricAvailability.SecurityUpdateRequired
        else -> BiometricAvailability.HardwareUnavailable
    }
}

/**
 * Shows a biometric authentication prompt using the system dialog.
 *
 * @param activity The fragment activity used as the prompt host.
 * @param title The title shown in the prompt.
 * @param subtitle The subtitle shown in the prompt.
 * @param onResult Called on the main thread with the authentication result.
 */
fun showBiometricPrompt(
    activity: FragmentActivity,
    title: String,
    subtitle: String,
    onResult: (BiometricAuthResult) -> Unit,
) {
    val availability = checkBiometricAvailability(activity)
    if (availability != BiometricAvailability.Available) {
        onResult(BiometricAuthResult.Error(availability.errorMessage()))
        return
    }

    val cryptoObject = try {
        BiometricPrompt.CryptoObject(createBiometricGateCipher())
    } catch (_: KeyPermanentlyInvalidatedException) {
        deleteBiometricGateKey()
        onResult(
            BiometricAuthResult.Error(
                "Biometric enrollment changed. Disable and re-enable biometric login.",
            ),
        )
        return
    } catch (_: Exception) {
        onResult(BiometricAuthResult.Error("Biometric authentication is unavailable."))
        return
    }

    val executor = ContextCompat.getMainExecutor(activity)

    val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            val cipher = result.cryptoObject?.cipher
            if (cipher == null) {
                onResult(BiometricAuthResult.Error("Biometric verification failed."))
                return
            }
            try {
                cipher.doFinal(BIOMETRIC_GATE_CHALLENGE)
                onResult(BiometricAuthResult.Success)
            } catch (_: Exception) {
                onResult(BiometricAuthResult.Error("Biometric verification failed."))
            }
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            when (errorCode) {
                BiometricPrompt.ERROR_USER_CANCELED,
                BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                BiometricPrompt.ERROR_CANCELED,
                -> onResult(BiometricAuthResult.Cancelled)
                BiometricPrompt.ERROR_LOCKOUT -> onResult(
                    BiometricAuthResult.Error("Too many attempts. Try biometric login again later."),
                )
                BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> onResult(
                    BiometricAuthResult.Error("Biometric login is locked. Unlock the device before trying again."),
                )
                BiometricPrompt.ERROR_HW_NOT_PRESENT,
                BiometricPrompt.ERROR_HW_UNAVAILABLE,
                BiometricPrompt.ERROR_NO_BIOMETRICS,
                BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL,
                BiometricPrompt.ERROR_NO_SPACE,
                BiometricPrompt.ERROR_SECURITY_UPDATE_REQUIRED,
                BiometricPrompt.ERROR_TIMEOUT,
                BiometricPrompt.ERROR_UNABLE_TO_PROCESS,
                BiometricPrompt.ERROR_VENDOR,
                -> onResult(BiometricAuthResult.Error(errString.toString()))
                else -> onResult(BiometricAuthResult.Error(errString.toString()))
            }
        }

        override fun onAuthenticationFailed() {
            onResult(BiometricAuthResult.Failed)
        }
    }

    val biometricPrompt = BiometricPrompt(activity, executor, callback)

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setNegativeButtonText("Cancel")
        .setAllowedAuthenticators(BIOMETRIC_PROMPT_AUTHENTICATORS)
        .build()

    try {
        biometricPrompt.authenticate(promptInfo, cryptoObject)
    } catch (_: IllegalArgumentException) {
        onResult(BiometricAuthResult.Error("Biometric prompt could not be started."))
    } catch (_: IllegalStateException) {
        onResult(BiometricAuthResult.Error("Biometric prompt could not be started."))
    }
}

/**
 * Availability state of biometric authentication on this device.
 */
enum class BiometricAvailability {
    /** Biometric hardware present and credentials enrolled. Can authenticate. */
    Available,
    /** No biometric hardware on this device. */
    NoHardware,
    /** Biometric hardware currently unavailable. */
    HardwareUnavailable,
    /** No biometric or device credentials enrolled — prompt user to set up. */
    NoneEnrolled,
    /** Security update is required before biometrics can be used. */
    SecurityUpdateRequired,
}

private fun BiometricAvailability.errorMessage(): String = when (this) {
    BiometricAvailability.Available -> ""
    BiometricAvailability.NoHardware -> "This device has no strong biometric hardware."
    BiometricAvailability.HardwareUnavailable -> "Strong biometric hardware is currently unavailable."
    BiometricAvailability.NoneEnrolled -> "Enroll a strong biometric before enabling biometric login."
    BiometricAvailability.SecurityUpdateRequired ->
        "A biometric sensor security update is required before biometric login can be used."
}

private fun createBiometricGateCipher(): Cipher {
    val cipher = Cipher.getInstance(BIOMETRIC_GATE_TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, getOrCreateBiometricGateKey())
    return cipher
}

@Suppress("DEPRECATION")
private fun getOrCreateBiometricGateKey(): SecretKey {
    val keyStore = androidKeyStore()
    (keyStore.getKey(BIOMETRIC_GATE_KEY_ALIAS, null) as? SecretKey)?.let { return it }

    val keyGenerator = KeyGenerator.getInstance(
        KeyProperties.KEY_ALGORITHM_AES,
        ANDROID_KEY_STORE,
    )
    val specBuilder = KeyGenParameterSpec.Builder(
        BIOMETRIC_GATE_KEY_ALIAS,
        KeyProperties.PURPOSE_ENCRYPT,
    )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setKeySize(BIOMETRIC_GATE_KEY_SIZE_BITS)
        .setUserAuthenticationRequired(true)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        specBuilder.setUserAuthenticationParameters(0, KEYSTORE_AUTHENTICATORS)
    } else {
        specBuilder.setUserAuthenticationValidityDurationSeconds(-1)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        specBuilder.setInvalidatedByBiometricEnrollment(true)
    }

    keyGenerator.init(specBuilder.build())
    return keyGenerator.generateKey()
}

private fun deleteBiometricGateKey() {
    runCatching {
        androidKeyStore().deleteEntry(BIOMETRIC_GATE_KEY_ALIAS)
    }
}

private fun androidKeyStore(): KeyStore =
    KeyStore.getInstance(ANDROID_KEY_STORE).also { it.load(null) }

private const val ANDROID_KEY_STORE = "AndroidKeyStore"
private const val BIOMETRIC_GATE_KEY_ALIAS = "pulse_biometric_gate_v2"
private const val BIOMETRIC_GATE_KEY_SIZE_BITS = 256
private const val BIOMETRIC_GATE_TRANSFORMATION = "AES/GCM/NoPadding"
private val BIOMETRIC_GATE_CHALLENGE = "pulse-biometric-gate".toByteArray(Charsets.UTF_8)
private const val BIOMETRIC_PROMPT_AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_STRONG
private const val KEYSTORE_AUTHENTICATORS = KeyProperties.AUTH_BIOMETRIC_STRONG
