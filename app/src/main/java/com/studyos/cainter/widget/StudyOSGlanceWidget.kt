package com.studyos.cainter.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.studyos.cainter.R
import com.studyos.cainter.StudyOSApplication
import com.studyos.cainter.ui.MainActivity
import com.studyos.cainter.util.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class StudyOSGlanceWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val app = context.applicationContext as? StudyOSApplication ?: return
        val db = app.database

        CoroutineScope(Dispatchers.IO).launch {
            val todayDay = DateUtils.getTodayDayOfWeek()
            val activeType = db.scheduleTypeDao().getActiveScheduleTypeSync()
            val typeId = activeType?.id ?: 1L
            val items = db.scheduleDao().getScheduleForDayAndTypeSync(todayDay, typeId)

            val current = items.firstOrNull { DateUtils.isCurrentTimeBetween(it.startTime, it.endTime) }
            val next = items.firstOrNull { DateUtils.isUpcoming(it.startTime) }
            val displayItem = current ?: next ?: items.firstOrNull()

            val todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val todayEnd = todayStart + (24L * 60 * 60 * 1000)
            val todayMinutes = db.studySessionDao().getMinutesBetween(todayStart, todayEnd)
            val hours = todayMinutes / 60
            val mins = todayMinutes % 60
            val studyTimeStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

            for (widgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_studyos_glance)

                val taskTitle = displayItem?.title ?: "Free Review / Rest"
                val taskTime = displayItem?.let {
                    val durStr = if (it.durationMinutes >= 60) "${it.durationMinutes / 60}h ${it.durationMinutes % 60}m" else "${it.durationMinutes}m"
                    "⏱️ $durStr • ~${it.startTime} (Flexible)"
                } ?: "Plan your next milestone"

                views.setTextViewText(R.id.tvWidgetTaskTitle, taskTitle)
                views.setTextViewText(R.id.tvWidgetTaskTime, taskTime)
                views.setTextViewText(R.id.tvWidgetStats, "⏱️ $studyTimeStr Today")

                val openIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("NAVIGATE_TO", "TIMER")
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    widgetId,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btnWidgetOpenTimer, pendingIntent)
                views.setOnClickPendingIntent(R.id.layoutWidgetRoot, pendingIntent)

                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }
}
