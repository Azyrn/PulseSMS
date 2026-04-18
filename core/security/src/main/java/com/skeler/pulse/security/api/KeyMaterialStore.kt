package com.skeler.pulse.security.api

import com.skeler.pulse.contracts.security.KeyManagementState
import com.skeler.pulse.contracts.security.KeyStoreCapability
import javax.crypto.SecretKey

interface KeyMaterialStore {
    fun getCapability(): KeyStoreCapability

    fun getKeyManagementState(alias: String): KeyManagementState

    fun getOrCreateKey(alias: String): SecretKey
}
