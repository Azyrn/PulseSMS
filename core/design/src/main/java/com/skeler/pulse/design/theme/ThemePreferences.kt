package com.skeler.pulse.design.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * DataStore-backed persistence for Serafina theme preferences.
 *
 * Stores:
 * - [dynamicColorEnabled] — whether to use wallpaper-based dynamic color (API 31+)
 * - [selectedPalette] — user-chosen [SerafinaPalette] seed
 * - [themeMode] — light, dark, or system-following theme mode
 * - [blackThemeEnabled] — whether dark mode should use pure black surfaces
 */
class ThemePreferences(private val context: Context) {

    private val store: DataStore<Preferences>
        get() = context.dataStore

    val state: Flow<SerafinaThemeState> =
        store.data
            .map(::preferencesToState)
            .distinctUntilChanged()

    val dynamicColorEnabled: Flow<Boolean> =
        state.map { themeState -> themeState.dynamicColorEnabled }.distinctUntilChanged()

    val selectedPalette: Flow<SerafinaPalette> =
        state.map { themeState -> themeState.selectedPalette }.distinctUntilChanged()

    val themeMode: Flow<SerafinaThemeMode> =
        state.map { themeState -> themeState.themeMode }.distinctUntilChanged()

    val blackThemeEnabled: Flow<Boolean> =
        state.map { themeState -> themeState.blackThemeEnabled }.distinctUntilChanged()

    val reduceMotion: Flow<Boolean> =
        state.map { themeState -> themeState.reduceMotion }.distinctUntilChanged()

    suspend fun currentState(): SerafinaThemeState =
        preferencesToState(store.data.first())

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        store.edit { prefs -> prefs[KEY_DYNAMIC_COLOR] = enabled }
    }

    suspend fun setSelectedPalette(palette: SerafinaPalette) {
        store.edit { prefs -> prefs[KEY_PALETTE] = palette.name }
    }

    suspend fun setThemeMode(themeMode: SerafinaThemeMode) {
        store.edit { prefs -> prefs[KEY_THEME_MODE] = themeMode.name }
    }

    suspend fun setBlackThemeEnabled(enabled: Boolean) {
        store.edit { prefs -> prefs[KEY_BLACK_THEME] = enabled }
    }

    suspend fun setReduceMotion(enabled: Boolean) {
        store.edit { prefs -> prefs[KEY_REDUCE_MOTION] = enabled }
    }

    companion object {
        private val Context.dataStore by preferencesDataStore(name = "serafina_theme_prefs")
        private val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color_enabled")
        private val KEY_PALETTE = stringPreferencesKey("selected_palette")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_BLACK_THEME = booleanPreferencesKey("black_theme_enabled")
        private val KEY_REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
    }

    private fun preferencesToState(prefs: Preferences): SerafinaThemeState {
        val paletteName = prefs[KEY_PALETTE] ?: SerafinaPalette.LavenderVolt.name
        val themeModeName = prefs[KEY_THEME_MODE] ?: SerafinaThemeMode.System.name

        return SerafinaThemeState(
            dynamicColorEnabled = prefs[KEY_DYNAMIC_COLOR] ?: true,
            selectedPalette = SerafinaPalette.entries.firstOrNull { it.name == paletteName }
                ?: SerafinaPalette.LavenderVolt,
            themeMode = SerafinaThemeMode.entries.firstOrNull { it.name == themeModeName }
                ?: SerafinaThemeMode.System,
            blackThemeEnabled = prefs[KEY_BLACK_THEME] ?: false,
            reduceMotion = prefs[KEY_REDUCE_MOTION] ?: false,
        )
    }
}
