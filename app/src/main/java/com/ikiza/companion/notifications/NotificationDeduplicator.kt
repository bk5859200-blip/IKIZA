package com.ikiza.companion.notifications

/**
 * Pure Kotlin sliding-window deduplicator to prevent repeating identical announcements.
 */
class NotificationDeduplicator(
    private val windowMs: Long = 8_000L,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private val history = mutableMapOf<String, Long>()

    /**
     * Returns true if notification is new and should be announced; false if it is a duplicate.
     */
    fun shouldAnnounce(packageName: String, keyOrTitle: String): Boolean {
        val now = clock()
        cleanOldEntries(now)

        val signature = "$packageName|$keyOrTitle"
        val lastSeen = history[signature]

        return if (lastSeen != null && (now - lastSeen) < windowMs) {
            false
        } else {
            history[signature] = now
            true
        }
    }

    fun clear() {
        history.clear()
    }

    private fun cleanOldEntries(now: Long) {
        val iterator = history.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value > windowMs * 2) {
                iterator.remove()
            }
        }
    }
}
