package com.ikiza.companion.voice

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.ArrayDeque
import java.util.Locale
import java.util.UUID

/**
 * TextToSpeech engine manager enforcing strict American English (en-US) female voice selection,
 * sequential queueing, debounce handling, and clean lifecycle management.
 */
class TtsVoiceManager(private val context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "IKIZA_TtsManager"
        private const val GREETING_DEBOUNCE_MS = 2500L

        // Known Google & Android TTS female voice identifiers
        val KNOWN_FEMALE_VOICE_PATTERNS = listOf(
            "female",
            "en-us-x-sfg", // Google TTS female 1
            "en-us-x-tpd", // Google TTS female 2
            "en-us-x-iob", // Google TTS female 3
            "en-us-x-iol", // Google TTS female 4
            "en-us-x-iom", // Google TTS female 5
            "en-us-x-tpc"  // Google TTS female variant
        )

        val KNOWN_MALE_VOICE_PATTERNS = listOf(
            "en-us-x-iog",
            "en-us-x-ioj",
            "en-us-x-iok",
            "en-us-x-tpf"
        )

        fun isExplicitlyMale(name: String): Boolean {
            val lower = name.lowercase()
            if (KNOWN_MALE_VOICE_PATTERNS.any { lower.contains(it) }) return true
            return lower.contains("male") && !lower.contains("female")
        }

        fun isStrictFemaleVoice(name: String, features: Set<String> = emptySet()): Boolean {
            if (isExplicitlyMale(name)) return false

            for (pattern in KNOWN_FEMALE_VOICE_PATTERNS) {
                if (name.contains(pattern, ignoreCase = true)) return true
            }

            if (features.any { it.contains("female", ignoreCase = true) }) return true

            return false
        }
    }

    private data class QueueItem(
        val utteranceId: String,
        val text: String,
        val onDone: (() -> Unit)? = null
    )

    private val mainHandler = Handler(Looper.getMainLooper())
    private var textToSpeech: TextToSpeech? = null

    private val queueLock = Any()
    private val speechQueue = ArrayDeque<QueueItem>()
    private var isSpeaking = false
    private val _isSpeakingFlow = MutableStateFlow(false)
    val isSpeakingFlow: StateFlow<Boolean> = _isSpeakingFlow.asStateFlow()

    private var lastGreetingTime = 0L

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isFemaleVoiceReady = MutableStateFlow(false)
    val isFemaleVoiceReady: StateFlow<Boolean> = _isFemaleVoiceReady.asStateFlow()

    private val _selectedVoiceName = MutableStateFlow<String?>("Detecting voice...")
    val selectedVoiceName: StateFlow<String?> = _selectedVoiceName.asStateFlow()

    private val _voiceStatusMessage = MutableStateFlow<String?>(null)
    val voiceStatusMessage: StateFlow<String?> = _voiceStatusMessage.asStateFlow()

    init {
        textToSpeech = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            configureVoice()
            setupUtteranceListener()
            _isInitialized.value = true
            Log.i(TAG, "TextToSpeech successfully initialized")

            // Process anything waiting in queue only if female voice is ready
            synchronized(queueLock) {
                if (!isSpeaking && speechQueue.isNotEmpty() && _isFemaleVoiceReady.value) {
                    playNextLocked()
                }
            }
        } else {
            _isFemaleVoiceReady.value = false
            _selectedVoiceName.value = "Voice Disabled"
            _voiceStatusMessage.value = "TTS initialization failed. Please verify Text-to-Speech settings."
            Log.e(TAG, "TTS initialization failed with code: $status")
        }
    }

    /**
     * Re-checks voice availability, for example when user returns from system TTS settings.
     */
    fun recheckVoiceConfiguration() {
        if (_isInitialized.value) {
            configureVoice()
        }
    }

    private fun configureVoice() {
        val tts = textToSpeech ?: return

        // 1. Set language to US English
        val langResult = tts.setLanguage(Locale.US)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            _isFemaleVoiceReady.value = false
            _selectedVoiceName.value = "Voice Disabled"
            _voiceStatusMessage.value = "American English (en-US) voice data is missing. Please download Google Speech Recognition & Synthesis from Android settings."
            Log.w(TAG, "en-US language data missing or not supported")
            return
        }

        // 2. Discover and set a high-quality female American English voice
        try {
            val availableVoices = tts.voices
            val usVoices = availableVoices?.filter { voice ->
                val loc = voice.locale
                loc.language.equals("en", ignoreCase = true) &&
                        (loc.country.equals("US", ignoreCase = true) || loc.country.isBlank())
            } ?: emptyList()

            val selectedVoice = findStrictFemaleVoice(usVoices)

            if (selectedVoice != null) {
                tts.voice = selectedVoice
                _isFemaleVoiceReady.value = true
                _selectedVoiceName.value = "${selectedVoice.name} (en-US Female)"
                _voiceStatusMessage.value = null
                Log.i(TAG, "Selected strict female en-US voice: ${selectedVoice.name}")
            } else {
                // Strictly refuse fallback to male or default voice
                _isFemaleVoiceReady.value = false
                _selectedVoiceName.value = "Voice Disabled"
                _voiceStatusMessage.value = "No female American English (en-US) voice found. Please install Google Speech Recognition & Synthesis female voice data in Android settings."
                Log.w(TAG, "Strict female voice check failed: No female en-US voice matched. Kept voice disabled.")
            }
        } catch (e: Exception) {
            _isFemaleVoiceReady.value = false
            _selectedVoiceName.value = "Voice Disabled"
            _voiceStatusMessage.value = "Could not query available voices: ${e.message}"
            Log.w(TAG, "Could not query available voices: ${e.message}")
        }

        // 3. Set natural speaking rate and pitch
        tts.setSpeechRate(1.0f)
        tts.setPitch(1.05f)
    }

    private fun findStrictFemaleVoice(voices: List<Voice>): Voice? {
        if (voices.isEmpty()) return null

        val installedVoices = voices.filter { voice ->
            val features = voice.features ?: emptySet()
            !features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
        }.ifEmpty { voices }

        return installedVoices.firstOrNull { voice ->
            isStrictFemaleVoice(voice.name, voice.features ?: emptySet())
        }
    }

    private fun setupUtteranceListener() {
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
                _isSpeakingFlow.value = true
            }

            override fun onDone(utteranceId: String?) {
                handlePlaybackFinished(utteranceId)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                handlePlaybackFinished(utteranceId)
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.w(TAG, "TTS playback error $errorCode for utterance: $utteranceId")
                handlePlaybackFinished(utteranceId)
            }
        })
    }

    /**
     * Speaks the companion greeting upon character reveal.
     * Enforces debounce to prevent duplicate greetings from rapid double-taps.
     */
    fun speakGreeting(greetingText: String = "Hello! I'm right here beside you.") {
        val now = System.currentTimeMillis()
        if (now - lastGreetingTime < GREETING_DEBOUNCE_MS) {
            return
        }
        lastGreetingTime = now
        enqueueSpeech(greetingText)
    }

    /**
     * Enqueues an announcement (e.g. notification or call).
     */
    fun speakAnnouncement(text: String) {
        if (text.isNotBlank()) {
            enqueueSpeech(text)
        }
    }

    private fun enqueueSpeech(text: String, onDone: (() -> Unit)? = null) {
        if (!_isFemaleVoiceReady.value) {
            Log.w(TAG, "Speech discarded: female en-US voice is not configured or ready.")
            return
        }

        val item = QueueItem(
            utteranceId = UUID.randomUUID().toString(),
            text = text,
            onDone = onDone
        )

        synchronized(queueLock) {
            speechQueue.addLast(item)
            if (!isSpeaking && _isInitialized.value && _isFemaleVoiceReady.value) {
                playNextLocked()
            }
        }
    }

    private fun playNextLocked() {
        if (!_isFemaleVoiceReady.value) {
            speechQueue.clear()
            isSpeaking = false
            _isSpeakingFlow.value = false
            return
        }

        if (speechQueue.isEmpty()) {
            isSpeaking = false
            _isSpeakingFlow.value = false
            return
        }

        val item = speechQueue.peekFirst() ?: return
        isSpeaking = true
        _isSpeakingFlow.value = true

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        val res = textToSpeech?.speak(
            item.text,
            TextToSpeech.QUEUE_FLUSH,
            params,
            item.utteranceId
        )

        if (res != TextToSpeech.SUCCESS) {
            Log.e(TAG, "Failed to start speech for utterance: ${item.utteranceId}")
            handlePlaybackFinished(item.utteranceId)
        }
    }

    private fun handlePlaybackFinished(utteranceId: String?) {
        var completed: QueueItem? = null
        synchronized(queueLock) {
            if (speechQueue.isNotEmpty()) {
                val head = speechQueue.peekFirst()
                if (head != null && (utteranceId == null || head.utteranceId == utteranceId)) {
                    completed = speechQueue.pollFirst()
                }
            }
            playNextLocked()
        }

        completed?.onDone?.let { callback ->
            mainHandler.post { callback.invoke() }
        }
    }

    fun stop() {
        synchronized(queueLock) {
            speechQueue.clear()
            isSpeaking = false
            _isSpeakingFlow.value = false
        }
        textToSpeech?.stop()
    }

    fun shutdown() {
        stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }
}
