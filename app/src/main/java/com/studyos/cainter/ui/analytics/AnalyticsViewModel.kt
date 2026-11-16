package com.studyos.cainter.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.studyos.cainter.data.local.entity.StudySession
import com.studyos.cainter.data.repository.ScheduleRepository
import com.studyos.cainter.data.repository.StudyRepository
import com.studyos.cainter.ui.common.DayStudyData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class AnalyticsUiState(
    val todayMinutes: Int = 0,
    val weekMinutes: Int = 0,
    val streak: Int = 0,
    val chartData: List<DayStudyData> = emptyList(),
    val recentSessions: List<StudySession> = emptyList()
)

class AnalyticsViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val studyRepository: StudyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            val streak = scheduleRepository.calculateStreak()

            combine(
                studyRepository.getTodayStudyMinutes(),
                studyRepository.getWeekStudyMinutes(),
                studyRepository.getRecentSessions()
            ) { todayMins, weekMins, sessions ->
                val chart = studyRepository.getWeeklyChartData(targetHoursPerDay = 8.0f)
                AnalyticsUiState(
                    todayMinutes = todayMins,
                    weekMinutes = weekMins,
                    streak = streak,
                    chartData = chart,
                    recentSessions = sessions.take(15)
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    class Factory(
        private val scheduleRepository: ScheduleRepository,
        private val studyRepository: StudyRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AnalyticsViewModel(scheduleRepository, studyRepository) as T
        }
    }
}
