package com.ikiza.companion.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ikiza.companion.core.model.CompanionExpression
import com.ikiza.companion.core.model.ProcessedAnnouncement
import com.ikiza.companion.notifications.engine.NotificationProcessor
import com.ikiza.companion.notifications.service.NotificationAccessHelper
import com.ikiza.companion.notifications.service.NotificationBridge
import com.ikiza.companion.settings.AppSettings
import com.ikiza.companion.settings.AppSettingsRepository
import com.ikiza.companion.speech.SpeechManager
import com.ikiza.companion.speech.SpeechState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Main orchestrator linking persistent settings, TTS speech queue,
 * incoming notification stream, and companion character animation state.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = AppSettingsRepository(application)
    val speechManager = SpeechManager(application)
    private val notificationProcessor = NotificationProcessor()

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val speechState: StateFlow<SpeechState> = speechManager.speechState

    private val _currentExpression = MutableStateFlow(CompanionExpression.NEUTRAL)
    val currentExpression: StateFlow<CompanionExpression> = _currentExpression.asStateFlow()

    private val _recentAnnouncement = MutableStateFlow<ProcessedAnnouncement?>(null)
    val recentAnnouncement: StateFlow<ProcessedAnnouncement?> = _recentAnnouncement.asStateFlow()

    private val _isNotificationAccessGranted = MutableStateFlow(false)
    val isNotificationAccessGranted: StateFlow<Boolean> = _isNotificationAccessGranted.asStateFlow()

    private var expressionResetJob: Job? = null

    init {
        // Synchronize speech manager settings with user preferences
        viewModelScope.launch {
            settings.collect { s ->
                speechManager.isMuted = !s.voiceEnabled
                speechManager.volume = s.volume
                speechManager.speechRate = s.speechRate
                speechManager.speechPitch = s.speechPitch
            }
        }

        // Listen for incoming notifications captured by CompanionNotificationListenerService
        viewModelScope.launch {
            NotificationBridge.incomingNotifications.collect { raw ->
                val currentConfig = settings.value.toNotificationRuleConfig()
                val announcement = notificationProcessor.process(raw, currentConfig)
                if (announcement != null) {
                    handleAnnouncement(announcement)
                }
            }
        }

        refreshNotificationAccessStatus()
    }

    fun refreshNotificationAccessStatus() {
        _isNotificationAccessGranted.value = NotificationAccessHelper.isNotificationAccessGranted(getApplication())
    }

    private fun handleAnnouncement(announcement: ProcessedAnnouncement) {
        _recentAnnouncement.value = announcement
        setExpression(announcement.targetExpression, autoResetMs = 6000L)

        // Queue speech sequentially so she never speaks over herself
        speechManager.speak(
            text = announcement.textToSpeak,
            onDone = {
                // Speech finished naturally
            }
        )
    }

    /**
     * Preview or trigger an expression manually.
     */
    fun setExpression(expression: CompanionExpression, autoResetMs: Long = 4000L) {
        expressionResetJob?.cancel()
        _currentExpression.value = expression

        if (expression != CompanionExpression.NEUTRAL && autoResetMs > 0) {
            expressionResetJob = viewModelScope.launch {
                delay(autoResetMs)
                _currentExpression.value = CompanionExpression.NEUTRAL
            }
        }
    }

    /**
     * Test speech synthesis.
     */
    fun testVoice() {
        setExpression(CompanionExpression.HAPPY, autoResetMs = 5000L)
        speechManager.speak("Hello! I am IKIZA, your desktop companion.")
    }

    /**
     * Toggle voice mute.
     */
    fun toggleVoice() {
        viewModelScope.launch {
            val current = settings.value.voiceEnabled
            settingsRepository.setVoiceEnabled(!current)
        }
    }

    fun setVolume(volume: Float) {
        viewModelScope.launch {
            settingsRepository.setVolume(volume)
        }
    }

    fun setSpeechRate(rate: Float) {
        viewModelScope.launch {
            settingsRepository.setSpeechRate(rate)
        }
    }

    fun setSpeechPitch(pitch: Float) {
        viewModelScope.launch {
            settingsRepository.setSpeechPitch(pitch)
        }
    }

    fun setQuietHoursEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setQuietHoursEnabled(enabled)
        }
    }

    fun setQuietHoursTime(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) {
        viewModelScope.launch {
            settingsRepository.setQuietHoursTime(startHour, startMinute, endHour, endMinute)
        }
    }

    fun addExcludedPackage(packageName: String) {
        if (packageName.isNotBlank()) {
            viewModelScope.launch {
                settingsRepository.addExcludedPackage(packageName.trim())
            }
        }
    }

    fun removeExcludedPackage(packageName: String) {
        viewModelScope.launch {
            settingsRepository.removeExcludedPackage(packageName)
        }
    }

    fun toggleSenderPackage(packageName: String) {
        viewModelScope.launch {
            settingsRepository.toggleSenderPackage(packageName)
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.shutdown()
    }
}
