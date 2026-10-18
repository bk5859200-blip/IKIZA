package com.ikiza.companion.settings

import com.ikiza.companion.core.model.NotificationRuleConfig
import com.ikiza.companion.core.model.QuietHours

/**
 * On-device persistent user preferences.
 */
data class AppSettings(
    val voiceEnabled: Boolean = true,
    val volume: Float = 0.85f,
    val speechRate: Float = 1.05f,
    val speechPitch: Float = 1.25f,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStartHour: Int = 22,
    val quietHoursStartMinute: Int = 0,
    val quietHoursEndHour: Int = 7,
    val quietHoursEndMinute: Int = 0,
    val excludedPackages: Set<String> = emptySet(),
    val includeSenderPackages: Set<String> = emptySet(),
    val ignoreOngoing: Boolean = true,
    val ignoreSystem: Boolean = true
) {
    fun toNotificationRuleConfig(): NotificationRuleConfig {
        return NotificationRuleConfig(
            quietHours = QuietHours(
                enabled = quietHoursEnabled,
                startHour = quietHoursStartHour,
                startMinute = quietHoursStartMinute,
                endHour = quietHoursEndHour,
                endMinute = quietHoursEndMinute
            ),
            excludedPackages = excludedPackages,
            includeSenderPackages = includeSenderPackages,
            ignoreOngoing = ignoreOngoing,
            ignoreSystem = ignoreSystem
        )
    }
}
