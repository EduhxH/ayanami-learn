package com.ayanami.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

/**
 * Persistent learner progress stored in MongoDB. This is the source of truth for XP,
 * streak and completed lessons, replacing the previous in-memory-only client state.
 */
@Serializable
data class LearnerProgressRecord(
    @SerialName("_id")
    val id: String = ObjectId().toHexString(),
    val userId: String,
    val studiedLanguage: String = "English",
    val totalXp: Int = 0,
    val streakDays: Int = 0,
    val lastStudyDay: String? = null,
    val completedLessonIds: List<String> = emptyList(),
    val skillLevels: Map<String, Int> = emptyMap(),
    val hasPerfectLesson: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

/** Body sent by the app when a lesson is finished. */
@Serializable
data class LessonCompletionRequest(
    val lessonId: String,
    val skill: String,
    val earnedXp: Int,
    val correctCount: Int,
    val totalCount: Int
)
