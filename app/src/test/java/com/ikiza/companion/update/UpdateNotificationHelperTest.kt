package com.ikiza.companion.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateNotificationHelperTest {

    @Test
    fun testEligibleNewReleaseShouldNotify() {
        val shouldNotify = UpdateNotificationHelper.shouldNotify(
            releaseTag = "v2.0.1",
            isDraft = false,
            isPrerelease = false,
            installedVersion = "2.0.0",
            lastNotifiedTag = null
        )
        assertTrue(shouldNotify)
    }

    @Test
    fun testSameOrOlderVersionDoesNotNotify() {
        assertFalse(
            UpdateNotificationHelper.shouldNotify(
                releaseTag = "v2.0.0",
                isDraft = false,
                isPrerelease = false,
                installedVersion = "2.0.0",
                lastNotifiedTag = null
            )
        )

        assertFalse(
            UpdateNotificationHelper.shouldNotify(
                releaseTag = "v1.9.9",
                isDraft = false,
                isPrerelease = false,
                installedVersion = "2.0.0",
                lastNotifiedTag = null
            )
        )
    }

    @Test
    fun testDraftsAndPrereleasesAreIgnored() {
        assertFalse(
            UpdateNotificationHelper.shouldNotify(
                releaseTag = "v2.1.0",
                isDraft = true,
                isPrerelease = false,
                installedVersion = "2.0.0",
                lastNotifiedTag = null
            )
        )

        assertFalse(
            UpdateNotificationHelper.shouldNotify(
                releaseTag = "v2.1.0-alpha",
                isDraft = false,
                isPrerelease = true,
                installedVersion = "2.0.0",
                lastNotifiedTag = null
            )
        )
    }

    @Test
    fun testDuplicateSuppressionForAlreadyNotifiedRelease() {
        assertFalse(
            UpdateNotificationHelper.shouldNotify(
                releaseTag = "v2.1.0",
                isDraft = false,
                isPrerelease = false,
                installedVersion = "2.0.0",
                lastNotifiedTag = "v2.1.0"
            )
        )

        // But when a subsequent release arrives, notification is permitted again
        assertTrue(
            UpdateNotificationHelper.shouldNotify(
                releaseTag = "v2.1.1",
                isDraft = false,
                isPrerelease = false,
                installedVersion = "2.0.0",
                lastNotifiedTag = "v2.1.0"
            )
        )
    }
}
