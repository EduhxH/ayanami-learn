package com.ayanami.models

import kotlinx.serialization.Serializable

@Serializable
data class EmailRegisterRequest(
    val fullName: String,
    val email: String,
    val password: String,
    val nativeLanguage: String = "pt"
)

@Serializable
data class EmailLoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val firebaseUid: String,
    val fullName: String,
    val email: String,
    val idToken: String,
    val refreshToken: String,
    val message: String
)
