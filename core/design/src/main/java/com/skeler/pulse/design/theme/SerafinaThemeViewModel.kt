package com.skeler.pulse.design.theme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Immutable snapshot of the current Serafina theme configuration.
 */
data class SerafinaThemeState(
    val dynamicColorEnabled: Boolean = true,
    val selectedPalette: SerafinaPalette = SerafinaPalette.LavenderVolt,
    val themeMode: SerafinaThemeMode = SerafinaThemeMode.System,
    val blackThemeEnabled: Boolean = false,
    val reduceMotion: Boolean = false,
)

/**
 * ViewModel that exposes the user's theme preferences as a [StateFlow]
 * and provides mutation methods that persist changes to DataStore.
 */
class SerafinaThemeViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = ThemePreferences(application)

    val state: StateFlow<SerafinaThemeState> = combine(
        prefs.dynamicColorEnabled,
        prefs.selectedPalette,
        prefs.themeMode,
        prefs.blackThemeEnabled,
        prefs.reduceMotion,
    ) { dynamicColor, palette, themeMode, blackThemeEnabled, reduceMotion ->
        SerafinaThemeState(
            dynamicColorEnabled = dynamicColor,
            selectedPalette = palette,
            themeMode = themeMode,
            blackThemeEnabled = blackThemeEnabled,
            reduceMotion = reduceMotion,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SerafinaThemeState(),
    )

    fun toggleDynamicColor() {
        viewModelScope.launch {
            prefs.setDynamicColorEnabled(!state.value.dynamicColorEnabled)
        }
    }

    fun selectPalette(palette: SerafinaPalette) {
        viewModelScope.launch {
            prefs.setSelectedPalette(palette)
        }
    }

    fun selectThemeMode(themeMode: SerafinaThemeMode) {
        viewModelScope.launch {
            prefs.setThemeMode(themeMode)
        }
    }

    fun setBlackThemeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setBlackThemeEnabled(enabled)
        }
    }

    fun toggleReduceMotion() {
        viewModelScope.launch {
            prefs.setReduceMotion(!state.value.reduceMotion)
        }
    }
}
