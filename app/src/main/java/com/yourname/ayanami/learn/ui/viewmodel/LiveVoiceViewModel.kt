package com.yourname.ayanami.learn.ui.viewmodel

import android.app.Application
import android.util.Log
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.ayanami.learn.data.local.UserPreferencesRepository
import com.yourname.ayanami.learn.data.remote.LiveVoiceApiService
import com.yourname.ayanami.learn.data.remote.LiveVoiceEvent
import com.yourname.ayanami.learn.data.remote.LiveVoiceOutbound
import com.yourname.ayanami.learn.ui.localization.appStrings
import com.yourname.ayanami.learn.utils.LiveAudioStreamer
import com.yourname.ayanami.learn.utils.PcmAudioPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.sqrt
import javax.inject.Inject

data class LiveVoiceUiState(
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val status: String = "Ready",
    val userTranscript: String = "",
    val assistantTranscript: String = "",
    val error: String? = null,
    val micLevel: Float = 0f,
    val audioChunksSent: Int = 0,
    val lastEvent: String = ""
)

@HiltViewModel
class LiveVoiceViewModel @Inject constructor(
    application: Application,
    private val liveVoiceApiService: LiveVoiceApiService,
    private val userPreferencesRepository: UserPreferencesRepository
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(LiveVoiceUiState())
    val uiState: StateFlow<LiveVoiceUiState> = _uiState.asStateFlow()

    private val audioStreamer = LiveAudioStreamer(application.applicationContext)
    private val audioPlayer = PcmAudioPlayer()
    private var sessionJob: Job? = null
    private var outgoing = Channel<LiveVoiceOutbound>(Channel.BUFFERED)
    private var pendingListenStart = false
    private var activityStarted = false
    private var sentAudioChunks = 0
    private var receivedAudioChunks = 0

    fun toggleListening() {
        if (_uiState.value.isListening) {
            stopListening()
        } else {
            startListening()
        }
    }

    fun onMicrophonePermissionDenied() {
        pendingListenStart = false
        _uiState.update {
            it.copy(
                status = "Microphone permission denied",
                error = "Allow microphone access to use live voice.",
                lastEvent = "Permission denied"
            )
        }
    }

    fun startListening() {
        if (_uiState.value.isListening) return
        val session = currentSessionOrReport() ?: return

        pendingListenStart = true
        ensureConnected(session)
        if (!_uiState.value.isConnected) {
            _uiState.update {
                it.copy(
                    status = "Connecting to voice session",
                    error = null,
                    lastEvent = "Waiting for Gemini Live"
                )
            }
            return
        }
        beginCapture()
    }

    private fun beginCapture() {
        if (_uiState.value.isListening) return

        pendingListenStart = false
        sentAudioChunks = 0
        // Manual activity detection: tell Gemini Live a user turn is starting BEFORE any
        // audio is queued, so the boundary is explicit (no silence-timer guessing).
        outgoing.trySend(LiveVoiceOutbound.ActivityStart)
        activityStarted = true
        val started = audioStreamer.start(
            onPcmChunk = { pcm ->
                val base64 = Base64.encodeToString(pcm, Base64.NO_WRAP)
                val result = outgoing.trySend(LiveVoiceOutbound.Audio(base64))
                if (result.isSuccess) {
                    sentAudioChunks += 1
                    if (sentAudioChunks == 1 || sentAudioChunks % 8 == 0) {
                        val level = pcm.calculatePcmLevel()
                        _uiState.update {
                            it.copy(
                                audioChunksSent = sentAudioChunks,
                                micLevel = level,
                                status = if (level < QUIET_LEVEL) "Listening - very low mic signal" else "Listening",
                                lastEvent = "Audio sent to backend"
                            )
                        }
                    }
                } else {
                    Log.w(TAG, "Could not send microphone chunk to WebSocket.", result.exceptionOrNull())
                    _uiState.update {
                        it.copy(
                            error = "Could not send microphone audio to the voice session.",
                            lastEvent = "Audio send failed"
                        )
                    }
                }
            },
            onError = { message ->
                Log.w(TAG, message)
                _uiState.update {
                    it.copy(
                        isListening = false,
                        status = "Microphone error",
                        error = message,
                        lastEvent = "Microphone read failed"
                    )
                }
            }
        )
        if (!started) {
            // Microphone never opened, so close the turn we just announced.
            endActivityIfNeeded()
        }
        _uiState.update {
            it.copy(
                isListening = started,
                status = if (started) "Listening" else "Microphone unavailable",
                error = if (started) null else "Could not open microphone.",
                micLevel = 0f,
                audioChunksSent = 0,
                lastEvent = if (started) "Microphone opened" else "Microphone unavailable"
            )
        }
    }

    /** Closes the current Gemini Live user turn so it starts generating the reply immediately. */
    private fun endActivityIfNeeded() {
        if (activityStarted) {
            if (_uiState.value.isConnected) outgoing.trySend(LiveVoiceOutbound.ActivityEnd)
            activityStarted = false
        }
    }

    fun stopListening() {
        val hadAudio = sentAudioChunks > 0
        audioStreamer.stop()
        endActivityIfNeeded()
        _uiState.update {
            it.copy(
                isListening = false,
                micLevel = 0f,
                status = when {
                    it.isSpeaking -> "Rei is speaking"
                    hadAudio -> "Processing voice"
                    else -> "No microphone audio captured"
                },
                error = if (hadAudio) null else "The microphone opened, but no audio chunks were captured.",
                lastEvent = if (hadAudio) "Audio stream ended" else "No audio chunks"
            )
        }
    }

    fun interruptPlayback() {
        audioPlayer.stop()
        _uiState.update { it.copy(isSpeaking = false, status = "Listening", lastEvent = "Playback interrupted") }
    }

    fun endSession() {
        pendingListenStart = false
        activityStarted = false
        stopListening()
        outgoing.trySend(LiveVoiceOutbound.Stop)
        sessionJob?.cancel()
        sessionJob = null
        outgoing.close()
        outgoing = Channel(Channel.BUFFERED)
        audioPlayer.stop()
        _uiState.value = LiveVoiceUiState(status = "Ready")
    }

    private fun ensureConnected(session: LiveVoiceSession) {
        if (sessionJob?.isActive == true) return

        outgoing = Channel(Channel.BUFFERED)
        _uiState.update {
            it.copy(
                isConnecting = true,
                status = "Connecting",
                error = null,
                lastEvent = "Opening voice WebSocket"
            )
        }
        sessionJob = viewModelScope.launch {
            liveVoiceApiService.runSession(
                userId = session.userId,
                nativeLanguage = session.nativeLanguageCode,
                outgoing = outgoing,
                onEvent = ::handleEvent
            )
        }
    }

    private fun currentSessionOrReport(): LiveVoiceSession? {
        val preferences = userPreferencesRepository.preferences.value
        val userId = preferences.firebaseUid ?: preferences.email
        if (userId.isNullOrBlank()) {
            pendingListenStart = false
            _uiState.update {
                it.copy(
                    isConnecting = false,
                    isListening = false,
                    status = "Session required",
                    error = preferences.nativeLanguage.appStrings().sessionRequired,
                    lastEvent = "No authenticated user"
                )
            }
            return null
        }
        return LiveVoiceSession(
            userId = userId,
            nativeLanguageCode = preferences.nativeLanguage.code
        )
    }

    private fun handleEvent(event: LiveVoiceEvent) {
        logVoiceEvent(event)
        when (event) {
            LiveVoiceEvent.Ready -> {
                _uiState.update {
                    it.copy(
                        isConnected = true,
                        isConnecting = false,
                        status = "Connected",
                        error = null,
                        lastEvent = "Gemini Live ready"
                    )
                }
                if (pendingListenStart) beginCapture()
            }
            is LiveVoiceEvent.Audio -> {
                receivedAudioChunks += 1
                if (_uiState.value.isListening) {
                    audioStreamer.stop()
                    endActivityIfNeeded()
                    _uiState.update {
                        it.copy(
                            isListening = false,
                            micLevel = 0f,
                            lastEvent = "Microphone paused for playback"
                        )
                    }
                }
                _uiState.update {
                    it.copy(isSpeaking = true, status = "Rei is speaking", lastEvent = "Audio received")
                }
                val pcm = Base64.decode(event.base64Pcm, Base64.DEFAULT)
                audioPlayer.playChunk(pcm, event.sampleRate)
            }
            is LiveVoiceEvent.InputTranscript -> {
                _uiState.update { it.copy(userTranscript = event.text, lastEvent = "Input transcript received") }
            }
            is LiveVoiceEvent.OutputTranscript -> {
                _uiState.update {
                    it.copy(
                        assistantTranscript = it.assistantTranscript + event.text,
                        lastEvent = "Output transcript received"
                    )
                }
            }
            LiveVoiceEvent.TurnComplete -> {
                audioPlayer.finishTurn()
                _uiState.update { it.copy(isSpeaking = false, status = "Connected", lastEvent = "Turn complete") }
            }
            LiveVoiceEvent.Interrupted -> {
                audioPlayer.stop()
                _uiState.update { it.copy(isSpeaking = false, status = "Interrupted", lastEvent = "Interrupted") }
            }
            is LiveVoiceEvent.Error -> {
                pendingListenStart = false
                audioStreamer.stop()
                Log.e(TAG, "Voice error: ${event.message}")
                _uiState.update {
                    it.copy(
                        isConnecting = false,
                        isListening = false,
                        status = "Error",
                        error = event.message,
                        lastEvent = "Voice error"
                    )
                }
            }
            LiveVoiceEvent.Closed -> {
                pendingListenStart = false
                audioStreamer.stop()
                _uiState.update {
                    it.copy(
                        isConnected = false,
                        isConnecting = false,
                        isListening = false,
                        isSpeaking = false,
                        micLevel = 0f,
                        status = if (it.error == null) "Closed" else it.status,
                        lastEvent = "Voice session closed"
                    )
                }
            }
        }
    }

    private fun ByteArray.calculatePcmLevel(): Float {
        var sum = 0.0
        var samples = 0
        var index = 0
        while (index + 1 < size) {
            val value = (this[index].toInt() and 0xff) or (this[index + 1].toInt() shl 8)
            val signed = if (value > Short.MAX_VALUE) value - 65_536 else value
            sum += signed.toDouble() * signed.toDouble()
            samples += 1
            index += 2
        }
        if (samples == 0) return 0f
        return (sqrt(sum / samples) / Short.MAX_VALUE).toFloat().coerceIn(0f, 1f)
    }

    private fun logVoiceEvent(event: LiveVoiceEvent) {
        if (event is LiveVoiceEvent.Audio) {
            if (receivedAudioChunks == 0 || receivedAudioChunks % 25 == 0) {
                Log.d(TAG, "Voice audio event count=${receivedAudioChunks + 1}")
            }
        } else {
            Log.d(TAG, "Voice event: ${event.javaClass.simpleName}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        endSession()
    }

    private companion object {
        const val TAG = "AyanamiVoice"
        const val QUIET_LEVEL = 0.015f
    }
}

private data class LiveVoiceSession(
    val userId: String,
    val nativeLanguageCode: String
)
