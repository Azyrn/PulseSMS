package com.skeler.pulse.design.theme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Immutable snapshot of the current Serafina theme configuration.
 */
data class SerafinaThemeState(
    val dynamicColorEnabled: Boolean = true,
    val selectedPalette: SerafinaPalette = SerafinaPalette.LavenderVolt,
    val themeMode: SerafinaThemeMode = SerafinaThemeMode.System,
    val blackThemeEnabled: Boolean = false,
    val reduceMotion: Boolean = false,
    val fingerprintEnabled: Boolean = false,
    val password: String = "",
)

/**
 * ViewModel that exposes the user's theme preferences as a [StateFlow]
 * and provides mutation methods that persist changes to DataStore.
 */
class SerafinaThemeViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = ThemePreferences(application)
    private val initialState = runBlocking {
        runCatching { prefs.currentState() }.getOrDefault(SerafinaThemeState())
    }

    val state: StateFlow<SerafinaThemeState> = prefs.state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = initialState,
    )

    fun toggleDynamicColor() {
        viewModelScope.launch {
            prefs.setDynamicColorEnabled(!state.value.dynamicColorEnabled)
        }
    }

    fun selectPalette(palette: SerafinaPalette) {
        if (state.value.selectedPalette == palette) return

        viewModelScope.launch {
            prefs.setSelectedPalette(palette)
        }
    }

    fun selectThemeMode(themeMode: SerafinaThemeMode) {
        if (state.value.themeMode == themeMode) return

        viewModelScope.launch {
            prefs.setThemeMode(themeMode)
        }
    }

    fun setBlackThemeEnabled(enabled: Boolean) {
        if (state.value.blackThemeEnabled == enabled) return

        viewModelScope.launch {
            prefs.setBlackThemeEnabled(enabled)
        }
    }

    fun toggleReduceMotion() {
        viewModelScope.launch {
            prefs.setReduceMotion(!state.value.reduceMotion)
        }
    }

    fun toggleFingerprint() {
        viewModelScope.launch {
            prefs.setFingerprintEnabled(!state.value.fingerprintEnabled)
        }
    }

    fun setFingerprintEnabled(enabled: Boolean) {
        if (state.value.fingerprintEnabled == enabled) return

        viewModelScope.launch {
            prefs.setFingerprintEnabled(enabled)
        }
    }

    fun setPassword(password: String) {
        viewModelScope.launch {
            prefs.setPassword(password)
        }
    }

    fun clearPassword() {
        viewModelScope.launch {
            prefs.setPassword("")
        }
    }
}
