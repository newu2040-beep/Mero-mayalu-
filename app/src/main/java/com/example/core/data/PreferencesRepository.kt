package com.example.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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
    val appLockEnabled: Boolean = false,
    val showOnlineStatus: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val dailyRemindersEnabled: Boolean = true,
    val callRingtoneId: String = "himalayan_flute",
    val callCustomRingtoneUri: String = "",
    val messageRingtoneId: String = "lotus_chime",
    val messageCustomRingtoneUri: String = "",
    val notificationRingtoneId: String = "kathmandu_bell",
    val notificationCustomRingtoneUri: String = "",
    val acceptedDisclaimerUids: Set<String> = emptySet()
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
        val SHOW_ONLINE_STATUS = booleanPreferencesKey("show_online_status")
        val HAPTIC_FEEDBACK_ENABLED = booleanPreferencesKey("haptic_feedback_enabled")
        val DAILY_REMINDERS_ENABLED = booleanPreferencesKey("daily_reminders_enabled")
        val CALL_RINGTONE_ID = stringPreferencesKey("call_ringtone_id")
        val CALL_CUSTOM_RINGTONE_URI = stringPreferencesKey("call_custom_ringtone_uri")
        val MESSAGE_RINGTONE_ID = stringPreferencesKey("message_ringtone_id")
        val MESSAGE_CUSTOM_RINGTONE_URI = stringPreferencesKey("message_custom_ringtone_uri")
        val NOTIFICATION_RINGTONE_ID = stringPreferencesKey("notification_ringtone_id")
        val NOTIFICATION_CUSTOM_RINGTONE_URI = stringPreferencesKey("notification_custom_ringtone_uri")
        val ACCEPTED_DISCLAIMER_UIDS = stringSetPreferencesKey("accepted_disclaimer_uids")
    }

    val preferencesFlow: Flow<LocalAppPreferences> = context.meroDataStore.data.map { prefs ->
        LocalAppPreferences(
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE] ?: AppLanguage.NEPALI.code),
            themeCatalog = MeroThemeCatalog.fromId(prefs[Keys.THEME_CATALOG] ?: MeroThemeCatalog.HIMALAYAN_DUSK.id),
            fontCatalog = MeroFontCatalog.fromId(prefs[Keys.FONT_CATALOG] ?: MeroFontCatalog.MUKTA.id),
            themeMode = MeroThemeMode.fromId(prefs[Keys.THEME_MODE] ?: MeroThemeMode.SYSTEM.id),
            useBikramSambat = prefs[Keys.USE_BIKRAM_SAMBAT] ?: true,
            reducedMotion = prefs[Keys.REDUCED_MOTION] ?: false,
            appLockEnabled = prefs[Keys.APP_LOCK_ENABLED] ?: false,
            showOnlineStatus = prefs[Keys.SHOW_ONLINE_STATUS] ?: true,
            hapticFeedbackEnabled = prefs[Keys.HAPTIC_FEEDBACK_ENABLED] ?: true,
            dailyRemindersEnabled = prefs[Keys.DAILY_REMINDERS_ENABLED] ?: true,
            callRingtoneId = prefs[Keys.CALL_RINGTONE_ID] ?: "himalayan_flute",
            callCustomRingtoneUri = prefs[Keys.CALL_CUSTOM_RINGTONE_URI] ?: "",
            messageRingtoneId = prefs[Keys.MESSAGE_RINGTONE_ID] ?: "lotus_chime",
            messageCustomRingtoneUri = prefs[Keys.MESSAGE_CUSTOM_RINGTONE_URI] ?: "",
            notificationRingtoneId = prefs[Keys.NOTIFICATION_RINGTONE_ID] ?: "kathmandu_bell",
            notificationCustomRingtoneUri = prefs[Keys.NOTIFICATION_CUSTOM_RINGTONE_URI] ?: "",
            acceptedDisclaimerUids = prefs[Keys.ACCEPTED_DISCLAIMER_UIDS] ?: emptySet()
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

    suspend fun setShowOnlineStatus(enabled: Boolean) {
        context.meroDataStore.edit { it[Keys.SHOW_ONLINE_STATUS] = enabled }
    }

    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        context.meroDataStore.edit { it[Keys.HAPTIC_FEEDBACK_ENABLED] = enabled }
    }

    suspend fun setDailyRemindersEnabled(enabled: Boolean) {
        context.meroDataStore.edit { it[Keys.DAILY_REMINDERS_ENABLED] = enabled }
    }

    suspend fun setRingtone(channel: String, ringtoneId: String, customUri: String? = null) {
        context.meroDataStore.edit { prefs ->
            when (channel) {
                "call" -> {
                    prefs[Keys.CALL_RINGTONE_ID] = ringtoneId
                    if (customUri != null) prefs[Keys.CALL_CUSTOM_RINGTONE_URI] = customUri
                }
                "message" -> {
                    prefs[Keys.MESSAGE_RINGTONE_ID] = ringtoneId
                    if (customUri != null) prefs[Keys.MESSAGE_CUSTOM_RINGTONE_URI] = customUri
                }
                else -> {
                    prefs[Keys.NOTIFICATION_RINGTONE_ID] = ringtoneId
                    if (customUri != null) prefs[Keys.NOTIFICATION_CUSTOM_RINGTONE_URI] = customUri
                }
            }
        }
    }

    suspend fun markDisclaimerAccepted(uid: String) {
        if (uid.isBlank()) return
        context.meroDataStore.edit { prefs ->
            val current = prefs[Keys.ACCEPTED_DISCLAIMER_UIDS] ?: emptySet()
            prefs[Keys.ACCEPTED_DISCLAIMER_UIDS] = current + uid
        }
    }
}
