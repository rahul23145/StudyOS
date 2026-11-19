package com.studyos.cainter.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TimerActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_TIMER_PAUSE_RESUME = "com.studyos.cainter.ACTION_TIMER_PAUSE_RESUME"
        const val ACTION_TIMER_DONE_NEXT = "com.studyos.cainter.ACTION_TIMER_DONE_NEXT"
        const val ACTION_TIMER_ALARM = "com.studyos.cainter.ACTION_TIMER_ALARM"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val ctx = context ?: return
        when (intent?.action) {
            ACTION_TIMER_PAUSE_RESUME -> {
                FocusGuardService.pauseOrResume(ctx)
            }
            ACTION_TIMER_DONE_NEXT -> {
                FocusGuardService.doneAndNext(ctx)
            }
            ACTION_TIMER_ALARM -> {
                FocusGuardService.onAlarmTriggered(ctx)
            }
        }
    }
}
