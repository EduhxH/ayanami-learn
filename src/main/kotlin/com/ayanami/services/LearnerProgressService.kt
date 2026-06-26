package com.ayanami.services

import com.ayanami.database.MongoManager
import com.ayanami.models.LearnerProgressRecord
import com.ayanami.models.LessonCompletionRequest
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import kotlinx.coroutines.flow.firstOrNull
import org.slf4j.LoggerFactory
import java.time.LocalDate

/**
 * Owns the real progression logic: loading a learner's saved progress and applying a
 * lesson completion (XP gain, streak update, completed-lesson tracking) to MongoDB.
 */
class LearnerProgressService {
    private val logger = LoggerFactory.getLogger(LearnerProgressService::class.java)
    private val collection =
        MongoManager.database.getCollection<LearnerProgressRecord>("learner_progress")

    suspend fun getProgress(userId: String): LearnerProgressRecord {
        return collection
            .find(Filters.eq(LearnerProgressRecord::userId.name, userId))
            .firstOrNull()
            ?: LearnerProgressRecord(userId = userId)
    }

    suspend fun recordCompletion(
        userId: String,
        request: LessonCompletionRequest
    ): LearnerProgressRecord {
        val current = getProgress(userId)
        val today = LocalDate.now().toString()
        val yesterday = LocalDate.now().minusDays(1).toString()

        val completedIds = (current.completedLessonIds + request.lessonId).distinct()
        val skillLevels = current.skillLevels.toMutableMap().apply {
            val key = request.skill.ifBlank { "general" }
            this[key] = (this[key] ?: 0) + 1
        }
        val totalXp = current.totalXp + request.earnedXp.coerceAtLeast(0)
        val streakDays = when (current.lastStudyDay) {
            today -> current.streakDays.coerceAtLeast(1)
            yesterday -> current.streakDays + 1
            else -> 1
        }
        val perfectLesson = current.hasPerfectLesson ||
            (request.totalCount > 0 && request.correctCount == request.totalCount)

        val updated = current.copy(
            totalXp = totalXp,
            streakDays = streakDays,
            lastStudyDay = today,
            completedLessonIds = completedIds,
            skillLevels = skillLevels,
            hasPerfectLesson = perfectLesson,
            updatedAt = System.currentTimeMillis()
        )

        collection.replaceOne(
            Filters.eq(LearnerProgressRecord::userId.name, userId),
            updated,
            ReplaceOptions().upsert(true)
        )
        logger.info(
            "Progress updated userId={} lesson={} xp={} streak={} completed={}",
            userId,
            request.lessonId,
            totalXp,
            streakDays,
            completedIds.size
        )
        return updated
    }
}
