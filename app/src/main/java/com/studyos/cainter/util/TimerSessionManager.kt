package com.studyos.cainter.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class SessionTimerState {
    IDLE,
    RUNNING,
    PAUSED,
    FINISHED
}

data class TimerSessionData(
    val state: SessionTimerState = SessionTimerState.IDLE,
    val taskTitle: String = "Focus Session",
    val totalSeconds: Long = 0L,
    val remainingSeconds: Long = 0L,
    val targetEndTimeMs: Long = 0L,
    val formattedTime: String = "00:00:00",
    val progressPercent: Int = 100,
    val isAutoAdvance: Boolean = true,
    val scheduleItemId: Long? = null,
    val scheduledEndTime: String = ""
)

object TimerSessionManager {

    private val _sessionState = MutableStateFlow(TimerSessionData())
    val sessionState: StateFlow<TimerSessionData> = _sessionState.asStateFlow()

    // Callbacks for background action receiver triggers
    var onSessionFinishedListener: ((String, Long) -> Unit)? = null
    var onAdvanceNextListener: (() -> Unit)? = null

    fun updateState(newData: TimerSessionData) {
        _sessionState.value = newData
    }

    fun setRunning(
        taskTitle: String,
        totalSeconds: Long,
        targetEndTimeMs: Long,
        scheduleItemId: Long? = null,
        scheduledEndTime: String = "",
        autoAdvance: Boolean = true
    ) {
        val now = System.currentTimeMillis()
        val rem = ((targetEndTimeMs - now + 999L) / 1000L).coerceAtLeast(0L)
        val progress = if (totalSeconds > 0) ((rem * 100) / totalSeconds).toInt().coerceIn(0, 100) else 100
        val formatted = formatSeconds(rem)

        _sessionState.value = TimerSessionData(
            state = SessionTimerState.RUNNING,
            taskTitle = taskTitle,
            totalSeconds = totalSeconds,
            remainingSeconds = rem,
            targetEndTimeMs = targetEndTimeMs,
            formattedTime = formatted,
            progressPercent = progress,
            isAutoAdvance = autoAdvance,
            scheduleItemId = scheduleItemId,
            scheduledEndTime = scheduledEndTime
        )
    }

    fun updateRemainingFromClock(): Long {
        val current = _sessionState.value
        if (current.state != SessionTimerState.RUNNING) return current.remainingSeconds

        val now = System.currentTimeMillis()
        val rem = ((current.targetEndTimeMs - now + 999L) / 1000L).coerceAtLeast(0L)
        val progress = if (current.totalSeconds > 0) ((rem * 100) / current.totalSeconds).toInt().coerceIn(0, 100) else 0
        val formatted = formatSeconds(rem)

        _sessionState.value = current.copy(
            remainingSeconds = rem,
            formattedTime = formatted,
            progressPercent = progress
        )
        return rem
    }

    fun setPaused(pausedRemaining: Long) {
        val current = _sessionState.value
        val progress = if (current.totalSeconds > 0) ((pausedRemaining * 100) / current.totalSeconds).toInt().coerceIn(0, 100) else 0
        val formatted = formatSeconds(pausedRemaining)
        _sessionState.value = current.copy(
            state = SessionTimerState.PAUSED,
            remainingSeconds = pausedRemaining,
            formattedTime = formatted,
            progressPercent = progress
        )
    }

    fun setFinished() {
        val current = _sessionState.value
        _sessionState.value = current.copy(
            state = SessionTimerState.FINISHED,
            remainingSeconds = 0L,
            formattedTime = "00:00:00",
            progressPercent = 0
        )
    }

    fun setIdle(title: String, totalSeconds: Long, scheduledEndTime: String = "") {
        val formatted = formatSeconds(totalSeconds)
        _sessionState.value = TimerSessionData(
            state = SessionTimerState.IDLE,
            taskTitle = title,
            totalSeconds = totalSeconds,
            remainingSeconds = totalSeconds,
            targetEndTimeMs = 0L,
            formattedTime = formatted,
            progressPercent = 100,
            scheduledEndTime = scheduledEndTime
        )
    }

    fun formatSeconds(totalSec: Long): String {
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
    }
}
