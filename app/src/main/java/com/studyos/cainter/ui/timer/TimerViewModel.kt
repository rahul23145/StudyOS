package com.studyos.cainter.ui.timer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.studyos.cainter.data.repository.ScheduleRepository
import com.studyos.cainter.data.repository.StudyRepository
import com.studyos.cainter.util.DateUtils
import com.studyos.cainter.util.FocusGuardService
import com.studyos.cainter.util.SoundVibrationHelper
import com.studyos.cainter.util.ZeroDistractionGuard
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class TimerState {
    IDLE, RUNNING, PAUSED, FINISHED
}

data class TimerUiState(
    val presetTitle: String = "2-Hour Study Block",
    val totalSeconds: Long = 120 * 60L,
    val remainingSeconds: Long = 120 * 60L,
    val formattedTime: String = "02:00:00",
    val progressPercent: Int = 100,
    val state: TimerState = TimerState.IDLE
)

data class SequenceTask(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val durationMinutes: Int,
    val category: String = "STUDY_BLOCK",
    val isCompleted: Boolean = false,
    val scheduleItemId: Long? = null
)

class TimerViewModel(
    application: Application,
    private val studyRepository: StudyRepository,
    private val scheduleRepository: ScheduleRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private val _timerFinishedEvent = MutableSharedFlow<String>()
    val timerFinishedEvent: SharedFlow<String> = _timerFinishedEvent.asSharedFlow()

    // Sequential Task Flow Mode State
    private val _isSequenceMode = MutableStateFlow(false)
    val isSequenceMode: StateFlow<Boolean> = _isSequenceMode.asStateFlow()

    private val _sequenceQueue = MutableStateFlow<List<SequenceTask>>(emptyList())
    val sequenceQueue: StateFlow<List<SequenceTask>> = _sequenceQueue.asStateFlow()

    private val _currentSequenceIndex = MutableStateFlow(0)
    val currentSequenceIndex: StateFlow<Int> = _currentSequenceIndex.asStateFlow()

    private val _isAutoAdvanceEnabled = MutableStateFlow(true)
    val isAutoAdvanceEnabled: StateFlow<Boolean> = _isAutoAdvanceEnabled.asStateFlow()

    private var timerJob: Job? = null

    init {
        loadTodayScheduleAsSequence()
    }

    fun setSequenceMode(enabled: Boolean) {
        _isSequenceMode.value = enabled
        if (enabled && _sequenceQueue.value.isNotEmpty()) {
            selectSequenceTask(_currentSequenceIndex.value.coerceIn(0, _sequenceQueue.value.size - 1))
        }
    }

    fun toggleAutoAdvance(enabled: Boolean) {
        _isAutoAdvanceEnabled.value = enabled
    }

    fun loadTodayScheduleAsSequence() {
        viewModelScope.launch {
            val todayDay = DateUtils.getTodayDayOfWeek()
            val activeType = scheduleRepository.getActiveScheduleTypeSync()
            val typeId = activeType?.id ?: 1L
            val items = scheduleRepository.getScheduleForDaySync(todayDay, typeId)

            val tasks = if (items.isNotEmpty()) {
                items.map { item ->
                    SequenceTask(
                        id = item.id,
                        title = item.title,
                        durationMinutes = item.durationMinutes.coerceAtLeast(1),
                        category = item.category,
                        isCompleted = false,
                        scheduleItemId = item.id
                    )
                }
            } else {
                listOf(
                    SequenceTask(title = "Morning Deep Focus Block", durationMinutes = 120),
                    SequenceTask(title = "Rest & Stretch Break", durationMinutes = 15, category = "BREAK"),
                    SequenceTask(title = "Afternoon Study Block", durationMinutes = 120),
                    SequenceTask(title = "Lunch & Relaxation", durationMinutes = 45, category = "BREAK"),
                    SequenceTask(title = "Evening Revision & Practice", durationMinutes = 90)
                )
            }

            _sequenceQueue.value = tasks
            _currentSequenceIndex.value = 0
            if (_isSequenceMode.value && tasks.isNotEmpty()) {
                selectSequenceTask(0)
            }
        }
    }

    fun addCustomSequenceTask(title: String, durationMinutes: Int) {
        val newTask = SequenceTask(
            title = title,
            durationMinutes = durationMinutes.coerceAtLeast(1)
        )
        val updated = _sequenceQueue.value.toMutableList()
        updated.add(newTask)
        _sequenceQueue.value = updated
        if (updated.size == 1 && _isSequenceMode.value) {
            selectSequenceTask(0)
        }
    }

    fun removeSequenceTask(index: Int) {
        val list = _sequenceQueue.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _sequenceQueue.value = list
            if (_currentSequenceIndex.value >= list.size) {
                _currentSequenceIndex.value = (list.size - 1).coerceAtLeast(0)
            }
            if (_isSequenceMode.value && list.isNotEmpty()) {
                selectSequenceTask(_currentSequenceIndex.value)
            }
        }
    }

    fun selectSequenceTask(index: Int) {
        val list = _sequenceQueue.value
        if (index in list.indices) {
            _currentSequenceIndex.value = index
            val task = list[index]
            setPreset(task.title, task.durationMinutes)
        }
    }

    fun skipToNextTask() {
        val nextIdx = _currentSequenceIndex.value + 1
        if (nextIdx < _sequenceQueue.value.size) {
            selectSequenceTask(nextIdx)
            if (_uiState.value.state == TimerState.RUNNING) {
                startTimer()
            }
        }
    }

    fun previousTask() {
        val prevIdx = _currentSequenceIndex.value - 1
        if (prevIdx >= 0) {
            selectSequenceTask(prevIdx)
            if (_uiState.value.state == TimerState.RUNNING) {
                startTimer()
            }
        }
    }

    fun setPreset(title: String, durationMinutes: Int) {
        timerJob?.cancel()
        val totalSec = durationMinutes * 60L
        _uiState.value = TimerUiState(
            presetTitle = title,
            totalSeconds = totalSec,
            remainingSeconds = totalSec,
            formattedTime = formatSeconds(totalSec),
            progressPercent = 100,
            state = TimerState.IDLE
        )
    }

    fun startOrPause() {
        when (_uiState.value.state) {
            TimerState.IDLE, TimerState.PAUSED -> startTimer()
            TimerState.RUNNING -> pauseTimer()
            TimerState.FINISHED -> resetTimer()
        }
    }

    fun finishCurrentTaskEarly() {
        viewModelScope.launch {
            val total = _uiState.value.totalSeconds
            val remaining = _uiState.value.remainingSeconds
            val elapsedSec = (total - remaining).coerceAtLeast(60L)
            val elapsedMins = (elapsedSec / 60).toInt().coerceAtLeast(1)

            timerJob?.cancel()

            // 1. Log actual elapsed study time
            studyRepository.logSession(
                subject = _uiState.value.presetTitle,
                category = "TIMER_SESSION",
                durationMinutes = elapsedMins,
                notes = "Completed early in ${elapsedMins}m via Flow Timer"
            )

            SoundVibrationHelper.playSessionCompleteAlert(getApplication())

            // 2. If in sequence mode, advance and adjust timetable
            if (_isSequenceMode.value) {
                val list = _sequenceQueue.value.toMutableList()
                val currentIdx = _currentSequenceIndex.value
                if (currentIdx in list.indices) {
                    val finishedTask = list[currentIdx]
                    list[currentIdx] = finishedTask.copy(isCompleted = true)
                    _sequenceQueue.value = list

                    val todayDate = DateUtils.getTodayDateString()
                    val todayDay = DateUtils.getTodayDayOfWeek()
                    val activeType = scheduleRepository.getActiveScheduleTypeSync()
                    val typeId = activeType?.id ?: 1L

                    // Mark completion in timetable
                    if (finishedTask.scheduleItemId != null) {
                        scheduleRepository.setCompletion(todayDate, finishedTask.scheduleItemId, true)
                        // Adjust remaining timetable blocks starting from right now!
                        scheduleRepository.adjustRemainingScheduleFromNow(todayDay, typeId, finishedTask.scheduleItemId)
                    }

                    // Auto-advance to next sequential task
                    if ((currentIdx + 1) < list.size) {
                        val nextIdx = currentIdx + 1
                        selectSequenceTask(nextIdx)
                        _timerFinishedEvent.emit("Completed '${finishedTask.title}' in ${elapsedMins}m! Starting '${list[nextIdx].title}'. Timetable auto-adjusted to current time.")
                        startTimer()
                        return@launch
                    }
                }
            }

            // If single mode or no more tasks
            FocusGuardService.stopService(getApplication())
            ZeroDistractionGuard.setSessionActive(getApplication(), false)
            _uiState.value = _uiState.value.copy(
                state = TimerState.FINISHED,
                progressPercent = 0,
                formattedTime = "00:00:00"
            )
            _timerFinishedEvent.emit("Completed '${_uiState.value.presetTitle}' early in ${elapsedMins}m!")
        }
    }

    fun adjustRemainingTime(deltaMinutes: Int) {
        val deltaSec = deltaMinutes * 60L
        val currentRem = _uiState.value.remainingSeconds
        val newRem = currentRem + deltaSec

        if (newRem <= 0) {
            finishCurrentTaskEarly()
            return
        }

        val newTotal = (_uiState.value.totalSeconds + deltaSec).coerceAtLeast(newRem)
        val progress = if (newTotal > 0) ((newRem * 100) / newTotal).toInt() else 0
        val formatted = formatSeconds(newRem)

        _uiState.value = _uiState.value.copy(
            totalSeconds = newTotal,
            remainingSeconds = newRem,
            formattedTime = formatted,
            progressPercent = progress
        )

        FocusGuardService.updateService(
            getApplication(),
            _uiState.value.presetTitle,
            formatted
        )
        ZeroDistractionGuard.setSessionActive(
            getApplication(),
            true,
            _uiState.value.presetTitle,
            formatted
        )
    }

    private fun startTimer() {
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(state = TimerState.RUNNING)

        FocusGuardService.startService(
            getApplication(),
            _uiState.value.presetTitle,
            _uiState.value.formattedTime
        )
        ZeroDistractionGuard.setSessionActive(
            getApplication(),
            true,
            _uiState.value.presetTitle,
            _uiState.value.formattedTime
        )

        timerJob = viewModelScope.launch {
            while (_uiState.value.remainingSeconds > 0) {
                delay(1000L)
                val newRemaining = _uiState.value.remainingSeconds - 1
                val total = _uiState.value.totalSeconds
                val progress = if (total > 0) ((newRemaining * 100) / total).toInt() else 0
                val formatted = formatSeconds(newRemaining)
                _uiState.value = _uiState.value.copy(
                    remainingSeconds = newRemaining,
                    formattedTime = formatted,
                    progressPercent = progress
                )

                FocusGuardService.updateService(
                    getApplication(),
                    _uiState.value.presetTitle,
                    formatted
                )
                ZeroDistractionGuard.setSessionActive(
                    getApplication(),
                    true,
                    _uiState.value.presetTitle,
                    formatted
                )

                // Enforce Zero Distraction Mode: check if user opened a blocked app
                ZeroDistractionGuard.checkAndBlockForegroundApp(
                    getApplication(),
                    _uiState.value.presetTitle,
                    formatted
                )
            }

            // Current Task Finished!
            handleTaskFinished()
        }
    }

    private suspend fun handleTaskFinished() {
        FocusGuardService.stopService(getApplication())
        ZeroDistractionGuard.setSessionActive(getApplication(), false)

        _uiState.value = _uiState.value.copy(
            state = TimerState.FINISHED,
            progressPercent = 0,
            formattedTime = "00:00:00"
        )
        SoundVibrationHelper.playSessionCompleteAlert(getApplication())
        _timerFinishedEvent.emit(_uiState.value.presetTitle)

        // Automatically log completed session
        val elapsedMinutes = (_uiState.value.totalSeconds / 60).toInt().coerceAtLeast(1)
        studyRepository.logSession(
            subject = _uiState.value.presetTitle,
            category = "TIMER_SESSION",
            durationMinutes = elapsedMinutes,
            notes = "Completed via StudyOS Flow Timer"
        )

        // If in sequential flow mode
        if (_isSequenceMode.value) {
            val list = _sequenceQueue.value.toMutableList()
            val currentIdx = _currentSequenceIndex.value
            if (currentIdx in list.indices) {
                val finishedTask = list[currentIdx]
                list[currentIdx] = finishedTask.copy(isCompleted = true)
                _sequenceQueue.value = list

                // Mark timetable task complete on dashboard
                if (finishedTask.scheduleItemId != null) {
                    val todayDate = DateUtils.getTodayDateString()
                    scheduleRepository.setCompletion(todayDate, finishedTask.scheduleItemId, true)
                }

                // Auto-advance to next sequential task
                if (_isAutoAdvanceEnabled.value && (currentIdx + 1) < list.size) {
                    delay(1500L) // Brief pause to celebrate completion
                    val nextIdx = currentIdx + 1
                    selectSequenceTask(nextIdx)
                    startTimer() // Automatically starts the next task!
                }
            }
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(state = TimerState.PAUSED)
        FocusGuardService.stopService(getApplication())
        ZeroDistractionGuard.setSessionActive(getApplication(), false)
    }

    fun resetTimer() {
        timerJob?.cancel()
        val total = _uiState.value.totalSeconds
        _uiState.value = _uiState.value.copy(
            remainingSeconds = total,
            formattedTime = formatSeconds(total),
            progressPercent = 100,
            state = TimerState.IDLE
        )
        FocusGuardService.stopService(getApplication())
        ZeroDistractionGuard.setSessionActive(getApplication(), false)
    }

    fun logCurrentSession(notes: String = "") {
        viewModelScope.launch {
            val elapsedSec = _uiState.value.totalSeconds - _uiState.value.remainingSeconds
            val elapsedMinutes = (elapsedSec / 60).toInt().coerceAtLeast(1)
            studyRepository.logSession(
                subject = _uiState.value.presetTitle,
                category = "TIMER_SESSION",
                durationMinutes = elapsedMinutes,
                notes = if (notes.isNotBlank()) notes else "Completed via StudyOS Timer"
            )
        }
    }

    private fun formatSeconds(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return String.format("%02d:%02d:%02d", h, m, s)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        FocusGuardService.stopService(getApplication())
        ZeroDistractionGuard.setSessionActive(getApplication(), false)
    }

    class Factory(
        private val application: Application,
        private val studyRepository: StudyRepository,
        private val scheduleRepository: ScheduleRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TimerViewModel(application, studyRepository, scheduleRepository) as T
        }
    }
}
