package com.ikiza.companion.core.model

/**
 * Facial expressions supported by the IKIZA companion.
 */
enum class CompanionExpression(
    val displayName: String,
    val defaultDurationMs: Long = 4000L
) {
    NEUTRAL("Neutral"),
    HAPPY("Happy"),
    SURPRISED("Surprised"),
    SLEEPY("Sleepy"),
    CURIOUS("Curious")
}
