package com.ikiza.companion.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ikiza_settings")

/**
 * On-device repository managing application configuration via Jetpack DataStore.
 * Zero network, zero third-party analytics - 100% private to the device.
 */
class AppSettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val VOICE_ENABLED = booleanPreferencesKey("voice_enabled")
        val VOLUME = floatPreferencesKey("volume")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val SPEECH_PITCH = floatPreferencesKey("speech_pitch")

        val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        val QUIET_HOURS_START_HOUR = intPreferencesKey("quiet_hours_start_hour")
        val QUIET_HOURS_START_MINUTE = intPreferencesKey("quiet_hours_start_minute")
        val QUIET_HOURS_END_HOUR = intPreferencesKey("quiet_hours_end_hour")
        val QUIET_HOURS_END_MINUTE = intPreferencesKey("quiet_hours_end_minute")

        val EXCLUDED_PACKAGES = stringSetPreferencesKey("excluded_packages")
        val INCLUDE_SENDER_PACKAGES = stringSetPreferencesKey("include_sender_packages")
        val IGNORE_ONGOING = booleanPreferencesKey("ignore_ongoing")
        val IGNORE_SYSTEM = booleanPreferencesKey("ignore_system")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            voiceEnabled = prefs[PreferencesKeys.VOICE_ENABLED] ?: true,
            volume = prefs[PreferencesKeys.VOLUME] ?: 0.85f,
            speechRate = prefs[PreferencesKeys.SPEECH_RATE] ?: 1.05f,
            speechPitch = prefs[PreferencesKeys.SPEECH_PITCH] ?: 1.25f,
            quietHoursEnabled = prefs[PreferencesKeys.QUIET_HOURS_ENABLED] ?: false,
            quietHoursStartHour = prefs[PreferencesKeys.QUIET_HOURS_START_HOUR] ?: 22,
            quietHoursStartMinute = prefs[PreferencesKeys.QUIET_HOURS_START_MINUTE] ?: 0,
            quietHoursEndHour = prefs[PreferencesKeys.QUIET_HOURS_END_HOUR] ?: 7,
            quietHoursEndMinute = prefs[PreferencesKeys.QUIET_HOURS_END_MINUTE] ?: 0,
            excludedPackages = prefs[PreferencesKeys.EXCLUDED_PACKAGES] ?: emptySet(),
            includeSenderPackages = prefs[PreferencesKeys.INCLUDE_SENDER_PACKAGES] ?: emptySet(),
            ignoreOngoing = prefs[PreferencesKeys.IGNORE_ONGOING] ?: true,
            ignoreSystem = prefs[PreferencesKeys.IGNORE_SYSTEM] ?: true
        )
    }

    suspend fun setVoiceEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.VOICE_ENABLED] = enabled
        }
    }

    suspend fun setVolume(volume: Float) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.VOLUME] = volume.coerceIn(0.0f, 1.0f)
        }
    }

    suspend fun setSpeechRate(rate: Float) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SPEECH_RATE] = rate.coerceIn(0.5f, 2.0f)
        }
    }

    suspend fun setSpeechPitch(pitch: Float) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SPEECH_PITCH] = pitch.coerceIn(0.5f, 2.0f)
        }
    }

    suspend fun setQuietHoursEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.QUIET_HOURS_ENABLED] = enabled
        }
    }

    suspend fun setQuietHoursTime(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.QUIET_HOURS_START_HOUR] = startHour
            prefs[PreferencesKeys.QUIET_HOURS_START_MINUTE] = startMinute
            prefs[PreferencesKeys.QUIET_HOURS_END_HOUR] = endHour
            prefs[PreferencesKeys.QUIET_HOURS_END_MINUTE] = endMinute
        }
    }

    suspend fun addExcludedPackage(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[PreferencesKeys.EXCLUDED_PACKAGES] ?: emptySet()
            prefs[PreferencesKeys.EXCLUDED_PACKAGES] = current + packageName
        }
    }

    suspend fun removeExcludedPackage(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[PreferencesKeys.EXCLUDED_PACKAGES] ?: emptySet()
            prefs[PreferencesKeys.EXCLUDED_PACKAGES] = current - packageName
        }
    }

    suspend fun toggleSenderPackage(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[PreferencesKeys.INCLUDE_SENDER_PACKAGES] ?: emptySet()
            prefs[PreferencesKeys.INCLUDE_SENDER_PACKAGES] = if (packageName in current) {
                current - packageName
            } else {
                current + packageName
            }
        }
    }

    suspend fun setIgnoreOngoing(ignore: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.IGNORE_ONGOING] = ignore
        }
    }

    suspend fun setIgnoreSystem(ignore: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.IGNORE_SYSTEM] = ignore
        }
    }
}
