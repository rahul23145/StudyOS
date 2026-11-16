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

    fun incrementLecture(subject: String, duration: Int = 60) {
        viewModelScope.launch {
            repository.incrementLecture(subject)
            val sessionDuration = when (subject) {
                "COSTING" -> 138
                "AUDIT" -> 90
                else -> duration
            }
            repository.logSession(
                subject = subject,
                category = subject,
                durationMinutes = sessionDuration,
                notes = "Completed $subject session"
            )
        }
    }

    fun decrementLecture(subject: String) {
        viewModelScope.launch {
            repository.decrementLecture(subject)
        }
    }

    fun addSubject(name: String, total: Int, duration: Int, dailyTarget: Int) {
        viewModelScope.launch {
            val key = "SUBJ_" + System.currentTimeMillis()
            val tracker = LectureTracker(
                subject = key,
                displayName = name,
                totalLectures = total,
                completedLectures = 0,
                dailyTarget = dailyTarget,
                lectureDurationMinutes = duration
            )
            repository.addOrUpdateSubject(tracker)
        }
    }

    fun deleteSubject(subject: String) {
        viewModelScope.launch {
            repository.deleteSubject(subject)
        }
    }

    class Factory(private val repository: StudyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LecturesViewModel(repository) as T
        }
    }
}
