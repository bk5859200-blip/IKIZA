package com.ikiza.companion.character.animation

/**
 * High-level animation states for the IKIZA anime companion.
 */
enum class CompanionAnimationState {
    /**
     * Default gentle breathing and subtle vertical floating.
     */
    Idle,

    /**
     * Subtle rhythmic speech motion synchronized with TTS output.
     */
    Speaking,

    /**
     * Gentle contemplative floating triggered during genuine async tasks (e.g. update check).
     */
    Thinking,

    /**
     * Gentle perk-up welcome reaction when character is revealed on double-tap.
     */
    Greeting,

    /**
     * Small, attentive reaction when an incoming app notification arrives.
     */
    NotificationReaction,

    /**
     * Distinct, restrained alert rhythm when an incoming phone or VoIP call is ringing.
     */
    CallReaction,

    /**
     * Subtle curious tilt.
     */
    Curious,

    /**
     * Slower, deeper resting breathing rhythm after a period of user inactivity.
     */
    Sleepy
}
