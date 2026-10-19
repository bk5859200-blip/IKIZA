package com.ikiza.companion.notifications

/**
 * Pure Kotlin helper for formatting call announcements.
 */
object CallAnnouncementHelper {

    fun formatCallAnnouncement(callerNameOrNumber: String?): String {
        val trimmed = callerNameOrNumber?.trim()
        return if (!trimmed.isNullOrBlank()) {
            "Call from $trimmed."
        } else {
            "Incoming call."
        }
    }

    fun formatVoipCallAnnouncement(appName: String, caller: String?): String {
        val trimmedCaller = caller?.trim()
        return if (!trimmedCaller.isNullOrBlank()) {
            "$appName call from $trimmedCaller."
        } else {
            "Incoming $appName call."
        }
    }
}
