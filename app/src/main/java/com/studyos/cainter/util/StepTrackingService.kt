package com.studyos.cainter.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.studyos.cainter.R
import com.studyos.cainter.ui.MainActivity
import java.util.Locale

class StepTrackingService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 4001
        private const val CHANNEL_ID = "studyos_step_tracker_channel"
        private const val CHANNEL_NAME = "Step Tracker Background Service"
        const val ACTION_STOP_TRACKING = "com.studyos.cainter.ACTION_STOP_STEP_TRACKING"

        fun startService(context: Context) {
            val intent = Intent(context, StepTrackingService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {}
        }

        fun stopService(context: Context) {
            val intent = Intent(context, StepTrackingService::class.java)
            try {
                context.stopService(intent)
            } catch (_: Exception) {}
        }

        fun updateNotification(context: Context, steps: Int) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val notification = buildNotification(context, steps)
            try {
                notificationManager.notify(NOTIFICATION_ID, notification)
            } catch (_: Exception) {}
        }

        fun buildNotification(context: Context, steps: Int): Notification {
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val stopIntent = Intent(context, StepTrackingService::class.java).apply {
                action = ACTION_STOP_TRACKING
            }
            val stopPendingIntent = PendingIntent.getService(
                context,
                1,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            return NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_steps)
                .setContentTitle("StudyOS Step Tracker")
                .setContentText(String.format(Locale.US, "%,d steps today • Target: 8,000", steps))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(openPendingIntent)
                .addAction(0, "Turn Off", stopPendingIntent)
                .build()
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val initialSteps = StepCounterManager.getTodaySteps(this)
        val notification = buildNotification(this, initialSteps)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {
            startForeground(NOTIFICATION_ID, notification)
        }

        StepCounterManager.startListening()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_TRACKING) {
            StepCounterManager.setTrackingEnabled(this, false)
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!StepCounterManager.isTrackingEnabled(this)) {
            StepCounterManager.stopListening()
        }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background step and activity tracking notification"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
