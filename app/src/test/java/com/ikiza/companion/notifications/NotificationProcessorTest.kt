package com.ikiza.companion.notifications

import com.ikiza.companion.core.model.CompanionExpression
import com.ikiza.companion.core.model.NotificationRuleConfig
import com.ikiza.companion.core.model.QuietHours
import com.ikiza.companion.core.model.RawNotification
import com.ikiza.companion.notifications.engine.NotificationProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class NotificationProcessorTest {

    private var mockCurrentTime: Long = 100_000L
    private val processor = NotificationProcessor(clock = { mockCurrentTime })

    @Test
    fun testDeduplicationWithinWindow() {
        val config = NotificationRuleConfig(dedupeWindowMs = 10_000L)
        val n1 = RawNotification(
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            title = "Alice",
            text = "Hey, are you free?",
            postTimeMillis = mockCurrentTime
        )

        // First message should be announced
        val result1 = processor.process(n1, config)
        assertNotNull(result1)
        assertEquals("Message from WhatsApp", result1?.textToSpeak)

        // Same message arrives 3 seconds later (within 10s window) -> should be deduplicated (null)
        mockCurrentTime += 3_000L
        val n2 = n1.copy(postTimeMillis = mockCurrentTime)
        val result2 = processor.process(n2, config)
        assertNull("Duplicate notification within window should be suppressed", result2)
    }

    @Test
    fun testDeduplicationOutsideWindow() {
        val config = NotificationRuleConfig(dedupeWindowMs = 10_000L)
        val n1 = RawNotification(
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            title = "Alice",
            text = "Hey, are you free?",
            postTimeMillis = mockCurrentTime
        )

        val result1 = processor.process(n1, config)
        assertNotNull(result1)

        // Advance clock past the dedupe window (12 seconds)
        mockCurrentTime += 12_000L
        val n2 = n1.copy(postTimeMillis = mockCurrentTime)
        val result2 = processor.process(n2, config)
        assertNotNull("Identical notification after dedupe window should be allowed", result2)
    }

    @Test
    fun testBurstCollapsing() {
        val config = NotificationRuleConfig(
            burstWindowMs = 5_000L,
            dedupeWindowMs = 10_000L
        )

        // 1st notification
        val m1 = RawNotification(
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            title = "Alice",
            text = "Message 1",
            postTimeMillis = mockCurrentTime
        )
        val r1 = processor.process(m1, config)
        assertNotNull(r1)
        assertEquals("Message from WhatsApp", r1?.textToSpeak)
        assertEquals(1, r1?.burstCount)

        // 2nd notification 1.5 seconds later
        mockCurrentTime += 1_500L
        val m2 = RawNotification(
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            title = "Alice",
            text = "Message 2",
            postTimeMillis = mockCurrentTime
        )
        val r2 = processor.process(m2, config)
        assertNotNull(r2)
        assertTrue(r2!!.isBurst)
        assertEquals(2, r2.burstCount)
        assertEquals("2 new messages on WhatsApp", r2.textToSpeak)
        assertEquals(CompanionExpression.SURPRISED, r2.targetExpression)

        // 3rd notification 1 second later
        mockCurrentTime += 1_000L
        val m3 = RawNotification(
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            title = "Alice",
            text = "Message 3",
            postTimeMillis = mockCurrentTime
        )
        val r3 = processor.process(m3, config)
        assertNotNull(r3)
        assertTrue(r3!!.isBurst)
        assertEquals(3, r3.burstCount)
        assertEquals("3 new messages on WhatsApp", r3.textToSpeak)

        // 4th notification after burst window expires (advance 6 seconds)
        mockCurrentTime += 6_000L
        val m4 = RawNotification(
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            title = "Bob",
            text = "New conversation",
            postTimeMillis = mockCurrentTime
        )
        val r4 = processor.process(m4, config)
        assertNotNull(r4)
        assertEquals("Burst should have reset", 1, r4!!.burstCount)
        assertEquals("Message from WhatsApp", r4.textToSpeak)
        assertEquals(CompanionExpression.HAPPY, r4.targetExpression)
    }

    @Test
    fun testExcludedAppSuppression() {
        val config = NotificationRuleConfig(
            excludedPackages = setOf("com.spam.promos")
        )
        val promo = RawNotification(
            packageName = "com.spam.promos",
            appName = "Promos",
            title = "Special Offer",
            text = "50% off today!"
        )
        val result = processor.process(promo, config)
        assertNull("Excluded app notifications must be suppressed", result)
    }

    @Test
    fun testOngoingNotificationIgnoredByDefault() {
        val config = NotificationRuleConfig(ignoreOngoing = true)
        val ongoingMusic = RawNotification(
            packageName = "com.spotify.music",
            appName = "Spotify",
            title = "Song title",
            text = "Playing",
            isOngoing = true
        )
        val result = processor.process(ongoingMusic, config)
        assertNull("Ongoing notifications should be suppressed by default", result)
    }

    @Test
    fun testSystemNotificationIgnoredByDefault() {
        val config = NotificationRuleConfig(ignoreSystem = true)
        val systemNotice = RawNotification(
            packageName = "android",
            appName = "Android System",
            title = "USB debugging connected",
            text = "Tap to turn off",
            isSystemApp = true
        )
        val result = processor.process(systemNotice, config)
        assertNull("System notifications should be suppressed by default", result)
    }

    @Test
    fun testSenderNameInclusionToggle() {
        val configWithoutSender = NotificationRuleConfig(includeSenderPackages = emptySet())
        val configWithSender = NotificationRuleConfig(includeSenderPackages = setOf("org.telegram.messenger"))

        val telegramMsg = RawNotification(
            packageName = "org.telegram.messenger",
            appName = "Telegram",
            title = "John",
            text = "Can you send the document?"
        )

        // Without sender toggle: app name only
        val resultNoSender = processor.process(telegramMsg, configWithoutSender)
        assertEquals("Message from Telegram", resultNoSender?.textToSpeak)

        // With sender toggle: includes sender name
        processor.reset()
        val resultWithSender = processor.process(telegramMsg, configWithSender)
        assertEquals("Message from John on Telegram", resultWithSender?.textToSpeak)
    }

    @Test
    fun testQuietHoursNormalRange() {
        val tz = TimeZone.getTimeZone("UTC")
        val quietHours = QuietHours(
            enabled = true,
            startHour = 13,
            startMinute = 0,
            endHour = 15,
            endMinute = 0
        )

        // 14:00 UTC (inside quiet hours)
        val calInside = Calendar.getInstance(tz).apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 0)
        }
        assertTrue("14:00 should be inside 13:00-15:00", quietHours.isInside(calInside.timeInMillis, tz))

        // 16:00 UTC (outside quiet hours)
        val calOutside = Calendar.getInstance(tz).apply {
            set(Calendar.HOUR_OF_DAY, 16)
            set(Calendar.MINUTE, 0)
        }
        org.junit.Assert.assertFalse("16:00 should be outside 13:00-15:00", quietHours.isInside(calOutside.timeInMillis, tz))
    }

    @Test
    fun testQuietHoursOvernightSpanningMidnight() {
        val tz = TimeZone.getTimeZone("UTC")
        val quietHours = QuietHours(
            enabled = true,
            startHour = 22,
            startMinute = 0,
            endHour = 7,
            endMinute = 0
        )

        // 23:30 (inside night hours)
        val cal2330 = Calendar.getInstance(tz).apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 30)
        }
        assertTrue("23:30 should be inside quiet hours", quietHours.isInside(cal2330.timeInMillis, tz))

        // 02:15 (inside morning hours before 07:00)
        val cal0215 = Calendar.getInstance(tz).apply {
            set(Calendar.HOUR_OF_DAY, 2)
            set(Calendar.MINUTE, 15)
        }
        assertTrue("02:15 should be inside quiet hours", quietHours.isInside(cal0215.timeInMillis, tz))

        // 14:00 (outside quiet hours during daytime)
        val cal1400 = Calendar.getInstance(tz).apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 0)
        }
        org.junit.Assert.assertFalse("14:00 should be outside quiet hours", quietHours.isInside(cal1400.timeInMillis, tz))
    }
}
