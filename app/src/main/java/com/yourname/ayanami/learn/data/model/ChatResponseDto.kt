package com.yourname.ayanami.learn.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatResponseDto(
    val reply: String,
    val audioReplyUrl: String? = null,
    val transcript: String? = null
)
