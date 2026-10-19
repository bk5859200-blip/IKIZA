package com.ikiza.companion.character.animation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Coordinates anime companion animation states, event reactions, speech synchronization,
 * and idle/sleepy inactivity transitions.
 */
class CompanionAnimationController(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    val inactivityTimeoutMs: Long = 90_000L
) {

    private val _state = MutableStateFlow(CompanionAnimationState.Idle)
    val state: StateFlow<CompanionAnimationState> = _state.asStateFlow()

    private var reactionJob: Job? = null
    private var inactivityJob: Job? = null
    private var isCallActive = false
    private var isSpeaking = false
    private var isTaskActive = false

    init {
        resetInactivityTimer()
    }

    /**
     * User touched or interacted with the app; wakes up sleepy companion and resets timeout.
     */
    fun onUserInteraction() {
        if (_state.value == CompanionAnimationState.Sleepy) {
            _state.value = CompanionAnimationState.Idle
        }
        resetInactivityTimer()
    }

    /**
     * Triggers the gentle greeting reaction upon double-tap reveal.
     */
    fun onGreeting(durationMs: Long = 1_500L) {
        triggerTemporaryReaction(CompanionAnimationState.Greeting, durationMs)
    }

    /**
     * Triggers a small, attentive perk-up reaction when an incoming notification is announced.
     */
    fun onNotificationReceived(durationMs: Long = 1_200L) {
        triggerTemporaryReaction(CompanionAnimationState.NotificationReaction, durationMs)
    }

    /**
     * Updates speaking state according to TTS engine callbacks.
     */
    fun onSpeechStateChanged(speaking: Boolean) {
        isSpeaking = speaking
        if (speaking) {
            reactionJob?.cancel()
            _state.value = CompanionAnimationState.Speaking
            resetInactivityTimer()
        } else {
            returnToBaseline()
        }
    }

    /**
     * Updates call state (incoming or ringing).
     */
    fun onCallStateChanged(ringing: Boolean) {
        isCallActive = ringing
        if (ringing) {
            reactionJob?.cancel()
            _state.value = CompanionAnimationState.CallReaction
            resetInactivityTimer()
        } else {
            returnToBaseline()
        }
    }

    /**
     * Sets thinking state during genuine long-running async tasks (e.g. update check).
     */
    fun onTaskStateChanged(active: Boolean) {
        isTaskActive = active
        if (active) {
            reactionJob?.cancel()
            _state.value = CompanionAnimationState.Thinking
            resetInactivityTimer()
        } else {
            returnToBaseline()
        }
    }

    private fun triggerTemporaryReaction(reaction: CompanionAnimationState, durationMs: Long) {
        resetInactivityTimer()
        if (isCallActive || isSpeaking) {
            // Priority: do not override urgent call alert or active speech
            return
        }

        reactionJob?.cancel()
        reactionJob = scope.launch {
            _state.value = reaction
            delay(durationMs)
            returnToBaseline()
        }
    }

    private fun returnToBaseline() {
        _state.value = when {
            isCallActive -> CompanionAnimationState.CallReaction
            isSpeaking -> CompanionAnimationState.Speaking
            isTaskActive -> CompanionAnimationState.Thinking
            else -> CompanionAnimationState.Idle
        }
        resetInactivityTimer()
    }

    private fun resetInactivityTimer() {
        inactivityJob?.cancel()
        inactivityJob = scope.launch {
            delay(inactivityTimeoutMs)
            if (isActive && !isCallActive && !isSpeaking && !isTaskActive && _state.value == CompanionAnimationState.Idle) {
                _state.value = CompanionAnimationState.Sleepy
            }
        }
    }

    fun stop() {
        reactionJob?.cancel()
        inactivityJob?.cancel()
    }
}
