package com.ikiza.companion.settings

/**
 * On-device persisted preferences for IKIZA.
 *
 * Exactly one top-level setting: [phoneNotificationsEnabled].
 * Custom character image path is stored privately.
 */
data class AppSettings(
    val phoneNotificationsEnabled: Boolean = false,
    val customCharacterPath: String? = null
)
