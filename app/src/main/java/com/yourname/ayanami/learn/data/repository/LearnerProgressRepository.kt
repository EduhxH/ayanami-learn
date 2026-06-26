package com.yourname.ayanami.learn.data.repository

import android.content.Context
import androidx.core.content.edit
import com.yourname.ayanami.learn.data.local.UserPreferencesRepository
import com.yourname.ayanami.learn.data.model.Achievement
import com.yourname.ayanami.learn.data.model.ExerciseSkill
import com.yourname.ayanami.learn.data.model.LearnerProgress
import com.yourname.ayanami.learn.data.model.LearnerProgressDto
import com.yourname.ayanami.learn.data.model.LessonCompletionRequestDto
import com.yourname.ayanami.learn.data.model.LessonCompletionResult
import com.yourname.ayanami.learn.data.remote.ProgressApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real, persisted learner progress. The source of truth is the backend (MongoDB) via
 * [ProgressApiService]; a local cache keeps the last known progress for instant/offline
 * display. When there is no signed-in user or the network fails, progress is still applied
 * locally so the app keeps working.
 */
@Singleton
class LearnerProgressRepository @Inject constructor(
    private val progressApiService: ProgressApiService,
    private val userPreferencesRepository: UserPreferencesRepository,
    @ApplicationContext context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cache = context.getSharedPreferences(CACHE_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _progress = MutableStateFlow(readCache().toDomain())
    val progress: StateFlow<LearnerProgress> = _progress.asStateFlow()

    init {
        scope.launch {
            userPreferencesRepository.preferences
                .map { it.firebaseUid ?: it.email }
                .distinctUntilChanged()
                .collect { userId ->
                    if (!userId.isNullOrBlank()) refreshFromBackend(userId)
                }
        }
    }

    fun recordLessonCompletion(result: LessonCompletionResult) {
        val userId = currentUserId()
        if (userId.isNullOrBlank()) {
            applyLocally(result)
            return
        }
        scope.launch {
            runCatching {
                progressApiService.recordCompletion(userId, result.toRequestDto())
            }.onSuccess { dto -> publish(dto) }
                .onFailure { applyLocally(result) }
        }
    }

    private suspend fun refreshFromBackend(userId: String) {
        runCatching { progressApiService.getProgress(userId) }
            .onSuccess { dto -> publish(dto) }
    }

    /** Local fallback that mirrors the backend streak/XP logic when offline or signed out. */
    private fun applyLocally(result: LessonCompletionResult) {
        val current = readCache()
        val today = LocalDate.now().toString()
        val yesterday = LocalDate.now().minusDays(1).toString()
        val completed = (current.completedLessonIds + result.lessonId).distinct()
        val skillLevels = current.skillLevels.toMutableMap().apply {
            this[result.skill.key] = (this[result.skill.key] ?: 0) + 1
        }
        val updated = current.copy(
            totalXp = current.totalXp + result.earnedXp.coerceAtLeast(0),
            streakDays = when (current.lastStudyDay) {
                today -> current.streakDays.coerceAtLeast(1)
                yesterday -> current.streakDays + 1
                else -> 1
            },
            lastStudyDay = today,
            completedLessonIds = completed,
            skillLevels = skillLevels,
            hasPerfectLesson = current.hasPerfectLesson ||
                (result.totalCount > 0 && result.correctCount == result.totalCount)
        )
        publish(updated)
    }

    private fun publish(dto: LearnerProgressDto) {
        writeCache(dto)
        _progress.value = dto.toDomain()
    }

    private fun currentUserId(): String? {
        val preferences = userPreferencesRepository.preferences.value
        return preferences.firebaseUid ?: preferences.email
    }

    private fun readCache(): LearnerProgressDto {
        val raw = cache.getString(KEY_PROGRESS, null) ?: return LearnerProgressDto()
        return runCatching { json.decodeFromString(LearnerProgressDto.serializer(), raw) }
            .getOrDefault(LearnerProgressDto())
    }

    private fun writeCache(dto: LearnerProgressDto) {
        cache.edit { putString(KEY_PROGRESS, json.encodeToString(LearnerProgressDto.serializer(), dto)) }
    }

    private fun LessonCompletionResult.toRequestDto() = LessonCompletionRequestDto(
        lessonId = lessonId,
        skill = skill.key,
        earnedXp = earnedXp,
        correctCount = correctCount,
        totalCount = totalCount
    )

    private fun LearnerProgressDto.toDomain(): LearnerProgress {
        return LearnerProgress(
            streakDays = streakDays,
            totalXp = totalXp,
            completedLessonIds = completedLessonIds.toSet(),
            skillLevels = skillLevels.mapKeys { (key, _) -> ExerciseSkill.fromKey(key) },
            lastStudyDay = lastStudyDay,
            achievements = buildAchievements(
                completedLessonCount = completedLessonIds.size,
                totalXp = totalXp,
                hasPerfectLesson = hasPerfectLesson
            )
        )
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

    private companion object {
        const val CACHE_NAME = "ayanami_progress_cache"
        const val KEY_PROGRESS = "progress_json"
    }
}
