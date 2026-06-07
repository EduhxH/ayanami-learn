package com.yourname.ayanami.learn.ui.viewmodel

import android.app.Activity
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuth
import com.yourname.ayanami.learn.data.local.ActivityStatus
import com.yourname.ayanami.learn.data.local.NativeLanguage
import com.yourname.ayanami.learn.data.local.UserPreferences
import com.yourname.ayanami.learn.data.local.UserPreferencesRepository
import com.yourname.ayanami.learn.data.repository.AccountConnectionRepository
import com.yourname.ayanami.learn.data.repository.AccountConnectionState
import com.yourname.ayanami.learn.ui.localization.appStrings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val accountConnectionRepository: AccountConnectionRepository,
    private val googleSignInClient: GoogleSignInClient,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {
    val preferences: StateFlow<UserPreferences> = userPreferencesRepository.preferences
    val connections: StateFlow<AccountConnectionState> = accountConnectionRepository.connectionState

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        accountConnectionRepository.refresh()
    }

    fun getGoogleSignInClient(): GoogleSignInClient = googleSignInClient

    fun clearMessage() {
        _message.value = null
    }

    fun updateProfile(name: String, bio: String, photoUri: Uri?) {
        userPreferencesRepository.updateProfile(name, bio, photoUri)
        _message.value = currentStrings().profileSaved
    }

    fun updateNativeLanguage(language: NativeLanguage) {
        userPreferencesRepository.updateNativeLanguage(language)
    }

    fun updateActivityStatus(status: ActivityStatus, remember: Boolean) {
        userPreferencesRepository.updateActivityStatus(status, remember)
    }

    fun updateDailyGoal(minutes: Int) {
        userPreferencesRepository.updateDailyGoal(minutes)
    }

    fun updateVoiceReplies(enabled: Boolean) {
        userPreferencesRepository.updateVoiceReplies(enabled)
    }

    fun updateSpeakingExercises(enabled: Boolean) {
        userPreferencesRepository.updateSpeakingExercises(enabled)
    }

    fun updateSoundEffects(enabled: Boolean) {
        userPreferencesRepository.updateSoundEffects(enabled)
    }

    fun updateStudyReminders(enabled: Boolean) {
        userPreferencesRepository.updateStudyReminders(enabled)
    }

    fun updateDarkTheme(enabled: Boolean) {
        userPreferencesRepository.updateDarkTheme(enabled)
    }

    fun connectGoogle(idToken: String) {
        runAccountAction {
            accountConnectionRepository.connectGoogle(idToken)
            currentStrings().googleConnectedMessage
        }
    }

    fun disconnectGoogle() {
        runAccountAction {
            accountConnectionRepository.disconnectGoogle()
            googleSignInClient.signOut().await()
            currentStrings().googleDisconnectedMessage
        }
    }

    fun connectGithub(activity: Activity) {
        runAccountAction {
            accountConnectionRepository.connectGithub(activity)
            currentStrings().githubConnectedMessage
        }
    }

    fun disconnectGithub() {
        runAccountAction {
            accountConnectionRepository.disconnectGithub()
            currentStrings().githubDisconnectedMessage
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            firebaseAuth.signOut()
            googleSignInClient.signOut().await()
            accountConnectionRepository.refresh()
            onLoggedOut()
        }
    }

    private fun runAccountAction(action: suspend () -> String) {
        viewModelScope.launch {
            runCatching { action() }
                .onSuccess { message -> _message.value = message }
                .onFailure { error ->
                    accountConnectionRepository.refresh()
                    _message.value = error.localizedMessage ?: currentStrings().accountActionFailed
                }
        }
    }

    private fun currentStrings() = userPreferencesRepository.preferences.value.nativeLanguage.appStrings()
}
