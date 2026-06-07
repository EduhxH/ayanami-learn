package com.ayanami.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class ChatMessageRecord(
    @SerialName("_id")
    val id: String = ObjectId().toHexString(),
    val userId: String,
    val role: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class UserMemory(
    @SerialName("_id")
    val id: String = ObjectId().toHexString(),
    val userId: String,
    val key: String,
    val value: String,
    val category: String = "general",
    val source: String = "chat",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
