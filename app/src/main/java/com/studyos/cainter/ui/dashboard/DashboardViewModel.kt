package com.studyos.cainter.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.studyos.cainter.BuildConfig
import com.studyos.cainter.data.local.entity.LectureTracker
import com.studyos.cainter.data.local.entity.ScheduleItem
import com.studyos.cainter.data.local.entity.TodoItem
import com.studyos.cainter.data.repository.ScheduleRepository
import com.studyos.cainter.data.repository.StudyRepository
import com.studyos.cainter.data.repository.TodoRepository
import com.studyos.cainter.ui.common.DayStudyData
import com.studyos.cainter.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

data class DashboardUiState(
    val dateDisplay: String = "",
    val routineLabel: String = "",
    val streakDays: Int = 0,
    val items: List<TodayScheduleUiModel> = emptyList(),
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val progressPercentage: Int = 0,
    val currentItem: ScheduleItem? = null,
    val nextItem: ScheduleItem? = null,
    val costingTracker: LectureTracker? = null,
    val auditTracker: LectureTracker? = null,
    val studyMinutesToday: Int = 0,
    val totalStudyMinutesAllTime: Int = 0,
    val chartData: List<DayStudyData> = emptyList()
)

class DashboardViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val studyRepository: StudyRepository,
    private val todoRepository: TodoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    val todoItems = todoRepository.getAllTodos()

    fun addTodo(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            todoRepository.addTodo(title)
        }
    }

    fun toggleTodo(todo: TodoItem, isCompleted: Boolean) {
        viewModelScope.launch {
            todoRepository.setCompleted(todo.id, isCompleted)
        }
    }

    fun deleteTodo(todo: TodoItem) {
        viewModelScope.launch {
            todoRepository.deleteTodoById(todo.id)
        }
    }

    fun clearCompletedTodos() {
        viewModelScope.launch {
            todoRepository.clearCompleted()
        }
    }

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        val todayDay = DateUtils.getTodayDayOfWeek()
        val todayDate = DateUtils.getTodayDateString()

        viewModelScope.launch {
            scheduleRepository.getActiveScheduleType().flatMapLatest { activeType ->
                val type = activeType ?: scheduleRepository.getActiveScheduleTypeSync()
                val typeId = type?.id ?: 1L
                val targetHours = type?.targetDailyHours ?: 8.0f
                val streak = scheduleRepository.calculateStreak()

                val routineLabel = if (BuildConfig.IS_CUSTOM_APP || typeId != 1L) {
                    val profileName = type?.name ?: "Personal Routine"
                    "$profileName • Target ${targetHours}h/day"
                } else {
                    when (todayDay) {
                        1, 3, 5 -> "MWF Routine • Tuition 6:00–7:00 PM • Costing 2x + Audit 2x"
                        2, 4, 6 -> "TTSS Routine • Running 6:30–7:15 PM • Costing 2x + Audit 2x"
                        7 -> "Sunday Audit-Only Routine • Standards & Ethics Deep Dive"
                        else -> "CA Inter Mastery Routine"
                    }
                }

                combine(
                    scheduleRepository.getScheduleForDay(todayDay, typeId),
                    scheduleRepository.getCompletionsForDate(todayDate),
                    studyRepository.getAllLectureTrackers(),
                    studyRepository.getTodayStudyMinutes(),
                    studyRepository.getAllTimeStudyMinutes()
                ) { scheduleItems, completions, trackers, minutesToday, minutesAllTime ->
                    val completedMap = completions.associate { it.scheduleItemId to it.isCompleted }
                    val uiModels = scheduleItems.map { item ->
                        TodayScheduleUiModel(
                            item = item,
                            isCompleted = completedMap[item.id] ?: false
                        )
                    }

                    val completedCount = uiModels.count { it.isCompleted }
                    val totalCount = uiModels.size
                    val percentage = if (totalCount > 0) (completedCount * 100) / totalCount else 0

                    val current = scheduleItems.firstOrNull { DateUtils.isCurrentTimeBetween(it.startTime, it.endTime) }
                    val next = scheduleItems.firstOrNull { DateUtils.isUpcoming(it.startTime) }

                    val costing = trackers.firstOrNull { it.subject == "COSTING" }
                    val audit = trackers.firstOrNull { it.subject == "AUDIT" }

                    val chart = studyRepository.getWeeklyChartData(targetHoursPerDay = targetHours)

                    val currentStreak = scheduleRepository.calculateStreak()

                    DashboardUiState(
                        dateDisplay = DateUtils.getFormattedDisplayDate(),
                        routineLabel = routineLabel,
                        streakDays = currentStreak,
                        items = uiModels,
                        completedCount = completedCount,
                        totalCount = totalCount,
                        progressPercentage = percentage,
                        currentItem = current,
                        nextItem = next,
                        costingTracker = costing,
                        auditTracker = audit,
                        studyMinutesToday = minutesToday,
                        totalStudyMinutesAllTime = minutesAllTime,
                        chartData = chart
                    )
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun toggleTaskCompletion(item: ScheduleItem, isCompleted: Boolean) {
        viewModelScope.launch {
            val todayDate = DateUtils.getTodayDateString()
            scheduleRepository.setCompletion(todayDate, item.id, isCompleted)
        }
    }

    class Factory(
        private val scheduleRepository: ScheduleRepository,
        private val studyRepository: StudyRepository,
        private val todoRepository: TodoRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(scheduleRepository, studyRepository, todoRepository) as T
        }
    }
}
