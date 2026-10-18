package com.ikiza.companion.notifications.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings

object NotificationAccessHelper {

    /**
     * Checks if notification listener access is enabled for IKIZA Companion.
     */
    fun isNotificationAccessGranted(context: Context): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false

        val myComponent = ComponentName(context, CompanionNotificationListenerService::class.java).flattenToString()
        return flat.contains(myComponent) || flat.contains(context.packageName)
    }

    /**
     * Creates an intent to direct the user to Android's Notification Access settings page.
     */
    fun createSettingsIntent(): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}
