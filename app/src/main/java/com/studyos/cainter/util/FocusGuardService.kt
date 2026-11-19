package com.studyos.cainter.util

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.studyos.cainter.R
import com.studyos.cainter.data.local.AppDatabase
import com.studyos.cainter.data.local.entity.ScheduleCompletion
import com.studyos.cainter.data.local.entity.StudySession
import com.studyos.cainter.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FocusGuardService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var tickerJob: Job? = null
    private var monitorJob: Job? = null

    private var wakeLock: PowerManager.WakeLock? = null
    private var alarmPendingIntent: PendingIntent? = null

    @Volatile private var currentTitle: String = "Deep Focus Session"
    @Volatile private var currentTotalSeconds: Long = 0L
    @Volatile private var currentTargetEndTimeMs: Long = 0L
    @Volatile private var currentRemainingSeconds: Long = 0L
    @Volatile private var isSessionRunning: Boolean = false
    @Volatile private var isSessionPaused: Boolean = false
    @Volatile private var isAutoAdvance: Boolean = true
    @Volatile private var currentScheduleItemId: Long? = null
    @Volatile private var currentScheduledEndTime: String = ""

    companion object {
        const val CHANNEL_ID = "studyos_focus_guard_channel"
        const val FINISH_CHANNEL_ID = "studyos_focus_finished_channel"
        const val NOTIFICATION_ID = 9001
        const val FINISHED_NOTIFICATION_ID = 9002

        const val ACTION_START = "ACTION_START_FOCUS"
        const val ACTION_PAUSE_RESUME = "ACTION_PAUSE_RESUME"
        const val ACTION_DONE_NEXT = "ACTION_DONE_NEXT"
        const val ACTION_STOP = "ACTION_STOP_FOCUS"
        const val ACTION_ALARM_FINISH = "ACTION_ALARM_FINISH"

        fun startFocusSession(
            context: Context,
            taskTitle: String,
            totalSeconds: Long,
            remainingSeconds: Long = totalSeconds,
            targetEndTimeMs: Long = System.currentTimeMillis() + remainingSeconds * 1000L,
            scheduleItemId: Long? = null,
            scheduledEndTime: String = "",
            autoAdvance: Boolean = true
        ) {
            val intent = Intent(context, FocusGuardService::class.java).apply {
                action = ACTION_START
                putExtra("TASK_TITLE", taskTitle)
                putExtra("TOTAL_SECONDS", totalSeconds)
                putExtra("REMAINING_SECONDS", remainingSeconds)
                putExtra("TARGET_END_TIME_MS", targetEndTimeMs)
                putExtra("SCHEDULE_ITEM_ID", scheduleItemId ?: -1L)
                putExtra("SCHEDULED_END_TIME", scheduledEndTime)
                putExtra("AUTO_ADVANCE", autoAdvance)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pauseOrResume(context: Context) {
            val intent = Intent(context, FocusGuardService::class.java).apply {
                action = ACTION_PAUSE_RESUME
            }
            context.startService(intent)
        }

        fun doneAndNext(context: Context) {
            val intent = Intent(context, FocusGuardService::class.java).apply {
                action = ACTION_DONE_NEXT
            }
            context.startService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FocusGuardService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun onAlarmTriggered(context: Context) {
            val intent = Intent(context, FocusGuardService::class.java).apply {
                action = ACTION_ALARM_FINISH
            }
            context.startService(intent)
        }

        // Backward compatibility for existing call sites
        fun startService(context: Context, taskTitle: String, remainingTime: String) {
            val totalSec = parseFormattedTimeToSeconds(remainingTime)
            startFocusSession(context, taskTitle, totalSec, totalSec)
        }

        fun updateService(context: Context, taskTitle: String, remainingTime: String) {
            // No-op: service now handles ticking internally using wall-clock time
        }

        private fun parseFormattedTimeToSeconds(time: String): Long {
            return try {
                val parts = time.split(":").map { it.trim().toLong() }
                when (parts.size) {
                    3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
                    2 -> parts[0] * 60 + parts[1]
                    1 -> parts[0]
                    else -> 0L
                }
            } catch (_: Exception) {
                0L
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_START -> {
                val title = intent?.getStringExtra("TASK_TITLE") ?: "Focus Session"
                val totalSec = intent?.getLongExtra("TOTAL_SECONDS", 1500L) ?: 1500L
                val remSec = intent?.getLongExtra("REMAINING_SECONDS", totalSec) ?: totalSec
                val targetEndMs = intent?.getLongExtra("TARGET_END_TIME_MS", System.currentTimeMillis() + remSec * 1000L)
                    ?: (System.currentTimeMillis() + remSec * 1000L)
                val rawItemId = intent?.getLongExtra("SCHEDULE_ITEM_ID", -1L) ?: -1L
                val itemId = if (rawItemId > 0) rawItemId else null
                val schedEnd = intent?.getStringExtra("SCHEDULED_END_TIME") ?: ""
                val autoAdv = intent?.getBooleanExtra("AUTO_ADVANCE", true) ?: true

                startSessionInternal(title, totalSec, remSec, targetEndMs, itemId, schedEnd, autoAdv)
            }
            ACTION_PAUSE_RESUME -> {
                togglePauseResumeInternal()
            }
            ACTION_DONE_NEXT -> {
                completeSessionInternal(advancedByButton = true)
            }
            ACTION_ALARM_FINISH -> {
                completeSessionInternal(advancedByButton = false)
            }
            ACTION_STOP -> {
                stopSessionInternal()
            }
        }
        return START_STICKY
    }

    private fun startSessionInternal(
        title: String,
        totalSec: Long,
        remSec: Long,
        targetEndMs: Long,
        itemId: Long?,
        schedEnd: String,
        autoAdv: Boolean
    ) {
        currentTitle = title
        currentTotalSeconds = totalSec.coerceAtLeast(1L)
        currentRemainingSeconds = remSec.coerceAtLeast(0L)
        currentTargetEndTimeMs = targetEndMs
        currentScheduleItemId = itemId
        currentScheduledEndTime = schedEnd
        isAutoAdvance = autoAdv
        isSessionRunning = true
        isSessionPaused = false

        acquireWakeLock(currentRemainingSeconds * 1000L + 60_000L)
        scheduleCompletionAlarm(currentTargetEndTimeMs)

        TimerSessionManager.setRunning(
            taskTitle = title,
            totalSeconds = currentTotalSeconds,
            targetEndTimeMs = currentTargetEndTimeMs,
            scheduleItemId = itemId,
            scheduledEndTime = schedEnd,
            autoAdvance = autoAdv
        )

        val notification = buildOngoingNotification(title, TimerSessionManager.formatSeconds(currentRemainingSeconds), isPaused = false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        startTicker()
        startDistractionMonitoring()
    }

    private fun togglePauseResumeInternal() {
        if (!isSessionRunning && !isSessionPaused) return

        if (isSessionRunning && !isSessionPaused) {
            // PAUSE
            isSessionPaused = true
            isSessionRunning = false
            tickerJob?.cancel()
            cancelCompletionAlarm()
            releaseWakeLock()

            val now = System.currentTimeMillis()
            currentRemainingSeconds = ((currentTargetEndTimeMs - now + 999L) / 1000L).coerceAtLeast(0L)
            TimerSessionManager.setPaused(currentRemainingSeconds)

            val notification = buildOngoingNotification(currentTitle, TimerSessionManager.formatSeconds(currentRemainingSeconds), isPaused = true)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.notify(NOTIFICATION_ID, notification)
        } else if (isSessionPaused) {
            // RESUME
            isSessionPaused = false
            isSessionRunning = true
            currentTargetEndTimeMs = System.currentTimeMillis() + currentRemainingSeconds * 1000L

            acquireWakeLock(currentRemainingSeconds * 1000L + 60_000L)
            scheduleCompletionAlarm(currentTargetEndTimeMs)

            TimerSessionManager.setRunning(
                taskTitle = currentTitle,
                totalSeconds = currentTotalSeconds,
                targetEndTimeMs = currentTargetEndTimeMs,
                scheduleItemId = currentScheduleItemId,
                scheduledEndTime = currentScheduledEndTime,
                autoAdvance = isAutoAdvance
            )

            val notification = buildOngoingNotification(currentTitle, TimerSessionManager.formatSeconds(currentRemainingSeconds), isPaused = false)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.notify(NOTIFICATION_ID, notification)

            startTicker()
            startDistractionMonitoring()
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive && isSessionRunning) {
                delay(1000L)
                val rem = TimerSessionManager.updateRemainingFromClock()
                currentRemainingSeconds = rem

                if (rem <= 0L) {
                    completeSessionInternal(advancedByButton = false)
                    break
                } else {
                    val formatted = TimerSessionManager.formatSeconds(rem)
                    val notification = buildOngoingNotification(currentTitle, formatted, isPaused = false)
                    val manager = getSystemService(NotificationManager::class.java)
                    manager?.notify(NOTIFICATION_ID, notification)
                }
            }
        }
    }

    private fun startDistractionMonitoring() {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            while (isActive && isSessionRunning) {
                delay(1500L)
                val formatted = TimerSessionManager.formatSeconds(currentRemainingSeconds)
                ZeroDistractionGuard.checkAndBlockForegroundApp(
                    this@FocusGuardService,
                    currentTitle,
                    formatted
                )
            }
        }
    }

    private fun completeSessionInternal(advancedByButton: Boolean) {
        tickerJob?.cancel()
        monitorJob?.cancel()
        cancelCompletionAlarm()
        releaseWakeLock()

        isSessionRunning = false
        isSessionPaused = false
        TimerSessionManager.setFinished()

        val taskTitle = currentTitle
        val totalSec = currentTotalSeconds
        val elapsedMinutes = (totalSec / 60L).toInt().coerceAtLeast(1)

        // 1. Play alert chime & vibration
        SoundVibrationHelper.playSessionCompleteAlert(this)

        // 2. Post High-Priority Completion Notification
        postFinishedNotification(taskTitle, elapsedMinutes)

        // 3. Automatically log study session to Room Database
        serviceScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getInstance(applicationContext)
                db.studySessionDao().insertSession(
                    StudySession(
                        subject = taskTitle,
                        category = "STUDY",
                        durationMinutes = elapsedMinutes,
                        timestamp = System.currentTimeMillis(),
                        notes = "Completed via StudyOS Focus Timer"
                    )
                )
                val itemId = currentScheduleItemId
                if (itemId != null && itemId > 0) {
                    db.scheduleDao().insertCompletion(
                        ScheduleCompletion(
                            scheduleItemId = itemId,
                            date = DateUtils.getTodayDateString(),
                            isCompleted = true,
                            completedAt = System.currentTimeMillis()
                        )
                    )
                }
            } catch (_: Exception) {}
        }

        // 4. Notify ViewModel listeners
        TimerSessionManager.onSessionFinishedListener?.invoke(taskTitle, totalSec)
        if (isAutoAdvance || advancedByButton) {
            TimerSessionManager.onAdvanceNextListener?.invoke()
        }

        // Remove ongoing foreground notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun stopSessionInternal() {
        tickerJob?.cancel()
        monitorJob?.cancel()
        cancelCompletionAlarm()
        releaseWakeLock()

        isSessionRunning = false
        isSessionPaused = false
        TimerSessionManager.setIdle(currentTitle, currentTotalSeconds, currentScheduledEndTime)

        OverlayBlockerManager.hideOverlay(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun acquireWakeLock(timeoutMs: Long) {
        try {
            releaseWakeLock()
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "StudyOS:FocusTimerWakeLock")?.apply {
                acquire(timeoutMs.coerceAtLeast(60_000L))
            }
        } catch (_: Exception) {}
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
        wakeLock = null
    }

    private fun scheduleCompletionAlarm(triggerAtMs: Long) {
        try {
            val am = getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(this, TimerActionReceiver::class.java).apply {
                action = TimerActionReceiver.ACTION_TIMER_ALARM
            }
            alarmPendingIntent = PendingIntent.getBroadcast(
                this,
                9002,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, alarmPendingIntent!!)
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMs, alarmPendingIntent!!)
            }
        } catch (_: Exception) {}
    }

    private fun cancelCompletionAlarm() {
        try {
            val am = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmPendingIntent?.let { am?.cancel(it) }
            alarmPendingIntent = null
        } catch (_: Exception) {}
    }

    private fun buildOngoingNotification(title: String, remaining: String, isPaused: Boolean): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NAVIGATE_TO", "TIMER")
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeIntent = Intent(this, TimerActionReceiver::class.java).apply {
            action = TimerActionReceiver.ACTION_TIMER_PAUSE_RESUME
        }
        val pauseResumePending = PendingIntent.getBroadcast(
            this,
            1,
            pauseResumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneNextIntent = Intent(this, TimerActionReceiver::class.java).apply {
            action = TimerActionReceiver.ACTION_TIMER_DONE_NEXT
        }
        val doneNextPending = PendingIntent.getBroadcast(
            this,
            2,
            doneNextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = if (isPaused) "⏸️ Paused • $remaining remaining" else "$remaining remaining • Focus Running"
        val actionLabel = if (isPaused) "▶ Resume" else "⏸ Pause"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🛡️ $title")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentIntent(pendingIntent)
            .setOngoing(!isPaused)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(if (isPaused) R.drawable.ic_play else R.drawable.ic_pause, actionLabel, pauseResumePending)
            .addAction(R.drawable.ic_check, "⚡ Done & Next", doneNextPending)
            .build()
    }

    private fun postFinishedNotification(title: String, durationMinutes: Int) {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NAVIGATE_TO", "TIMER")
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, FINISH_CHANNEL_ID)
            .setContentTitle("🎉 Focus Session Completed!")
            .setContentText("$title ($durationMinutes min) successfully completed. Great work!")
            .setSmallIcon(R.drawable.ic_check)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(FINISHED_NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ongoingChannel = NotificationChannel(
                CHANNEL_ID,
                "Focus Session Guard",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps focus timers running accurately in the background"
            }

            val finishChannel = NotificationChannel(
                FINISH_CHANNEL_ID,
                "Focus Completion Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when a study focus session completes"
                enableVibration(true)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(ongoingChannel)
            manager?.createNotificationChannel(finishChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tickerJob?.cancel()
        monitorJob?.cancel()
        cancelCompletionAlarm()
        releaseWakeLock()
        OverlayBlockerManager.hideOverlay(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
