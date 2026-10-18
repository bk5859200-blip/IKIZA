package com.ikiza.companion.notifications.service

import android.app.Notification
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.ikiza.companion.core.model.RawNotification

/**
 * Android NotificationListenerService implementation.
 *
 * Captures system notifications, extracts raw fields, and forwards them
 * into [NotificationBridge] for processing by the pure rules engine.
 */
class CompanionNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "IKIZA_ListenerService"
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        NotificationBridge.setServiceConnected(true)
        Log.i(TAG, "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        NotificationBridge.setServiceConnected(false)
        Log.i(TAG, "Notification listener disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val pkgName = sbn.packageName ?: return
        // Ignore IKIZA's own notifications
        if (pkgName == applicationContext.packageName) return

        val notification = sbn.notification ?: return
        val extras = notification.extras

        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val subText = extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

        val pm = packageManager
        var appName = pkgName
        var isSystem = false

        try {
            val appInfo = pm.getApplicationInfo(pkgName, 0)
            appName = pm.getApplicationLabel(appInfo).toString()
            isSystem = (appInfo.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
        } catch (_: PackageManager.NameNotFoundException) {
            // Keep pkgName as fallback appName
        }

        val raw = RawNotification(
            packageName = pkgName,
            appName = appName,
            title = title,
            text = text,
            subText = subText,
            postTimeMillis = sbn.postTime,
            isOngoing = sbn.isOngoing,
            isSystemApp = isSystem,
            key = sbn.key ?: ""
        )

        NotificationBridge.postNotification(raw)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // No action required for removed notifications in Phase 1
    }
}
