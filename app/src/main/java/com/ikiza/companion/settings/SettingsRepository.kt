package com.ikiza.companion.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ikiza_v2_settings")

/**
 * On-device repository managing application configuration via Jetpack DataStore.
 * Zero network transmission; 100% private to the device.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val PHONE_NOTIFICATIONS_ENABLED = booleanPreferencesKey("phone_notifications_enabled")
        val CUSTOM_CHARACTER_PATH = stringPreferencesKey("custom_character_path")
        val LAST_NOTIFIED_UPDATE_TAG = stringPreferencesKey("last_notified_update_tag")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            phoneNotificationsEnabled = prefs[Keys.PHONE_NOTIFICATIONS_ENABLED] ?: false,
            customCharacterPath = prefs[Keys.CUSTOM_CHARACTER_PATH]
        )
    }

    val lastNotifiedUpdateTagFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAST_NOTIFIED_UPDATE_TAG]
    }

    suspend fun setPhoneNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PHONE_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setLastNotifiedUpdateTag(tag: String?) {
        context.dataStore.edit { prefs ->
            if (tag == null) {
                prefs.remove(Keys.LAST_NOTIFIED_UPDATE_TAG)
            } else {
                prefs[Keys.LAST_NOTIFIED_UPDATE_TAG] = tag
            }
        }
    }

    suspend fun setCustomCharacterPath(path: String?) {
        context.dataStore.edit { prefs ->
            if (path == null) {
                prefs.remove(Keys.CUSTOM_CHARACTER_PATH)
            } else {
                prefs[Keys.CUSTOM_CHARACTER_PATH] = path
            }
        }
    }
}
