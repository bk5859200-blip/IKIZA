package com.ikiza.companion.core.model

import java.util.Calendar
import java.util.TimeZone

/**
 * Pure Kotlin representation of an incoming notification.
 * Decoupled from Android's StatusBarNotification for unit testing.
 */
data class RawNotification(
    val packageName: String,
    val appName: String,
    val title: String? = null,
    val text: String? = null,
    val subText: String? = null,
    val postTimeMillis: Long = System.currentTimeMillis(),
    val isOngoing: Boolean = false,
    val isSystemApp: Boolean = false,
    val key: String = ""
)

/**
 * Time-based quiet hours configuration.
 *
 * Supports midnight crossing, e.g. start at 22:00 and end at 07:00.
 */
data class QuietHours(
    val enabled: Boolean = false,
    val startHour: Int = 22,
    val startMinute: Int = 0,
    val endHour: Int = 7,
    val endMinute: Int = 0
) {
    /**
     * Determines whether [timestampMillis] falls within the quiet hours interval.
     */
    fun isInside(
        timestampMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Boolean {
        if (!enabled) return false

        val calendar = Calendar.getInstance(timeZone).apply {
            timeInMillis = timestampMillis
        }
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val startTotalMinutes = startHour * 60 + startMinute
        val endTotalMinutes = endHour * 60 + endMinute

        return if (startTotalMinutes <= endTotalMinutes) {
            // Same-day range, e.g. 13:00 to 15:00
            currentMinutes in startTotalMinutes until endTotalMinutes
        } else {
            // Overnight range spanning midnight, e.g. 22:00 to 07:00
            currentMinutes >= startTotalMinutes || currentMinutes < endTotalMinutes
        }
    }
}

/**
 * Rules applied by the notification evaluator.
 */
data class NotificationRuleConfig(
    val quietHours: QuietHours = QuietHours(),
    val excludedPackages: Set<String> = emptySet(),
    val includeSenderPackages: Set<String> = emptySet(),
    val ignoreOngoing: Boolean = true,
    val ignoreSystem: Boolean = true,
    val dedupeWindowMs: Long = 10_000L,
    val burstWindowMs: Long = 5_000L
)

/**
 * Result of evaluating an incoming notification.
 */
data class ProcessedAnnouncement(
    val textToSpeak: String,
    val displayText: String,
    val targetExpression: CompanionExpression = CompanionExpression.HAPPY,
    val postTimeMillis: Long = System.currentTimeMillis(),
    val packageName: String,
    val appName: String,
    val isBurst: Boolean = false,
    val burstCount: Int = 1
)
