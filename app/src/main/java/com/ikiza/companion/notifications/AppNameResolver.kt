package com.ikiza.companion.notifications

/**
 * Pure Kotlin app name resolution and normalization for spoken announcements.
 * Resolves clean spoken titles (e.g. "WhatsApp", "Instagram", "Messages")
 * and prevents letter-by-letter package spelling.
 */
object AppNameResolver {

    private val KNOWN_APP_PACKAGES = mapOf(
        "com.whatsapp" to "WhatsApp",
        "com.whatsapp.w4b" to "WhatsApp Business",
        "org.telegram.messenger" to "Telegram",
        "org.thoughtcrime.securesms" to "Signal",
        "com.instagram.android" to "Instagram",
        "com.google.android.apps.messaging" to "Messages",
        "com.google.android.gm" to "Gmail",
        "com.google.android.youtube" to "YouTube",
        "com.google.android.calendar" to "Google Calendar",
        "com.discord" to "Discord",
        "com.twitter.android" to "Twitter",
        "com.zhiliaoapp.musically" to "TikTok",
        "com.spotify.music" to "Spotify",
        "com.snapchat.android" to "Snapchat",
        "com.reddit.frontpage" to "Reddit",
        "com.Slack" to "Slack",
        "com.facebook.katana" to "Facebook",
        "com.facebook.orca" to "Messenger",
        "com.linkedin.android" to "LinkedIn",
        "com.pinterest" to "Pinterest",
        "com.netflix.mediaclient" to "Netflix",
        "com.amazon.mShop.android.shopping" to "Amazon",
        "com.ubercab" to "Uber",
        "com.microsoft.teams" to "Microsoft Teams",
        "com.google.android.dialer" to "Phone",
        "com.android.dialer" to "Phone",
        "com.samsung.android.dialer" to "Phone"
    )

    private val PREFIX_REGEX = Regex("^(com|org|net|io|co|de|app|in|eu)\\.")
    private val NOISE_SUFFIX_REGEX = Regex("\\.(android|mobile|app|client|release|beta)$", RegexOption.IGNORE_CASE)

    /**
     * Resolves a spoken, natural application name.
     */
    fun resolve(label: String?, packageName: String): String {
        // 1. Check known mapping
        val known = KNOWN_APP_PACKAGES[packageName.trim()]
        if (known != null) return known

        // 2. If label is provided and valid (not raw package name), clean it
        val trimmedLabel = label?.trim()
        if (!trimmedLabel.isNullOrBlank() && trimmedLabel != packageName && !trimmedLabel.contains(".")) {
            val cleaned = trimmedLabel.replace(Regex("[^\\p{L}\\p{N}\\s&'-]"), " ").trim()
            val collapsed = cleaned.replace(Regex("\\s+"), " ")
            if (collapsed.isNotBlank()) return collapsed
        }

        // 3. Fallback: Parse package name into clean spoken words
        return parseFromPackage(packageName)
    }

    private fun parseFromPackage(pkg: String): String {
        var trimmed = pkg.trim()
        if (trimmed.isBlank()) return "Notification"

        trimmed = trimmed.replace(PREFIX_REGEX, "")
        trimmed = trimmed.replace(NOISE_SUFFIX_REGEX, "")

        val segments = trimmed.split(".")
        val rawSegment = when {
            segments.isEmpty() -> trimmed
            segments.size == 1 -> segments[0]
            segments.last().equals("android", ignoreCase = true) -> segments.dropLast(1).lastOrNull() ?: segments[0]
            else -> segments.last()
        }

        val spaced = rawSegment
            .replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
            .replace(Regex("[_-]"), " ")
            .trim()

        return spaced.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
            .ifBlank { "Notification" }
    }
}
