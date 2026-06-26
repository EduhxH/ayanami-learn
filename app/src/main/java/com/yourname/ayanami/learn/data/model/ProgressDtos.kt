package com.yourname.ayanami.learn.data.model

import kotlinx.serialization.Serializable

/** Mirrors the backend LearnerProgressRecord (extra fields are ignored on decode). */
@Serializable
data class LearnerProgressDto(
    val totalXp: Int = 0,
    val streakDays: Int = 0,
    val lastStudyDay: String? = null,
    val completedLessonIds: List<String> = emptyList(),
    val skillLevels: Map<String, Int> = emptyMap(),
    val hasPerfectLesson: Boolean = false
)

@Serializable
data class LessonCompletionRequestDto(
    val lessonId: String,
    val skill: String,
    val earnedXp: Int,
    val correctCount: Int,
    val totalCount: Int
)
