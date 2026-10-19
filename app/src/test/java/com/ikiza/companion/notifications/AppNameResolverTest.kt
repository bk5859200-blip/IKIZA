package com.ikiza.companion.notifications

import org.junit.Assert.assertEquals
import org.junit.Test

class AppNameResolverTest {

    @Test
    fun testKnownAppPackageMappings() {
        assertEquals("WhatsApp", AppNameResolver.resolve(null, "com.whatsapp"))
        assertEquals("Instagram", AppNameResolver.resolve(null, "com.instagram.android"))
        assertEquals("Messages", AppNameResolver.resolve(null, "com.google.android.apps.messaging"))
        assertEquals("Telegram", AppNameResolver.resolve(null, "org.telegram.messenger"))
        assertEquals("Spotify", AppNameResolver.resolve(null, "com.spotify.music"))
    }

    @Test
    fun testValidLabelIsSanitizedAndUsed() {
        assertEquals("Duolingo", AppNameResolver.resolve("Duolingo", "com.duolingo"))
        assertEquals("My Super App", AppNameResolver.resolve("My Super App", "com.example.superapp"))
    }

    @Test
    fun testFallbackFromPackageNameWithCamelCase() {
        assertEquals("Secret Notes", AppNameResolver.resolve(null, "com.developer.secretNotes.android"))
        assertEquals("Fitness Tracker", AppNameResolver.resolve(null, "org.health.fitness_tracker"))
    }

    @Test
    fun testFallbackNeverReturnsEmpty() {
        val result = AppNameResolver.resolve(null, "")
        assertEquals("Notification", result)
    }
}
