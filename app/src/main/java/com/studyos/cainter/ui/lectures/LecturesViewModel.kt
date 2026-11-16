package com.studyos.cainter.ui.lectures

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.studyos.cainter.data.local.entity.LectureTracker
import com.studyos.cainter.data.repository.StudyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LecturesViewModel(
    private val repository: StudyRepository
) : ViewModel() {

    private val _trackers = MutableStateFlow<List<LectureTracker>>(emptyList())
    val trackers: StateFlow<List<LectureTracker>> = _trackers.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllLectureTrackers().collect {
                _trackers.value = it
            }
        }
    }

    fun incrementLecture(subject: String) {
        viewModelScope.launch {
            repository.incrementLecture(subject)
            // Also log to study sessions automatically
            val duration = if (subject == "COSTING") 138 else 90
            repository.logSession(
                subject = subject,
                category = subject,
                durationMinutes = duration,
                notes = "Completed $subject lecture via lecture tracker"
            )
        }
    }

    fun decrementLecture(subject: String) {
        viewModelScope.launch {
            repository.decrementLecture(subject)
        }
    }

    class Factory(private val repository: StudyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LecturesViewModel(repository) as T
        }
    }
}
