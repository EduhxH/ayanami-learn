package com.ayanami.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import org.bson.types.ObjectId

@Serializable
data class User(
    @SerialName("_id")
    val id: String = ObjectId().toHexString(),
    val firebaseUid: String,
    val fullName: String,
    val email: String,
    val nativeLanguage: String = "pt",
    val streak: Int = 0,
    val rankPoints: Int = 0,
    val aiPersonality: String = "Friendly"
)
