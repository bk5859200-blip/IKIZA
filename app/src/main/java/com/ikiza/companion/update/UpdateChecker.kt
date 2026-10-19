package com.ikiza.companion.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

sealed class CheckResult {
    data class UpdateAvailable(val release: ReleaseInfo) : CheckResult()
    object UpToDate : CheckResult()
    object NoReleaseFound : CheckResult()
    data class Error(val message: String) : CheckResult()
}

/**
 * Checks GitHub Releases for new IKIZA updates.
 * User-initiated only; zero telemetry.
 */
class UpdateChecker(
    private val owner: String = "bk5859200-blip",
    private val repo: String = "IKIZA"
) {

    companion object {
        private val SHA256_HEADER_REGEX = Regex("""(?i)(?:sha-?256|checksum)[\s:=]+([a-f0-9]{64})""")
        private val STANDALONE_HEX_REGEX = Regex("""\b([a-f0-9]{64})\b""", RegexOption.IGNORE_CASE)

        fun extractSha256(text: String?): String? {
            if (text.isNullOrBlank()) return null
            val match = SHA256_HEADER_REGEX.find(text)
            if (match != null) {
                return match.groupValues[1].lowercase()
            }
            return null
        }

        fun parseChecksumFileContent(content: String?): String? {
            if (content.isNullOrBlank()) return null
            val match = STANDALONE_HEX_REGEX.find(content)
            return match?.groupValues?.get(1)?.lowercase()
        }
    }

    suspend fun check(installedVersion: String): CheckResult = withContext(Dispatchers.IO) {
        val endpoint = "https://api.github.com/repos/$owner/$repo/releases/latest"
        var conn: HttpURLConnection? = null

        try {
            val url = URL(endpoint)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "IKIZA-Companion-UpdateChecker")
            }

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                return@withContext CheckResult.NoReleaseFound
            }

            if (responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext CheckResult.Error("GitHub response error (HTTP $responseCode)")
            }

            val body = conn.inputStream.bufferedReader().use(BufferedReader::readText)
            val json = JSONObject(body)

            val tagName = json.optString("tag_name", "")
            if (tagName.isBlank()) {
                return@withContext CheckResult.NoReleaseFound
            }

            val notes = json.optString("body", "")
            val publishedAt = json.optString("published_at", "")

            // Find APK asset and optional companion checksum asset
            val assetsArray = json.optJSONArray("assets") ?: return@withContext CheckResult.NoReleaseFound
            var apkUrl: String? = null
            var apkName: String? = null
            var apkSize = 0L
            var sha256AssetUrl: String? = null

            for (i in 0 until assetsArray.length()) {
                val assetObj = assetsArray.getJSONObject(i)
                val name = assetObj.optString("name", "")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    apkUrl = assetObj.optString("browser_download_url", "")
                    apkName = name
                    apkSize = assetObj.optLong("size", 0L)
                } else if (name.endsWith(".sha256", ignoreCase = true) ||
                    name.equals("checksums.txt", ignoreCase = true) ||
                    name.equals("SHA256SUMS", ignoreCase = true)
                ) {
                    sha256AssetUrl = assetObj.optString("browser_download_url", "")
                }
            }

            if (apkUrl.isNullOrBlank() || apkName.isNullOrBlank()) {
                return@withContext CheckResult.Error("Release found ($tagName) but contains no .apk asset")
            }

            val expectedSha256 = extractSha256(notes)

            val release = ReleaseInfo(
                tagName = tagName,
                releaseNotes = notes,
                apkDownloadUrl = apkUrl,
                apkFileName = apkName,
                apkSizeBytes = apkSize,
                publishedAt = publishedAt,
                expectedSha256 = expectedSha256,
                sha256AssetUrl = sha256AssetUrl
            )

            if (UpdateVersionComparator.isNewer(tagName, installedVersion)) {
                CheckResult.UpdateAvailable(release)
            } else {
                CheckResult.UpToDate
            }
        } catch (e: Exception) {
            CheckResult.Error(e.localizedMessage ?: "Network connection error")
        } finally {
            conn?.disconnect()
        }
    }
}
