package com.ikiza.companion.notifications

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.ikiza.companion.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Android NotificationListenerService implementation.
 *
 * Requirements:
 * - Active only when "Phone Notifications & Calls" is enabled.
 * - Detects eligible posted notifications and calls.
 * - Identifies originating application.
 * - Formats concise announcement: "Notification from [App Name]."
 * - Deduplicates repeats within sliding window.
 * - Filters ongoing, self, and silent background notifications.
 */
class CompanionNotificationService : NotificationListenerService() {

    companion object {
        private const val TAG = "IKIZA_NotificationService"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val deduplicator = NotificationDeduplicator(windowMs = 8_000L)
    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(applicationContext)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val pkgName = sbn.packageName ?: return
        if (pkgName == applicationContext.packageName) return // Ignore IKIZA itself

        serviceScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            if (!settings.phoneNotificationsEnabled) {
                return@launch
            }

            val notification = sbn.notification ?: return@launch

            // Filter ongoing notifications (media playback, timers, persistent foreground services)
            if (sbn.isOngoing) {
                return@launch
            }

            val extras = notification.extras
            val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            val category = notification.category

            val pm = packageManager
            var resolvedLabel: String? = null
            try {
                val appInfo = pm.getApplicationInfo(pkgName, 0)
                resolvedLabel = pm.getApplicationLabel(appInfo).toString()
            } catch (_: PackageManager.NameNotFoundException) {
                // Resolved via fallback in AppNameResolver
            }

            val appName = AppNameResolver.resolve(resolvedLabel, pkgName)

            // Handle incoming call notifications (VoIP calls or phone app calls)
            if (category == Notification.CATEGORY_CALL) {
                val callKey = "call|$pkgName|${title.orEmpty()}"
                if (deduplicator.shouldAnnounce(pkgName, callKey)) {
                    val announcement = CallAnnouncementHelper.formatVoipCallAnnouncement(appName, title)
                    Log.i(TAG, "VoIP call detected: $announcement")
                    CompanionBridge.emitAnnouncement(announcement)
                }
                return@launch
            }

            // Handle standard app notifications
            val dedupeKey = "${title.orEmpty()}|${extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()}"
            if (deduplicator.shouldAnnounce(pkgName, dedupeKey)) {
                val announcement = "Notification from $appName."
                Log.i(TAG, "Notification announced: $announcement")
                CompanionBridge.emitAnnouncement(announcement)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // No action needed for removed notifications
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
