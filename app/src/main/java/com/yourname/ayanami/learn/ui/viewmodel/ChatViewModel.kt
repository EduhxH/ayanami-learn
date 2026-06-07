package com.yourname.ayanami.learn.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.ayanami.learn.data.local.UserPreferencesRepository
import com.yourname.ayanami.learn.data.local.UserPreferences
import com.yourname.ayanami.learn.data.model.ChatRequestDto
import com.yourname.ayanami.learn.data.remote.ChatApiService
import com.yourname.ayanami.learn.ui.localization.appStrings
import com.yourname.ayanami.learn.utils.AudioPlayer
import com.yourname.ayanami.learn.utils.AudioRecorder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ChatMessage(
    val role: String,
    val content: String,
    val audioReplyUrl: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    application: Application,
    private val chatApiService: ChatApiService,
    private val userPreferencesRepository: UserPreferencesRepository
) : AndroidViewModel(application) {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    val preferences: StateFlow<UserPreferences> = userPreferencesRepository.preferences

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val audioRecorder = AudioRecorder(application.applicationContext)
    private val audioPlayer = AudioPlayer()
    private var currentRecordingFile: File? = null

    fun sendTextMessage(text: String) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return
        val session = currentSessionOrReport() ?: return

        _messages.value = _messages.value + ChatMessage(role = "user", content = cleanText)
        viewModelScope.launch {
            runCatching {
                val request = ChatRequestDto(
                    message = cleanText,
                    userId = session.userId,
                    nativeLanguage = session.nativeLanguageCode
                )
                chatApiService.sendTextMessage(request)
            }.onSuccess { response ->
                _messages.value = _messages.value + ChatMessage(
                    role = "assistant",
                    content = response.reply,
                    audioReplyUrl = response.audioReplyUrl
                )
                response.audioReplyUrl?.let(audioPlayer::playAudioFromUrl)
            }.onFailure { error ->
                _messages.value = _messages.value + ChatMessage(
                    role = "assistant",
                    content = "Error: ${error.localizedMessage ?: "Could not reach the tutor."}"
                )
            }
        }
    }

    fun startRecording() {
        if (currentSessionOrReport() == null) return
        currentRecordingFile = audioRecorder.startRecording()
        if (currentRecordingFile != null) {
            _isRecording.value = true
        }
    }

    fun stopRecording() {
        val recordedFile = audioRecorder.stopRecording()
        _isRecording.value = false
        val session = currentSessionOrReport()

        if (recordedFile == null) {
            _messages.value = _messages.value + ChatMessage(
                role = "assistant",
                content = "Voice error: no audio was recorded."
            )
            return
        }

        if (session == null) {
            recordedFile.delete()
            return
        }

        val voicePlaceholder = ChatMessage(role = "user", content = "(Transcribing voice...)")
        _messages.value = _messages.value + voicePlaceholder
        viewModelScope.launch {
            runCatching {
                chatApiService.sendVoiceMessage(
                    audioFile = recordedFile,
                    userId = session.userId,
                    nativeLanguage = session.nativeLanguageCode
                )
            }.onSuccess { response ->
                val voiceMessage = response.transcript
                    ?.takeIf { it.isNotBlank() }
                    ?.let { transcript -> ChatMessage(role = "user", content = "Voice: $transcript") }
                    ?: ChatMessage(role = "user", content = "Voice: audio not clear")

                _messages.value = _messages.value.dropLast(1) + voiceMessage
                _messages.value = _messages.value + ChatMessage(
                    role = "assistant",
                    content = response.reply,
                    audioReplyUrl = response.audioReplyUrl
                )
                response.audioReplyUrl?.let(audioPlayer::playAudioFromUrl)
            }.onFailure { error ->
                _messages.value = _messages.value.dropLast(1) + ChatMessage(
                    role = "user",
                    content = "Voice: upload failed"
                )
                _messages.value = _messages.value + ChatMessage(
                    role = "assistant",
                    content = "Voice error: ${error.localizedMessage ?: "Could not process audio."}"
                )
            }

            recordedFile.delete()
        }
    }

    fun toggleDarkTheme() {
        val current = userPreferencesRepository.preferences.value.darkTheme
        userPreferencesRepository.updateDarkTheme(!current)
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.cancelRecording()
        audioPlayer.stopPlaying()
    }

    private fun currentSessionOrReport(): ChatSession? {
        val preferences = userPreferencesRepository.preferences.value
        val userId = preferences.firebaseUid ?: preferences.email
        if (userId.isNullOrBlank()) {
            _messages.value = _messages.value + ChatMessage(
                role = "assistant",
                content = preferences.nativeLanguage.appStrings().sessionRequired
            )
            return null
        }
        return ChatSession(
            userId = userId,
            nativeLanguageCode = preferences.nativeLanguage.code
        )
    }
}

private data class ChatSession(
    val userId: String,
    val nativeLanguageCode: String
)
