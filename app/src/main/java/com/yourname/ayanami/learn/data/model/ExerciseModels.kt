package com.yourname.ayanami.learn.data.model

import com.yourname.ayanami.learn.data.local.NativeLanguage

enum class ExerciseSkill(
    val key: String,
    val title: String,
    val subtitle: String
) {
    Reading(
        key = "reading",
        title = "Reading",
        subtitle = "Understand useful phrases"
    ),
    Listening(
        key = "listening",
        title = "Listening",
        subtitle = "Train your ear"
    ),
    Speaking(
        key = "speaking",
        title = "Speaking",
        subtitle = "Say short sentences clearly"
    ),
    Writing(
        key = "writing",
        title = "Writing",
        subtitle = "Build correct sentences"
    ),
    Daily(
        key = "daily",
        title = "Daily Plan",
        subtitle = "A balanced lesson for today"
    ),
    Practice(
        key = "practice",
        title = "Practice",
        subtitle = "Review your weak points"
    ),
    League(
        key = "league",
        title = "League Challenge",
        subtitle = "Score XP with mixed drills"
    );

    companion object {
        fun fromKey(key: String): ExerciseSkill {
            return entries.firstOrNull { it.key == key } ?: Reading
        }
    }

    fun localizedTitle(language: NativeLanguage): String {
        return when (language) {
            NativeLanguage.Portuguese -> when (this) {
                Reading -> "Leitura"
                Listening -> "Escuta"
                Speaking -> "Fala"
                Writing -> "Escrita"
                Daily -> "Plano diário"
                Practice -> "Prática"
                League -> "Desafio da liga"
            }
            NativeLanguage.Ukrainian -> when (this) {
                Reading -> "Читання"
                Listening -> "Аудіювання"
                Speaking -> "Мовлення"
                Writing -> "Письмо"
                Daily -> "Щоденний план"
                Practice -> "Практика"
                League -> "Виклик ліги"
            }
            NativeLanguage.Russian -> when (this) {
                Reading -> "Чтение"
                Listening -> "Аудирование"
                Speaking -> "Говорение"
                Writing -> "Письмо"
                Daily -> "Ежедневный план"
                Practice -> "Практика"
                League -> "Вызов лиги"
            }
        }
    }
}

/** One ordered step in the learning path, backed by a real [ExerciseLesson]. */
data class CurriculumLesson(
    val lessonId: String,
    val route: String,
    val skill: ExerciseSkill,
    val title: String
)

/** A group of ordered lessons shown as one section of the path. */
data class CurriculumUnit(
    val unitId: String,
    val title: String,
    val subtitle: String,
    val lessons: List<CurriculumLesson>
)

data class ExerciseLesson(
    val id: String,
    val unitTitle: String,
    val skill: ExerciseSkill,
    val title: String,
    val targetMinutes: Int,
    val xpReward: Int,
    val cefrLevel: String,
    val topic: String,
    val learningObjectives: List<String>,
    val grammarFocus: List<String>,
    val vocabulary: List<VocabularyEntry>,
    val sourceInfluences: List<ContentSourceRef>,
    val items: List<ExerciseItem>
)

data class VocabularyEntry(
    val word: String,
    val meaning: String,
    val example: String,
    val pronunciationHint: String? = null
)

data class ContentSourceRef(
    val provider: String,
    val url: String,
    val usage: SourceUsage,
    val notes: String
)

enum class SourceUsage {
    CurriculumReference,
    ApiCandidate,
    DatasetCandidate,
    LicensedDatasetCandidate
}

sealed class ExerciseItem {
    abstract val id: String
    abstract val prompt: String
    abstract val instruction: String

    data class MultipleChoice(
        override val id: String,
        override val prompt: String,
        override val instruction: String,
        val choices: List<String>,
        val answerIndex: Int
    ) : ExerciseItem()

    data class MatchingPairs(
        override val id: String,
        override val prompt: String,
        override val instruction: String,
        val pairs: List<MatchPair>
    ) : ExerciseItem()

    data class Writing(
        override val id: String,
        override val prompt: String,
        override val instruction: String,
        val acceptedAnswers: List<String>
    ) : ExerciseItem()

    data class Listening(
        override val id: String,
        override val prompt: String,
        override val instruction: String,
        val spokenText: String,
        val choices: List<String>,
        val answerIndex: Int
    ) : ExerciseItem()

    data class Speaking(
        override val id: String,
        override val prompt: String,
        override val instruction: String,
        val targetPhrase: String,
        val acceptedPhrases: List<String>
    ) : ExerciseItem()
}

data class MatchPair(
    val left: String,
    val right: String
)
