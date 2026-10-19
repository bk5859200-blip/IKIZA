package com.ikiza.companion.update

import java.io.File

data class ReleaseInfo(
    val tagName: String,
    val releaseNotes: String?,
    val apkDownloadUrl: String,
    val apkFileName: String,
    val apkSizeBytes: Long,
    val publishedAt: String?,
    val expectedSha256: String? = null,
    val sha256AssetUrl: String? = null
)

sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Checking : UpdateStatus()
    data class UpToDate(val currentVersion: String) : UpdateStatus()
    data class Available(val release: ReleaseInfo, val currentVersion: String) : UpdateStatus()
    data class Downloading(val progressPercent: Int, val downloadedBytes: Long, val totalBytes: Long) : UpdateStatus()
    data class ReadyToInstall(val apkFile: File, val release: ReleaseInfo) : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}
