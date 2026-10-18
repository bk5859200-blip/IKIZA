package com.ikiza.companion.speech

/**
 * State of the speech engine observed by the UI and character animation system.
 */
data class SpeechState(
    val isSpeaking: Boolean = false,
    val currentText: String? = null,
    val queueSize: Int = 0,
    val isInitialized: Boolean = false,
    val isMuted: Boolean = false
)
