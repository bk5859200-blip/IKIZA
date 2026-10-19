package com.ikiza.companion.notifications

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Event bridge decoupling background Android notification and telephony listeners
 * from the UI and TextToSpeech engine.
 */
object CompanionBridge {

    private val _announcements = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val announcements: SharedFlow<String> = _announcements.asSharedFlow()

    fun emitAnnouncement(text: String) {
        _announcements.tryEmit(text)
    }
}
