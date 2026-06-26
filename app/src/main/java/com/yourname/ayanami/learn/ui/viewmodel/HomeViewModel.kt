package com.yourname.ayanami.learn.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.ayanami.learn.data.local.UserPreferencesRepository
import com.yourname.ayanami.learn.data.model.CurriculumUnit
import com.yourname.ayanami.learn.data.model.LearnerProgress
import com.yourname.ayanami.learn.data.repository.ExerciseRepository
import com.yourname.ayanami.learn.data.repository.LearnerProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    learnerProgressRepository: LearnerProgressRepository,
    private val exerciseRepository: ExerciseRepository,
    userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    val progress: StateFlow<LearnerProgress> = learnerProgressRepository.progress

    val curriculum: StateFlow<List<CurriculumUnit>> = userPreferencesRepository.preferences
        .map { exerciseRepository.getCurriculum(it.nativeLanguage) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = exerciseRepository.getCurriculum()
        )
}
