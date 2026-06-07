package com.yourname.ayanami.learn.data.model

import kotlinx.serialization.Serializable

@Serializable
data class EmailRegisterRequestDto(
    val fullName: String,
    val email: String,
    val password: String,
    val nativeLanguage: String = "pt"
)

@Serializable
data class EmailLoginRequestDto(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponseDto(
    val firebaseUid: String,
    val fullName: String,
    val email: String,
    val idToken: String,
    val refreshToken: String,
    val message: String
)
