package com.skeler.pulse.security.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.util.Log
import com.skeler.pulse.contracts.security.KeyManagementState
import com.skeler.pulse.contracts.security.KeyStoreCapability
import com.skeler.pulse.security.api.KeyMaterialStore
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory

internal fun resolveKeyManagementState(
    aliasExists: Boolean,
    key: SecretKey?,
): KeyManagementState = when {
    !aliasExists -> KeyManagementState.Ready
    key != null -> KeyManagementState.Ready
    else -> KeyManagementState.Corrupted
}

class AndroidKeyMaterialStore(
    private val context: Context,
) : KeyMaterialStore {

    override fun getCapability(): KeyStoreCapability = try {
        androidKeyStore()
        if (hasStrongBoxFeature()) {
            KeyStoreCapability.Available(hardwareBacked = true)
        } else {
            KeyStoreCapability.SoftwareOnly
        }
    } catch (_: Exception) {
        KeyStoreCapability.Unavailable
    }

    override fun getKeyManagementState(alias: String): KeyManagementState = try {
        val keyStore = androidKeyStore()
        val aliasExists = keyStore.containsAlias(alias)
        val key = if (aliasExists) {
            keyStore.getKey(alias, null) as? SecretKey
        } else {
            null
        }
        resolveKeyManagementState(aliasExists, key)
    } catch (_: Exception) {
        KeyManagementState.Unrecoverable
    }

    override fun getOrCreateKey(alias: String): SecretKey {
        val keyStore = androidKeyStore()
        val existing = keyStore.getKey(alias, null) as? SecretKey
        if (existing != null) {
            return existing
        }

        val shouldPreferStrongBox = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && hasStrongBoxFeature()
        return try {
            generateKey(alias = alias, shouldUseStrongBox = shouldPreferStrongBox)
        } catch (exception: Exception) {
            if (!shouldPreferStrongBox) {
                throw exception
            }
            Log.w(TAG, "StrongBox key creation failed for '$alias'; retrying with Android Keystore.", exception)
            generateKey(alias = alias, shouldUseStrongBox = false)
        }
    }

    private fun generateKey(alias: String, shouldUseStrongBox: Boolean): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        val keySpecBuilder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && shouldUseStrongBox) {
            keySpecBuilder.setIsStrongBoxBacked(true)
        }
        keyGenerator.init(keySpecBuilder.build())
        val key = keyGenerator.generateKey()
        verifyHardwareBacking(alias, key)
        return key
    }

    private fun hasStrongBoxFeature(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return false
        }
        return try {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
        } catch (_: Exception) {
            false
        }
    }

    private fun verifyHardwareBacking(alias: String, key: SecretKey) {
        try {
            val factory = SecretKeyFactory.getInstance(key.algorithm, ANDROID_KEY_STORE)
            val keyInfo = factory.getKeySpec(key, KeyInfo::class.java) as KeyInfo
            if (!keyInfo.isInsideSecureHardware) {
                Log.w(
                    TAG,
                    "Key '$alias' is not hardware-backed; falling back to software keystore.",
                )
            }
        } catch (e: Exception) {
            Log.w(
                TAG,
                "Could not verify hardware backing for '$alias'",
                e,
            )
        }
    }

    private fun androidKeyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE).also { it.load(null) }

    private companion object {
        const val TAG = "AndroidKeyMaterialStore"
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_SIZE_BITS = 256
    }
}
