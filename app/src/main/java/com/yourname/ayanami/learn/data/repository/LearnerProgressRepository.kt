package com.yourname.ayanami.learn.data.repository

import com.yourname.ayanami.learn.data.model.Achievement
import com.yourname.ayanami.learn.data.model.ExerciseSkill
import com.yourname.ayanami.learn.data.model.LearnerProgress
import com.yourname.ayanami.learn.data.model.LessonCompletionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LearnerProgressRepository @Inject constructor() {
    private val _progress = MutableStateFlow(
        LearnerProgress(
            achievements = buildAchievements(
                completedLessonCount = 0,
                totalXp = 0,
                hasPerfectLesson = false
            )
        )
    )
    val progress: StateFlow<LearnerProgress> = _progress.asStateFlow()

    fun recordLessonCompletion(result: LessonCompletionResult) {
        _progress.update { current ->
            val today = LocalDate.now().toString()
            val completedIds = current.completedLessonIds + result.lessonId
            val skillLevels = current.skillLevels + (
                result.skill to ((current.skillLevels[result.skill] ?: 0) + 1)
            )
            val totalXp = current.totalXp + result.earnedXp
            val streak = when (current.lastStudyDay) {
                today -> current.streakDays
                LocalDate.now().minusDays(1).toString() -> current.streakDays + 1
                null -> 1
                else -> 1
            }
            val perfectLesson = result.correctCount == result.totalCount && result.totalCount > 0

            current.copy(
                streakDays = streak,
                totalXp = totalXp,
                completedLessonIds = completedIds,
                skillLevels = skillLevels,
                lastStudyDay = today,
                achievements = buildAchievements(
                    completedLessonCount = completedIds.size,
                    totalXp = totalXp,
                    hasPerfectLesson = perfectLesson || current.achievements.any {
                        it.id == "perfect_lesson" && it.unlocked
                    }
                )
            )
        }
    }

    private fun buildAchievements(
        completedLessonCount: Int,
        totalXp: Int,
        hasPerfectLesson: Boolean
    ): List<Achievement> {
        return listOf(
            Achievement(
                id = "first_lesson",
                title = "First step",
                description = "Complete your first lesson.",
                unlocked = completedLessonCount >= 1
            ),
            Achievement(
                id = "xp_100",
                title = "100 XP",
                description = "Earn 100 total XP.",
                unlocked = totalXp >= 100
            ),
            Achievement(
                id = "perfect_lesson",
                title = "Perfect lesson",
                description = "Finish a lesson without mistakes.",
                unlocked = hasPerfectLesson
            ),
            Achievement(
                id = "four_lessons",
                title = "Balanced learner",
                description = "Complete four lessons.",
                unlocked = completedLessonCount >= 4
            )
        )
    }
}
