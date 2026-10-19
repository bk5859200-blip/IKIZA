package com.ikiza.companion.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ikiza.companion.ui.MainActivity

/**
 * Pure Kotlin logic and Android notification dispatch for companion app updates.
 */
object UpdateNotificationHelper {

    const val CHANNEL_ID = "ikiza_app_updates"
    const val NOTIFICATION_ID = 4001
    const val ACTION_OPEN_UPDATES = "com.ikiza.companion.action.OPEN_UPDATES"

    /**
     * Evaluates whether an update notification should be posted.
     * Enforces:
     * - Only stable public releases (no drafts or prereleases).
     * - Strict version comparison (must be newer than installed).
     * - Duplicate suppression (will not notify twice for the exact same tag).
     */
    fun shouldNotify(
        releaseTag: String,
        isDraft: Boolean,
        isPrerelease: Boolean,
        installedVersion: String,
        lastNotifiedTag: String?
    ): Boolean {
        if (isDraft || isPrerelease) {
            return false
        }

        if (!UpdateVersionComparator.isNewer(releaseTag, installedVersion)) {
            return false
        }

        if (!lastNotifiedTag.isNullOrBlank() && releaseTag.equals(lastNotifiedTag.trim(), ignoreCase = true)) {
            return false
        }

        return true
    }

    /**
     * Creates the Android notification channel on API 26+.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "App Updates"
            val description = "Notifications when a newer stable version of IKIZA is available."
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                this.description = description
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Dispatches an update notification to the user's phone.
     */
    fun dispatchUpdateNotification(context: Context, releaseTag: String) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_UPDATES
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("IKIZA Update Available")
            .setContentText("Version $releaseTag is available. Tap to inspect and update.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("A newer version of IKIZA ($releaseTag) is available on GitHub Releases.\nTap to review changes and install."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Android 13+ POST_NOTIFICATIONS permission not yet granted by user
        }
    }
}
