package com.ikiza.companion

import android.app.Application

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ikiza.companion.update.UpdateCheckWorker
import com.ikiza.companion.update.UpdateNotificationHelper
import java.util.concurrent.TimeUnit

/**
 * Main application class for IKIZA V2.
 * Initializes notification channels and schedules conservative background update checks.
 */
class IkizaApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize notification channel for updates
        UpdateNotificationHelper.createNotificationChannel(this)

        // 2. Schedule non-intrusive background update checks (every 6 hours when connected to network)
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val updateWorkRequest = PeriodicWorkRequestBuilder<UpdateCheckWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                UpdateCheckWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                updateWorkRequest
            )
        } catch (_: Exception) {
            // Graceful fallback if WorkManager initialization is restricted by system
        }
    }
}
