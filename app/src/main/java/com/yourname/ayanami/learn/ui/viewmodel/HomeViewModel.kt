package com.yourname.ayanami.learn.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.yourname.ayanami.learn.data.model.LearnerProgress
import com.yourname.ayanami.learn.data.repository.LearnerProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    learnerProgressRepository: LearnerProgressRepository
) : ViewModel() {
    val progress: StateFlow<LearnerProgress> = learnerProgressRepository.progress
}
