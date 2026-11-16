package com.studyos.cainter.ui.timer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.studyos.cainter.StudyOSApplication
import com.studyos.cainter.data.repository.StudyRepository
import com.studyos.cainter.util.SoundVibrationHelper
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

class TimerViewModel(
    application: Application,
    private val studyRepository: StudyRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private val _timerFinishedEvent = MutableSharedFlow<String>()
    val timerFinishedEvent: SharedFlow<String> = _timerFinishedEvent.asSharedFlow()

    private var timerJob: Job? = null

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

    private fun startTimer() {
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(state = TimerState.RUNNING)

        timerJob = viewModelScope.launch {
            while (_uiState.value.remainingSeconds > 0) {
                delay(1000L)
                val newRemaining = _uiState.value.remainingSeconds - 1
                val total = _uiState.value.totalSeconds
                val progress = if (total > 0) ((newRemaining * 100) / total).toInt() else 0
                _uiState.value = _uiState.value.copy(
                    remainingSeconds = newRemaining,
                    formattedTime = formatSeconds(newRemaining),
                    progressPercent = progress
                )
            }
            // Timer Finished
            _uiState.value = _uiState.value.copy(
                state = TimerState.FINISHED,
                progressPercent = 0,
                formattedTime = "00:00:00"
            )
            SoundVibrationHelper.playSessionCompleteAlert(getApplication())
            _timerFinishedEvent.emit(_uiState.value.presetTitle)
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(state = TimerState.PAUSED)
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
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }

    class Factory(
        private val application: Application,
        private val repository: StudyRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TimerViewModel(application, repository) as T
        }
    }
}
