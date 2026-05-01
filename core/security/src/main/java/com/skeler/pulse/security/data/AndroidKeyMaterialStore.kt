package com.skeler.pulse.security.data

import android.content.Context
import android.content.pm.PackageManager
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
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
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)) {
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

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .setIsStrongBoxBacked(requireStrongBoxBacking())
                .build()
        )
        val key = keyGenerator.generateKey()
        verifyHardwareBacking(alias, key)
        return key
    }

    private fun requireStrongBoxBacking(): Boolean {
        return try {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
        } catch (_: Exception) {
            false
        }
    }

    private fun verifyHardwareBacking(alias: String, key: SecretKey) {
        val factory = SecretKeyFactory.getInstance(key.algorithm, ANDROID_KEY_STORE)
        val keyInfo = factory.getKeySpec(key, KeyInfo::class.java) as KeyInfo
        require(keyInfo.isInsideSecureHardware) {
            "Key '$alias' was generated with StrongBox flag but is not hardware-backed. Hardware keystore may be compromised."
        }
    }

    private fun androidKeyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE).also { it.load(null) }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_SIZE_BITS = 256
    }
}
