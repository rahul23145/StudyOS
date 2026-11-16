package com.studyos.cainter.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TimerActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_TIMER_PAUSE_RESUME = "com.studyos.cainter.ACTION_TIMER_PAUSE_RESUME"
        const val ACTION_TIMER_DONE_NEXT = "com.studyos.cainter.ACTION_TIMER_DONE_NEXT"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            ACTION_TIMER_PAUSE_RESUME -> {
                TimerController.triggerPauseResume()
            }
            ACTION_TIMER_DONE_NEXT -> {
                TimerController.triggerDoneNext()
            }
        }
    }
}
