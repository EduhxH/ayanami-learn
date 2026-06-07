package com.yourname.ayanami.learn.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatRequestDto(
    val message: String,
    val userId: String,
    val nativeLanguage: String = "pt"
)
