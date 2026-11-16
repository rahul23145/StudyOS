package com.studyos.cainter.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.studyos.cainter.R
import com.studyos.cainter.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FocusGuardService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var monitorJob: Job? = null

    companion object {
        const val CHANNEL_ID = "studyos_focus_guard_channel"
        const val NOTIFICATION_ID = 9001
        const val ACTION_START = "ACTION_START_FOCUS"
        const val ACTION_UPDATE = "ACTION_UPDATE_FOCUS"
        const val ACTION_STOP = "ACTION_STOP_FOCUS"

        fun startService(context: Context, taskTitle: String, remainingTime: String) {
            val intent = Intent(context, FocusGuardService::class.java).apply {
                action = ACTION_START
                putExtra("TASK_TITLE", taskTitle)
                putExtra("REMAINING_TIME", remainingTime)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun updateService(context: Context, taskTitle: String, remainingTime: String) {
            val intent = Intent(context, FocusGuardService::class.java).apply {
                action = ACTION_UPDATE
                putExtra("TASK_TITLE", taskTitle)
                putExtra("REMAINING_TIME", remainingTime)
            }
            context.startService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FocusGuardService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        val title = intent?.getStringExtra("TASK_TITLE") ?: "Deep Study Block"
        val remaining = intent?.getStringExtra("REMAINING_TIME") ?: "--:--"

        when (action) {
            ACTION_START, ACTION_UPDATE -> {
                val notification = buildNotification(title, remaining)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
                startMonitoring(title, remaining)
            }
            ACTION_STOP -> {
                monitorJob?.cancel()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startMonitoring(title: String, remaining: String) {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            while (isActive) {
                delay(500L)
                ZeroDistractionGuard.checkAndBlockForegroundApp(
                    this@FocusGuardService,
                    title,
                    remaining
                )
            }
        }
    }

    private fun buildNotification(title: String, remaining: String): Notification {
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

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🛡️ Focus Active: $title")
            .setContentText("$remaining remaining • Zero Distraction Guard Active")
            .setSmallIcon(R.drawable.ic_timer)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Focus Session Guard",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps focus timers running accurately and guards against distracting apps"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        monitorJob?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
