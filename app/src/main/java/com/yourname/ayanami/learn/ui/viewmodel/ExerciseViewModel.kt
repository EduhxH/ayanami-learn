package com.yourname.ayanami.learn.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.ayanami.learn.data.local.NativeLanguage
import com.yourname.ayanami.learn.data.local.UserPreferencesRepository
import com.yourname.ayanami.learn.data.model.ExerciseItem
import com.yourname.ayanami.learn.data.model.ExerciseLesson
import com.yourname.ayanami.learn.data.model.LessonCompletionResult
import com.yourname.ayanami.learn.data.repository.ExerciseRepository
import com.yourname.ayanami.learn.data.repository.LearnerProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExerciseUiState(
    val lesson: ExerciseLesson? = null,
    val currentIndex: Int = 0,
    val selectedChoiceIndex: Int? = null,
    val selectedMatchLeft: String? = null,
    val matchedPairs: Map<String, String> = emptyMap(),
    val writingAnswer: String = "",
    val speechTranscript: String = "",
    val checked: Boolean = false,
    val isCorrect: Boolean? = null,
    val correctExerciseIds: Set<String> = emptySet(),
    val completed: Boolean = false,
    val completionRecorded: Boolean = false,
    val lessonLanguage: NativeLanguage = NativeLanguage.Portuguese
) {
    val currentItem: ExerciseItem?
        get() = lesson?.items?.getOrNull(currentIndex)

    val totalItems: Int
        get() = lesson?.items?.size ?: 0

    val progress: Float
        get() = if (totalItems == 0) 0f else currentIndex.toFloat() / totalItems.toFloat()

    val xpEarned: Int
        get() = correctExerciseIds.size * XP_PER_CORRECT

    companion object {
        private const val XP_PER_CORRECT = 10
    }
}

@HiltViewModel
class ExerciseViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val learnerProgressRepository: LearnerProgressRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExerciseUiState())
    val uiState: StateFlow<ExerciseUiState> = _uiState.asStateFlow()
    private var currentSkillKey: String? = null

    init {
        viewModelScope.launch {
            userPreferencesRepository.preferences.collect { preferences ->
                currentSkillKey?.let { skillKey ->
                    loadLesson(skillKey, preferences.nativeLanguage)
                }
            }
        }
    }

    fun load(skillKey: String) {
        currentSkillKey = skillKey
        loadLesson(skillKey, userPreferencesRepository.preferences.value.nativeLanguage)
    }

    private fun loadLesson(skillKey: String, nativeLanguage: NativeLanguage) {
        val lesson = exerciseRepository.getLesson(skillKey, nativeLanguage)
        val state = _uiState.value
        if (state.lesson?.id == lesson.id && state.lessonLanguage == nativeLanguage) return

        _uiState.value = ExerciseUiState(
            lesson = lesson,
            lessonLanguage = nativeLanguage
        )
    }

    fun selectChoice(index: Int) {
        _uiState.update {
            it.copy(
                selectedChoiceIndex = index,
                checked = false,
                isCorrect = null
            )
        }
    }

    fun selectMatchLeft(left: String) {
        val state = _uiState.value
        if (state.matchedPairs.containsKey(left)) return
        _uiState.update {
            it.copy(
                selectedMatchLeft = left,
                checked = false,
                isCorrect = null
            )
        }
    }

    fun selectMatchRight(right: String) {
        val state = _uiState.value
        val item = state.currentItem as? ExerciseItem.MatchingPairs ?: return
        val left = state.selectedMatchLeft ?: return
        if (state.matchedPairs.containsValue(right)) return

        val expectedRight = item.pairs.firstOrNull { it.left == left }?.right
        if (expectedRight == right) {
            val newPairs = state.matchedPairs + (left to right)
            val completedPairSet = newPairs.size == item.pairs.size
            _uiState.update {
                it.copy(
                    selectedMatchLeft = null,
                    matchedPairs = newPairs,
                    checked = completedPairSet,
                    isCorrect = if (completedPairSet) true else null,
                    correctExerciseIds = if (completedPairSet) {
                        it.correctExerciseIds + item.id
                    } else {
                        it.correctExerciseIds
                    }
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    selectedMatchLeft = null,
                    checked = true,
                    isCorrect = false
                )
            }
        }
    }

    fun updateWritingAnswer(answer: String) {
        _uiState.update {
            it.copy(
                writingAnswer = answer,
                checked = false,
                isCorrect = null
            )
        }
    }

    fun updateSpeechTranscript(transcript: String) {
        _uiState.update {
            it.copy(
                speechTranscript = transcript,
                checked = false,
                isCorrect = null
            )
        }
    }

    fun checkCurrent() {
        val state = _uiState.value
        val item = state.currentItem ?: return
        val correct = when (item) {
            is ExerciseItem.MultipleChoice -> state.selectedChoiceIndex == item.answerIndex
            is ExerciseItem.Listening -> state.selectedChoiceIndex == item.answerIndex
            is ExerciseItem.Writing -> item.acceptedAnswers.any { accepted ->
                state.writingAnswer.normalizedAnswer() == accepted.normalizedAnswer()
            }
            is ExerciseItem.Speaking -> item.acceptedPhrases.any { accepted ->
                state.speechTranscript.normalizedAnswer() == accepted.normalizedAnswer()
            }
            is ExerciseItem.MatchingPairs -> state.matchedPairs.size == item.pairs.size
        }

        _uiState.update {
            it.copy(
                checked = true,
                isCorrect = correct,
                correctExerciseIds = if (correct) it.correctExerciseIds + item.id else it.correctExerciseIds
            )
        }
    }

    fun retryCurrent() {
        _uiState.update { state ->
            state.copy(
                selectedChoiceIndex = null,
                selectedMatchLeft = null,
                matchedPairs = if (state.currentItem is ExerciseItem.MatchingPairs) state.matchedPairs else emptyMap(),
                writingAnswer = "",
                speechTranscript = "",
                checked = false,
                isCorrect = null
            )
        }
    }

    fun continueLesson() {
        val state = _uiState.value
        val lesson = state.lesson ?: return
        if (!state.checked || state.isCorrect != true) return

        val nextIndex = state.currentIndex + 1
        if (nextIndex >= lesson.items.size) {
            if (!state.completionRecorded) {
                learnerProgressRepository.recordLessonCompletion(
                    LessonCompletionResult(
                        lessonId = lesson.id,
                        skill = lesson.skill,
                        earnedXp = state.xpEarned,
                        correctCount = state.correctExerciseIds.size,
                        totalCount = lesson.items.size
                    )
                )
            }
            _uiState.update {
                it.copy(
                    completed = true,
                    completionRecorded = true,
                    currentIndex = lesson.items.size
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                currentIndex = nextIndex,
                selectedChoiceIndex = null,
                selectedMatchLeft = null,
                matchedPairs = emptyMap(),
                writingAnswer = "",
                speechTranscript = "",
                checked = false,
                isCorrect = null
            )
        }
    }

    private fun String.normalizedAnswer(): String {
        return lowercase()
            .replace("'", "")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
