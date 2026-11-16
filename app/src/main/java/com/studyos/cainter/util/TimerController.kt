package com.studyos.cainter.util

object TimerController {
    var onPauseResumeListener: (() -> Unit)? = null
    var onDoneNextListener: (() -> Unit)? = null

    fun triggerPauseResume() {
        onPauseResumeListener?.invoke()
    }

    fun triggerDoneNext() {
        onDoneNextListener?.invoke()
    }
}
