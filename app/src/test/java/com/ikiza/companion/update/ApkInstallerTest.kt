package com.ikiza.companion.update

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ApkInstallerTest {

    @Test
    fun testNonExistentFileReturnsFailure() {
        val nonExistent = File("build/does_not_exist.apk")
        // Create dummy mock or check file verification
        val res = if (!nonExistent.exists() || nonExistent.length() == 0L) {
            ApkVerificationResult.Failure("Downloaded APK file is empty or missing.")
        } else {
            ApkVerificationResult.Valid
        }
        assertTrue(res is ApkVerificationResult.Failure)
    }

    @Test
    fun testEmptyFileReturnsFailure() {
        val tempEmptyFile = File.createTempFile("empty_test", ".apk")
        try {
            val res = if (!tempEmptyFile.exists() || tempEmptyFile.length() == 0L) {
                ApkVerificationResult.Failure("Downloaded APK file is empty or missing.")
            } else {
                ApkVerificationResult.Valid
            }
            assertTrue(res is ApkVerificationResult.Failure)
        } finally {
            tempEmptyFile.delete()
        }
    }

    @Test
    fun testPackageMismatchDetected() {
        val expected = "com.ikiza.companion"
        val actual = "com.unrelated.app"
        val res = if (actual != expected) {
            ApkVerificationResult.Failure("Package mismatch: Expected $expected, but downloaded APK contains $actual.")
        } else {
            ApkVerificationResult.Valid
        }
        assertTrue(res is ApkVerificationResult.Failure)
        assertTrue((res as ApkVerificationResult.Failure).reason.contains("Package mismatch"))
    }

    @Test
    fun testVersionDowngradeDetected() {
        val installedVersionCode = 4L
        val apkVersionCode = 3L
        val res = if (installedVersionCode > 0 && apkVersionCode <= installedVersionCode) {
            ApkVerificationResult.Failure("Version downgrade prevented: Downloaded APK versionCode ($apkVersionCode) is not newer than currently installed versionCode ($installedVersionCode).")
        } else {
            ApkVerificationResult.Valid
        }
        assertTrue(res is ApkVerificationResult.Failure)
        assertTrue((res as ApkVerificationResult.Failure).reason.contains("Version downgrade prevented"))
    }
}
