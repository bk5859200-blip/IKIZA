package com.ikiza.companion.notifications

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationDeduplicatorTest {

    private var mockTime = 100_000L
    private val deduplicator = NotificationDeduplicator(windowMs = 8_000L, clock = { mockTime })

    @Test
    fun testFirstNotificationPasses() {
        val result = deduplicator.shouldAnnounce("com.whatsapp", "Alice|Hello")
        assertTrue("First notification should be announced", result)
    }

    @Test
    fun testImmediateDuplicateIsSuppressed() {
        deduplicator.shouldAnnounce("com.whatsapp", "Alice|Hello")

        // 2 seconds later (within 8s window) -> suppressed
        mockTime += 2_000L
        val result = deduplicator.shouldAnnounce("com.whatsapp", "Alice|Hello")
        assertFalse("Duplicate within sliding window must be suppressed", result)
    }

    @Test
    fun testDifferentMessagePasses() {
        deduplicator.shouldAnnounce("com.whatsapp", "Alice|Hello")

        mockTime += 1_000L
        val differentMsg = deduplicator.shouldAnnounce("com.whatsapp", "Bob|Hey there")
        assertTrue("Different message must be announced", differentMsg)

        val differentApp = deduplicator.shouldAnnounce("com.instagram.android", "Alice|Hello")
        assertTrue("Different app must be announced", differentApp)
    }

    @Test
    fun testNotificationAfterWindowExpiresPasses() {
        deduplicator.shouldAnnounce("com.whatsapp", "Alice|Hello")

        // 10 seconds later (past 8s window) -> allowed
        mockTime += 10_000L
        val result = deduplicator.shouldAnnounce("com.whatsapp", "Alice|Hello")
        assertTrue("Identical notification after sliding window must be announced", result)
    }
}
