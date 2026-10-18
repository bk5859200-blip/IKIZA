package com.ikiza.companion.speech

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.ArrayDeque
import java.util.Locale
import java.util.UUID

/**
 * TextToSpeech wrapper ensuring strictly sequential playback via an internal FIFO queue.
 * Guarantees the companion never talks over herself.
 */
class SpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "IKIZA_SpeechManager"
    }

    private data class QueueItem(
        val utteranceId: String,
        val text: String,
        val onStart: (() -> Unit)?,
        val onDone: (() -> Unit)?
    )

    private val mainHandler = Handler(Looper.getMainLooper())
    private var textToSpeech: TextToSpeech? = null

    private val queueLock = Any()
    private val speechQueue = ArrayDeque<QueueItem>()
    private var isPlaying = false

    private val _speechState = MutableStateFlow(SpeechState())
    val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()

    // Configurable parameters
    var isMuted: Boolean = false
        set(value) {
            field = value
            if (value) {
                stopCurrentAndClear()
            }
            _speechState.update { it.copy(isMuted = value) }
        }

    var volume: Float = 0.85f
    var speechRate: Float = 1.05f
        set(value) {
            field = value
            textToSpeech?.setSpeechRate(value)
        }

    var speechPitch: Float = 1.25f
        set(value) {
            field = value
            textToSpeech?.setPitch(value)
        }

    init {
        textToSpeech = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to US English if default locale lacks speech synthesis data
                textToSpeech?.setLanguage(Locale.US)
            }
            textToSpeech?.setSpeechRate(speechRate)
            textToSpeech?.setPitch(speechPitch)
            setupUtteranceListener()

            _speechState.update { it.copy(isInitialized = true) }
            Log.d(TAG, "TextToSpeech successfully initialized")

            // Process any items enqueued before initialization completed
            synchronized(queueLock) {
                if (!isPlaying && speechQueue.isNotEmpty()) {
                    playNextLocked()
                }
            }
        } else {
            Log.e(TAG, "TextToSpeech initialization failed with status: $status")
        }
    }

    private fun setupUtteranceListener() {
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                mainHandler.post {
                    _speechState.update { it.copy(isSpeaking = true) }
                }
            }

            override fun onDone(utteranceId: String?) {
                mainHandler.post {
                    finishCurrentPlayback(utteranceId)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                mainHandler.post {
                    finishCurrentPlayback(utteranceId)
                }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.w(TAG, "TTS playback error code: $errorCode for utterance: $utteranceId")
                mainHandler.post {
                    finishCurrentPlayback(utteranceId)
                }
            }
        })
    }

    /**
     * Enqueues speech text. If nothing is currently speaking, begins playback immediately.
     * Otherwise waits in sequential FIFO order.
     */
    fun speak(
        text: String,
        onStart: (() -> Unit)? = null,
        onDone: (() -> Unit)? = null
    ) {
        if (isMuted || text.isBlank()) {
            return
        }

        val item = QueueItem(
            utteranceId = UUID.randomUUID().toString(),
            text = text,
            onStart = onStart,
            onDone = onDone
        )

        synchronized(queueLock) {
            speechQueue.addLast(item)
            _speechState.update { it.copy(queueSize = speechQueue.size) }

            if (!isPlaying && _speechState.value.isInitialized) {
                playNextLocked()
            }
        }
    }

    private fun playNextLocked() {
        if (isMuted || speechQueue.isEmpty()) {
            isPlaying = false
            _speechState.update {
                it.copy(
                    isSpeaking = false,
                    currentText = null,
                    queueSize = speechQueue.size
                )
            }
            return
        }

        val item = speechQueue.peekFirst() ?: return
        isPlaying = true

        _speechState.update {
            it.copy(
                isSpeaking = true,
                currentText = item.text,
                queueSize = speechQueue.size
            )
        }

        item.onStart?.invoke()

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume.coerceIn(0.0f, 1.0f))
        }

        val res = textToSpeech?.speak(
            item.text,
            TextToSpeech.QUEUE_FLUSH, // Queue within TTS is always flush because our manager handles FIFO
            params,
            item.utteranceId
        )

        if (res != TextToSpeech.SUCCESS) {
            Log.e(TAG, "TTS speak failed to queue utterance: ${item.utteranceId}")
            // Skip failed utterance and move to next
            finishCurrentPlayback(item.utteranceId)
        }
    }

    private fun finishCurrentPlayback(utteranceId: String?) {
        var completedItem: QueueItem? = null
        synchronized(queueLock) {
            if (speechQueue.isNotEmpty()) {
                val head = speechQueue.peekFirst()
                if (head != null && (utteranceId == null || head.utteranceId == utteranceId)) {
                    completedItem = speechQueue.pollFirst()
                }
            }
            playNextLocked()
        }

        completedItem?.onDone?.invoke()
    }

    /**
     * Stops any currently active speech and purges the pending FIFO queue.
     */
    fun stopCurrentAndClear() {
        synchronized(queueLock) {
            speechQueue.clear()
            isPlaying = false
        }
        textToSpeech?.stop()
        _speechState.update {
            it.copy(
                isSpeaking = false,
                currentText = null,
                queueSize = 0
            )
        }
    }

    fun shutdown() {
        stopCurrentAndClear()
        textToSpeech?.shutdown()
        textToSpeech = null
    }
}
