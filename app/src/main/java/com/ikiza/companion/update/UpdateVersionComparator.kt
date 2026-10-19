package com.ikiza.companion.update

/**
 * Pure Kotlin semantic version comparison.
 * Compares version components numerically (e.g. 2.10.0 > 2.2.0, not alphabetically).
 */
object UpdateVersionComparator {

    /**
     * Returns true if [remoteTag] is strictly newer than [installedVersion].
     */
    fun isNewer(remoteTag: String, installedVersion: String): Boolean {
        val remoteNums = parseVersionNumbers(remoteTag)
        val installedNums = parseVersionNumbers(installedVersion)

        val maxLen = maxOf(remoteNums.size, installedNums.size)
        for (i in 0 until maxLen) {
            val r = remoteNums.getOrElse(i) { 0 }
            val cur = installedNums.getOrElse(i) { 0 }
            if (r > cur) return true
            if (r < cur) return false
        }
        return false
    }

    private fun parseVersionNumbers(raw: String): List<Int> {
        val sanitized = raw.trim()
            .removePrefix("v")
            .removePrefix("V")
            .split("-")[0] // Strip build suffix if any (e.g. 2.0.0-beta)

        return sanitized.split(".")
            .mapNotNull { it.trim().toIntOrNull() }
    }
}
