package com.ikiza.companion.notifications.engine

import com.ikiza.companion.core.model.CompanionExpression
import com.ikiza.companion.core.model.NotificationRuleConfig
import com.ikiza.companion.core.model.ProcessedAnnouncement
import com.ikiza.companion.core.model.RawNotification

/**
 * Pure Kotlin rules engine for evaluating and filtering notifications.
 *
 * Implements:
 * 1. Ongoing / system notification filtering
 * 2. Per-app exclusion lists
 * 3. Quiet hours suppression
 * 4. Exact deduplication within a sliding time window
 * 5. Rapid-fire burst collapsing ("5 new messages on WhatsApp")
 * 6. Sender name formatting vs app-name-only default
 *
 * Fully decoupled from Android OS classes for deterministic JVM unit testing.
 */
class NotificationProcessor(
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private data class DedupeRecord(
        val key: String,
        val timestamp: Long
    )

    private data class BurstRecord(
        var count: Int,
        var lastTimestamp: Long
    )

    // In-memory sliding window history
    private val dedupeHistory = mutableMapOf<String, Long>()
    private val burstMap = mutableMapOf<String, BurstRecord>()

    /**
     * Clear all in-memory tracking states (useful for testing or cache resets).
     */
    fun reset() {
        dedupeHistory.clear()
        burstMap.clear()
    }

    /**
     * Evaluates an incoming [RawNotification] against [config].
     *
     * @return [ProcessedAnnouncement] if the notification should be voiced, or null if suppressed.
     */
    fun process(
        raw: RawNotification,
        config: NotificationRuleConfig
    ): ProcessedAnnouncement? {
        val now = clock()
        val postTime = if (raw.postTimeMillis > 0) raw.postTimeMillis else now

        // 1. Filter ongoing notifications (media playback, timers, ongoing calls, foreground services)
        if (raw.isOngoing && config.ignoreOngoing) {
            return null
        }

        // 2. Filter system packages (Android system, SystemUI, download managers, etc.)
        if (raw.isSystemApp && config.ignoreSystem) {
            return null
        }

        // 3. Excluded apps list
        if (config.excludedPackages.contains(raw.packageName)) {
            return null
        }

        // 4. Quiet hours check
        if (config.quietHours.isInside(postTime)) {
            return null
        }

        // 5. Deduplication check
        // We build a semantic signature based on package, title, and content text
        val dedupeKey = "${raw.packageName}|${raw.title.orEmpty().trim()}|${raw.text.orEmpty().trim()}"
        cleanOldDedupeEntries(now, config.dedupeWindowMs * 2)

        val lastDedupeTime = dedupeHistory[dedupeKey]
        if (lastDedupeTime != null && (now - lastDedupeTime) < config.dedupeWindowMs) {
            // Drop repeat
            return null
        }
        dedupeHistory[dedupeKey] = now

        // 6. Burst collapsing check
        val burstRecord = burstMap[raw.packageName]
        val isBurst: Boolean
        val burstCount: Int

        if (burstRecord != null && (now - burstRecord.lastTimestamp) <= config.burstWindowMs) {
            burstRecord.count += 1
            burstRecord.lastTimestamp = now
            isBurst = true
            burstCount = burstRecord.count
        } else {
            burstMap[raw.packageName] = BurstRecord(count = 1, lastTimestamp = now)
            isBurst = false
            burstCount = 1
        }

        // 7. Format announcement text
        val includeSender = config.includeSenderPackages.contains(raw.packageName)
        val sender = raw.title?.trim()

        val textToSpeak: String
        val targetExpression: CompanionExpression

        if (isBurst && burstCount > 1) {
            // Burst format: "5 new messages on WhatsApp"
            textToSpeak = "$burstCount new messages on ${raw.appName}"
            targetExpression = CompanionExpression.SURPRISED
        } else {
            // Single announcement
            textToSpeak = if (includeSender && !sender.isNullOrBlank()) {
                "Message from $sender on ${raw.appName}"
            } else {
                "Message from ${raw.appName}"
            }
            targetExpression = CompanionExpression.HAPPY
        }

        val displayContent = when {
            isBurst && burstCount > 1 -> "$burstCount new messages"
            !raw.text.isNullOrBlank() && !sender.isNullOrBlank() -> "$sender: ${raw.text}"
            !raw.text.isNullOrBlank() -> raw.text
            !sender.isNullOrBlank() -> sender
            else -> "New notification"
        }

        return ProcessedAnnouncement(
            textToSpeak = textToSpeak,
            displayText = "${raw.appName}: $displayContent",
            targetExpression = targetExpression,
            postTimeMillis = postTime,
            packageName = raw.packageName,
            appName = raw.appName,
            isBurst = isBurst && burstCount > 1,
            burstCount = burstCount
        )
    }

    private fun cleanOldDedupeEntries(now: Long, maxAgeMs: Long) {
        val iterator = dedupeHistory.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value > maxAgeMs) {
                iterator.remove()
            }
        }
    }
}
