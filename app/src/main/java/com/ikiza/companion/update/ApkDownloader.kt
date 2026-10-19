package com.ikiza.companion.update

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Downloads an update APK asset securely into the app's private cache directory.
 * Computes SHA-256 checksum during streaming.
 */
class ApkDownloader(private val context: Context) {

    suspend fun download(
        downloadUrl: String,
        targetFileName: String,
        expectedSha256: String? = null,
        sha256AssetUrl: String? = null,
        onProgress: (percent: Int, downloaded: Long, total: Long) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val targetFile = File(updatesDir, targetFileName.ifBlank { "ikiza-update.apk" })

        if (targetFile.exists()) {
            targetFile.delete()
        }

        var conn: HttpURLConnection? = null
        try {
            var currentUrl = downloadUrl
            var redirectCount = 0

            // Handle GitHub HTTP 302 redirects
            while (redirectCount < 5) {
                val url = URL(currentUrl)
                conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 12_000
                    readTimeout = 15_000
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "IKIZA-Companion-Downloader")
                }

                val status = conn.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                    status == HttpURLConnection.HTTP_MOVED_PERM ||
                    status == 307 || status == 308
                ) {
                    currentUrl = conn.getHeaderField("Location")
                    conn.disconnect()
                    redirectCount++
                } else if (status == HttpURLConnection.HTTP_OK) {
                    break
                } else {
                    throw IllegalStateException("Download server returned HTTP $status")
                }
            }

            val totalBytes = conn?.contentLength?.toLong() ?: -1L
            val inputStream: InputStream = conn?.inputStream
                ?: throw IllegalStateException("Could not open input stream from $currentUrl")

            val digest = MessageDigest.getInstance("SHA-256")
            var downloadedBytes = 0L
            val buffer = ByteArray(8192)

            FileOutputStream(targetFile).use { output ->
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    digest.update(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead

                    val percent = if (totalBytes > 0) {
                        ((downloadedBytes * 100) / totalBytes).toInt()
                    } else {
                        -1
                    }
                    onProgress(percent, downloadedBytes, totalBytes)
                }
            }

            if (targetFile.length() == 0L) {
                targetFile.delete()
                throw IllegalStateException("Downloaded APK file is empty.")
            }

            val computedSha256 = digest.digest().joinToString("") { "%02x".format(it) }

            // Determine target hash from explicit hash or companion .sha256 asset
            val targetSha256 = expectedSha256?.trim()?.lowercase()
                ?: if (!sha256AssetUrl.isNullOrBlank()) {
                    fetchChecksumFromUrl(sha256AssetUrl)
                } else {
                    null
                }

            if (targetSha256 != null) {
                if (!computedSha256.equals(targetSha256, ignoreCase = true)) {
                    targetFile.delete()
                    throw SecurityException(
                        "APK Checksum mismatch!\nExpected: $targetSha256\nComputed: $computedSha256"
                    )
                }
            }

            targetFile
        } finally {
            conn?.disconnect()
        }
    }

    private fun fetchChecksumFromUrl(urlStr: String): String? {
        return try {
            var currentUrl = urlStr
            var redirectCount = 0
            var conn: HttpURLConnection? = null

            while (redirectCount < 5) {
                val url = URL(currentUrl)
                conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8_000
                    readTimeout = 8_000
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "IKIZA-Companion-Downloader")
                }

                val status = conn.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                    status == HttpURLConnection.HTTP_MOVED_PERM ||
                    status == 307 || status == 308
                ) {
                    currentUrl = conn.getHeaderField("Location")
                    conn.disconnect()
                    redirectCount++
                } else if (status == HttpURLConnection.HTTP_OK) {
                    break
                } else {
                    return null
                }
            }

            val text = conn?.inputStream?.bufferedReader()?.use { it.readText() }
            conn?.disconnect()
            UpdateChecker.parseChecksumFileContent(text)
        } catch (_: Exception) {
            null
        }
    }
}
