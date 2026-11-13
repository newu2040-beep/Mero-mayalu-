package com.example.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.designsystem.MeroFontCatalog
import com.example.core.designsystem.MeroThemeCatalog
import com.example.core.designsystem.MeroThemeMode
import com.example.core.localization.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.meroDataStore: DataStore<Preferences> by preferencesDataStore(name = "mero_mayalu_prefs")

data class LocalAppPreferences(
    val language: AppLanguage = AppLanguage.NEPALI,
    val themeCatalog: MeroThemeCatalog = MeroThemeCatalog.HIMALAYAN_DUSK,
    val fontCatalog: MeroFontCatalog = MeroFontCatalog.MUKTA,
    val themeMode: MeroThemeMode = MeroThemeMode.SYSTEM,
    val useBikramSambat: Boolean = true,
    val reducedMotion: Boolean = false,
    val appLockEnabled: Boolean = false
)

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val THEME_CATALOG = stringPreferencesKey("theme_catalog")
        val FONT_CATALOG = stringPreferencesKey("font_catalog")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_BIKRAM_SAMBAT = booleanPreferencesKey("use_bikram_sambat")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
    }

    val preferencesFlow: Flow<LocalAppPreferences> = context.meroDataStore.data.map { prefs ->
        LocalAppPreferences(
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE] ?: AppLanguage.NEPALI.code),
            themeCatalog = MeroThemeCatalog.fromId(prefs[Keys.THEME_CATALOG] ?: MeroThemeCatalog.HIMALAYAN_DUSK.id),
            fontCatalog = MeroFontCatalog.fromId(prefs[Keys.FONT_CATALOG] ?: MeroFontCatalog.MUKTA.id),
            themeMode = MeroThemeMode.fromId(prefs[Keys.THEME_MODE] ?: MeroThemeMode.SYSTEM.id),
            useBikramSambat = prefs[Keys.USE_BIKRAM_SAMBAT] ?: true,
            reducedMotion = prefs[Keys.REDUCED_MOTION] ?: false,
            appLockEnabled = prefs[Keys.APP_LOCK_ENABLED] ?: false
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.meroDataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setThemeCatalog(catalog: MeroThemeCatalog) {
        context.meroDataStore.edit { it[Keys.THEME_CATALOG] = catalog.id }
    }

    suspend fun setFontCatalog(font: MeroFontCatalog) {
        context.meroDataStore.edit { it[Keys.FONT_CATALOG] = font.id }
    }

    suspend fun setThemeMode(mode: MeroThemeMode) {
        context.meroDataStore.edit { it[Keys.THEME_MODE] = mode.id }
    }

    suspend fun setUseBikramSambat(enabled: Boolean) {
        context.meroDataStore.edit { it[Keys.USE_BIKRAM_SAMBAT] = enabled }
    }

    suspend fun setReducedMotion(enabled: Boolean) {
        context.meroDataStore.edit { it[Keys.REDUCED_MOTION] = enabled }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.meroDataStore.edit { it[Keys.APP_LOCK_ENABLED] = enabled }
    }
}
