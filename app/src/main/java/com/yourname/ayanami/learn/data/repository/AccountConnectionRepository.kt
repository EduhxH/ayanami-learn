package com.yourname.ayanami.learn.data.repository

import android.app.Activity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class AccountConnectionState(
    val googleConnected: Boolean = false,
    val githubConnected: Boolean = false,
    val firebaseSignedIn: Boolean = false
)

@Singleton
class AccountConnectionRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    private val _connectionState = MutableStateFlow(readState())
    val connectionState: StateFlow<AccountConnectionState> = _connectionState.asStateFlow()

    fun refresh() {
        _connectionState.value = readState()
    }

    suspend fun connectGoogle(idToken: String) {
        val user = firebaseAuth.currentUser ?: error("Sign in before connecting Google.")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        user.linkWithCredential(credential).await()
        refresh()
    }

    suspend fun disconnectGoogle() {
        val user = firebaseAuth.currentUser ?: error("Sign in before disconnecting Google.")
        requireProviderLinked(GOOGLE_PROVIDER)
        user.unlink(GOOGLE_PROVIDER).await()
        refresh()
    }

    suspend fun connectGithub(activity: Activity) {
        val user = firebaseAuth.currentUser ?: error("Sign in before connecting GitHub.")
        val provider = OAuthProvider.newBuilder(GITHUB_PROVIDER).build()
        user.startActivityForLinkWithProvider(activity, provider).await()
        refresh()
    }

    suspend fun disconnectGithub() {
        val user = firebaseAuth.currentUser ?: error("Sign in before disconnecting GitHub.")
        requireProviderLinked(GITHUB_PROVIDER)
        user.unlink(GITHUB_PROVIDER).await()
        refresh()
    }

    private fun requireProviderLinked(providerId: String) {
        val linked = firebaseAuth.currentUser
            ?.providerData
            ?.any { provider -> provider.providerId == providerId } == true
        if (!linked) error("Provider is not connected.")
    }

    private fun readState(): AccountConnectionState {
        val user = firebaseAuth.currentUser
        val providers = user?.providerData.orEmpty().map { it.providerId }.toSet()
        return AccountConnectionState(
            googleConnected = GOOGLE_PROVIDER in providers,
            githubConnected = GITHUB_PROVIDER in providers,
            firebaseSignedIn = user != null
        )
    }

    private companion object {
        const val GOOGLE_PROVIDER = "google.com"
        const val GITHUB_PROVIDER = "github.com"
    }
}
