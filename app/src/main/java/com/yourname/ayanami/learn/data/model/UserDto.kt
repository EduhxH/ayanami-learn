package com.yourname.ayanami.learn.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val firebaseUid: String,
    val fullName: String,
    val email: String,
    val nativeLanguage: String = "pt"
)
