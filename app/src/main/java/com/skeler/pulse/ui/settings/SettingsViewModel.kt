package com.skeler.pulse.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skeler.pulse.contracts.protocol.ProtocolMode
import com.skeler.pulse.contracts.security.KeyStoreCapability
import com.skeler.pulse.security.api.BusinessComplianceProvider
import com.skeler.pulse.security.api.BusinessComplianceStatus
import com.skeler.pulse.security.api.KeyExchangeProtocol
import com.skeler.pulse.security.api.KeyMaterialStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Aggregated settings state from real codebase features.
 *
 * All fields are populated from live SecurityComponent, ComplianceProvider,
 * and BuildConfig values — no demo / placeholder data.
 */
data class SettingsState(
    // Security
    val protocolMode: ProtocolMode = ProtocolMode.PQXDH,
    val keyStoreCapability: KeyStoreCapability = KeyStoreCapability.Unavailable,
    val isHardwareBacked: Boolean = false,

    // Compliance
    val complianceStatus: BusinessComplianceStatus = BusinessComplianceStatus(),
    val complianceLoaded: Boolean = false,

    // Sync / Build
    val syncEnvironment: String = "dev",
    val versionName: String = "1.0",
)

class SettingsViewModel(
    private val keyMaterialStore: KeyMaterialStore,
    private val protocols: List<KeyExchangeProtocol>,
    private val businessComplianceProvider: BusinessComplianceProvider,
    private val syncEnvironment: String,
    private val versionName: String,
    private val defaultConversationId: String,
) : ViewModel() {

    private val mutableState = MutableStateFlow(
        SettingsState(
            syncEnvironment = syncEnvironment,
            versionName = versionName,
        )
    )
    val state: StateFlow<SettingsState> = mutableState.asStateFlow()

    init {
        loadSecurityInfo()
        loadComplianceInfo()
    }

    /**
     * Query the KeyMaterialStore and protocol list to determine live security status.
     */
    private fun loadSecurityInfo() {
        viewModelScope.launch {
            val capability = keyMaterialStore.getCapability()
            val isHardware = capability is KeyStoreCapability.Available && capability.hardwareBacked
            val preferredMode = protocols.firstOrNull()?.mode ?: ProtocolMode.X3DH

            mutableState.update {
                it.copy(
                    keyStoreCapability = capability,
                    isHardwareBacked = isHardware,
                    protocolMode = preferredMode,
                )
            }
        }
    }

    /**
     * Fetch live compliance status for the default conversation.
     */
    private fun loadComplianceInfo() {
        viewModelScope.launch {
            try {
                val status = businessComplianceProvider.currentStatus(defaultConversationId)
                mutableState.update {
                    it.copy(
                        complianceStatus = status,
                        complianceLoaded = true,
                    )
                }
            } catch (_: Exception) {
                // Compliance data unavailable — leave defaults
                mutableState.update { it.copy(complianceLoaded = true) }
            }
        }
    }

    /**
     * Refresh compliance data (can be called from pull-to-refresh or similar).
     */
    fun refreshCompliance() {
        loadComplianceInfo()
    }
}
