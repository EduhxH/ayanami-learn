package com.yourname.ayanami.learn.data.model

data class LearnerProgress(
    val studiedLanguage: String = "English",
    val streakDays: Int = 0,
    val totalXp: Int = 0,
    val completedLessonIds: Set<String> = emptySet(),
    val skillLevels: Map<ExerciseSkill, Int> = emptyMap(),
    val achievements: List<Achievement> = emptyList(),
    val lastStudyDay: String? = null
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val unlocked: Boolean
)

data class LessonCompletionResult(
    val lessonId: String,
    val skill: ExerciseSkill,
    val earnedXp: Int,
    val correctCount: Int,
    val totalCount: Int,
    val completedAtMillis: Long = System.currentTimeMillis()
)
