package com.yourname.ayanami.learn.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.yourname.ayanami.learn.data.local.NativeLanguage
import com.yourname.ayanami.learn.data.local.UserPreferencesRepository
import com.yourname.ayanami.learn.data.model.UserDto
import com.yourname.ayanami.learn.data.remote.AuthApiService
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val authApiService: AuthApiService,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend fun signInWithGoogle(idToken: String): String {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = firebaseAuth.signInWithCredential(credential).await()
        val firebaseUser = result.user ?: error("Firebase user is null.")
        userPreferencesRepository.setAuthenticatedUser(
            firebaseUid = firebaseUser.uid,
            fullName = firebaseUser.displayName.orEmpty(),
            email = firebaseUser.email.orEmpty()
        )
        return registerCurrentUser(
            firebaseUid = firebaseUser.uid,
            fullName = firebaseUser.displayName.orEmpty(),
            email = firebaseUser.email.orEmpty()
        )
    }

    suspend fun signInWithEmail(email: String, password: String): String {
        val authResponse = authApiService.loginWithEmail(
            email = email,
            password = password
        )
        firebaseAuth.signInWithEmailAndPassword(email, password).await()
        userPreferencesRepository.setAuthenticatedUser(
            firebaseUid = authResponse.firebaseUid,
            fullName = authResponse.fullName,
            email = authResponse.email
        )
        return authResponse.message
    }

    suspend fun sendPasswordResetEmail(email: String) {
        firebaseAuth.sendPasswordResetEmail(email.trim()).await()
    }

    suspend fun createAccount(
        fullName: String,
        email: String,
        password: String,
        nativeLanguage: String
    ): String {
        userPreferencesRepository.updateNativeLanguage(NativeLanguage.fromCode(nativeLanguage))
        val authResponse = authApiService.registerWithEmail(
            fullName = fullName,
            email = email,
            password = password,
            nativeLanguage = nativeLanguage
        )
        firebaseAuth.signInWithEmailAndPassword(email, password).await()
        userPreferencesRepository.setAuthenticatedUser(
            firebaseUid = authResponse.firebaseUid,
            fullName = authResponse.fullName,
            email = authResponse.email
        )
        return authResponse.message
    }

    private suspend fun registerCurrentUser(
        firebaseUid: String,
        fullName: String,
        email: String
    ): String {
        val firebaseToken = firebaseAuth.currentUser
            ?.getIdToken(true)
            ?.await()
            ?.token
            ?: error("Firebase token is null.")

        val message = authApiService.authenticateWithFirebase(
            firebaseToken = firebaseToken,
            userDto = UserDto(
                firebaseUid = firebaseUid,
                fullName = fullName,
                email = email,
                nativeLanguage = userPreferencesRepository.preferences.value.nativeLanguage.code
            )
        )
        userPreferencesRepository.setAuthenticatedUser(
            firebaseUid = firebaseUid,
            fullName = fullName,
            email = email
        )
        return message
    }
}
