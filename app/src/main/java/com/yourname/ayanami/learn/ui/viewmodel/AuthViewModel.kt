package com.yourname.ayanami.learn.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.yourname.ayanami.learn.data.local.NativeLanguage
import com.yourname.ayanami.learn.data.local.UserPreferences
import com.yourname.ayanami.learn.data.local.UserPreferencesRepository
import com.yourname.ayanami.learn.data.repository.AuthRepository
import com.yourname.ayanami.learn.ui.localization.appStrings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AuthState {
    data object Idle : AuthState
    data object Loading : AuthState
    data class Success(val message: String) : AuthState
    data class Error(val message: String) : AuthState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val googleSignInClient: GoogleSignInClient,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    val preferences: StateFlow<UserPreferences> = userPreferencesRepository.preferences

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    fun getGoogleSignInClient(): GoogleSignInClient = googleSignInClient

    fun signInWithGoogle(idToken: String) {
        runAuthAction {
            authRepository.signInWithGoogle(idToken)
        }
    }

    fun signInWithEmail(email: String, password: String) {
        runAuthAction {
            authRepository.signInWithEmail(email.trim(), password)
        }
    }

    fun createAccount(
        fullName: String,
        email: String,
        password: String,
        nativeLanguage: String
    ) {
        runAuthAction {
            authRepository.createAccount(
                fullName = fullName.trim(),
                email = email.trim(),
                password = password,
                nativeLanguage = nativeLanguage
            )
        }
    }

    fun updateNativeLanguage(language: NativeLanguage) {
        userPreferencesRepository.updateNativeLanguage(language)
    }

    fun sendPasswordResetEmail(email: String) {
        val cleanEmail = email.trim()
        val strings = currentStrings()
        if (cleanEmail.isBlank()) {
            _notice.value = strings.authPasswordResetEmailRequired
            return
        }
        viewModelScope.launch {
            runCatching { authRepository.sendPasswordResetEmail(cleanEmail) }
                .onSuccess {
                    _notice.value = currentStrings().authPasswordResetEmailSent(cleanEmail)
                }
                .onFailure { error ->
                    _notice.value = error.localizedMessage ?: currentStrings().authPasswordResetFailed
                }
        }
    }

    fun clearNotice() {
        _notice.value = null
    }

    fun clearState() {
        _authState.value = AuthState.Idle
    }

    private fun runAuthAction(action: suspend () -> String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            runCatching { action() }
                .onSuccess { message -> _authState.value = AuthState.Success(message) }
                .onFailure { error ->
                    _authState.value = AuthState.Error(error.localizedMessage ?: currentStrings().authFailed)
                }
        }
    }

    private fun currentStrings() = userPreferencesRepository.preferences.value.nativeLanguage.appStrings()
}
