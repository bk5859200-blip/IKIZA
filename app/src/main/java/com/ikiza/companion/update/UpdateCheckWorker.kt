package com.ikiza.companion.update

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ikiza.companion.settings.SettingsRepository
import kotlinx.coroutines.flow.firstOrNull

/**
 * Android WorkManager periodic worker that queries GitHub Releases in the background.
 *
 * Characteristics:
 * - Operates at non-intrusive periodic intervals (e.g. 6 to 12 hours).
 * - Enforces NetworkType.CONNECTED so it never loops or wastes battery when offline.
 * - Suppresses duplicate notifications for already-notified release tags.
 * - Subject to HyperOS and Android battery saver optimizations.
 */
class UpdateCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "IKIZA_UpdateWorker"
        const val WORK_NAME = "ikiza_periodic_update_check"
    }

    override suspend fun doWork(): Result {
        return try {
            val context = applicationContext
            val pInfo = try {
                context.packageManager.getPackageInfo(context.packageName, 0)
            } catch (_: Exception) {
                null
            }
            val installedVersion = pInfo?.versionName ?: "2.0.0"

            val settingsRepo = SettingsRepository(context)
            val lastNotifiedTag = settingsRepo.lastNotifiedUpdateTagFlow.firstOrNull()

            val updateChecker = UpdateChecker(owner = "bk5859200-blip", repo = "IKIZA")
            when (val checkResult = updateChecker.check(installedVersion)) {
                is CheckResult.UpdateAvailable -> {
                    val release = checkResult.release
                    val shouldNotify = UpdateNotificationHelper.shouldNotify(
                        releaseTag = release.tagName,
                        isDraft = false,
                        isPrerelease = false,
                        installedVersion = installedVersion,
                        lastNotifiedTag = lastNotifiedTag
                    )

                    if (shouldNotify) {
                        Log.i(TAG, "New update available: ${release.tagName}. Dispatching notification.")
                        UpdateNotificationHelper.dispatchUpdateNotification(context, release.tagName)
                        settingsRepo.setLastNotifiedUpdateTag(release.tagName)
                    } else {
                        Log.i(TAG, "Update ${release.tagName} available but already notified or not eligible.")
                    }
                    Result.success()
                }
                is CheckResult.UpToDate,
                is CheckResult.NoReleaseFound -> {
                    Log.d(TAG, "Companion is up to date (version $installedVersion).")
                    Result.success()
                }
                is CheckResult.Error -> {
                    Log.w(TAG, "Update check failed: ${checkResult.message}")
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during periodic update check: ${e.message}")
            Result.failure()
        }
    }
}
