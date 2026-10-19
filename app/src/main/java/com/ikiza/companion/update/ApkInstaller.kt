package com.ikiza.companion.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

sealed class ApkVerificationResult {
    object Valid : ApkVerificationResult()
    data class Failure(val reason: String) : ApkVerificationResult()
}

object ApkInstaller {

    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun createInstallPermissionIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    /**
     * Verifies that the downloaded APK:
     * 1. Is a valid parseable Android package archive.
     * 2. Matches the expected application ID (com.ikiza.companion).
     * 3. Has a versionCode greater than the currently installed application (downgrade prevention).
     * 4. Is signed with a compatible certificate to prevent update signature failures.
     */
    fun verifyApk(context: Context, apkFile: File): ApkVerificationResult {
        if (!apkFile.exists() || apkFile.length() == 0L) {
            return ApkVerificationResult.Failure("Downloaded APK file is empty or missing.")
        }

        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }

        val archiveInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, flags)
            ?: return ApkVerificationResult.Failure("Corrupt or invalid APK archive. Failed to parse package metadata.")

        // 1. Verify package identity
        val expectedPackage = context.packageName
        if (archiveInfo.packageName != expectedPackage) {
            return ApkVerificationResult.Failure(
                "Package mismatch: Expected $expectedPackage, but downloaded APK contains ${archiveInfo.packageName}."
            )
        }

        // 2. Verify version upgrade (prevent downgrade failures)
        val installedPackageInfo = try {
            pm.getPackageInfo(expectedPackage, flags)
        } catch (_: Exception) {
            null
        }

        val installedVersionCode = if (installedPackageInfo != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                installedPackageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                installedPackageInfo.versionCode.toLong()
            }
        } else {
            0L
        }

        val apkVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            archiveInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            archiveInfo.versionCode.toLong()
        }

        if (installedVersionCode > 0 && apkVersionCode <= installedVersionCode) {
            return ApkVerificationResult.Failure(
                "Version downgrade prevented: Downloaded APK versionCode ($apkVersionCode) is not newer than currently installed versionCode ($installedVersionCode)."
            )
        }

        // 3. Verify signing certificate compatibility
        val installedCerts = getCertificates(installedPackageInfo)
        val apkCerts = getCertificates(archiveInfo)

        if (installedCerts.isNotEmpty() && apkCerts.isNotEmpty()) {
            val hasMatchingCertificate = installedCerts.any { instCert ->
                apkCerts.any { apkCert -> instCert.contentEquals(apkCert) }
            }
            if (!hasMatchingCertificate) {
                return ApkVerificationResult.Failure(
                    "Signing certificate incompatible: APK was signed with a different key than the installed app. Updating from this build would be rejected by Android."
                )
            }
        }

        return ApkVerificationResult.Valid
    }

    private fun getCertificates(packageInfo: PackageInfo?): List<ByteArray> {
        if (packageInfo == null) return emptyList()
        val certs = mutableListOf<ByteArray>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = packageInfo.signingInfo
            if (signingInfo != null) {
                if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners?.forEach { certs.add(it.toByteArray()) }
                } else {
                    signingInfo.signingCertificateHistory?.forEach { certs.add(it.toByteArray()) }
                }
            }
        }
        if (certs.isEmpty()) {
            @Suppress("DEPRECATION")
            packageInfo.signatures?.forEach { certs.add(it.toByteArray()) }
        }
        return certs
    }

    fun installApk(context: Context, apkFile: File) {
        val verification = verifyApk(context, apkFile)
        if (verification is ApkVerificationResult.Failure) {
            throw SecurityException(verification.reason)
        }

        val authority = "${context.packageName}.fileprovider"
        val contentUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(installIntent)
    }
}
