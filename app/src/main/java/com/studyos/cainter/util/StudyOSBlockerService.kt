package com.studyos.cainter.util

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.studyos.cainter.ui.distraction.BlockedAppActivity

class StudyOSBlockerService : AccessibilityService() {

    private var lastBlockedTime = 0L
    private var lastBlockedPkg = ""

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName == this.packageName) return

        val now = System.currentTimeMillis()
        if (packageName == lastBlockedPkg && (now - lastBlockedTime) < 1500L) {
            return
        }

        if (ZeroDistractionGuard.isEnabled(this) && ZeroDistractionGuard.isFocusSessionRunning(this)) {
            val blockedPackages = ZeroDistractionGuard.getBlockedPackages(this)
            if (blockedPackages.contains(packageName)) {
                lastBlockedTime = now
                lastBlockedPkg = packageName

                val appLabel = ZeroDistractionGuard.DEFAULT_APPS.firstOrNull { it.packageName == packageName }?.appName
                    ?: packageName

                val intent = Intent(this, BlockedAppActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("BLOCKED_APP_NAME", appLabel)
                    putExtra("BLOCKED_PACKAGE", packageName)
                    putExtra("TASK_TITLE", ZeroDistractionGuard.getActiveSessionTitle(this@StudyOSBlockerService))
                    putExtra("REMAINING_TIME", ZeroDistractionGuard.getActiveSessionRemainingTime(this@StudyOSBlockerService))
                }
                startActivity(intent)
            }
        }
    }

    override fun onInterrupt() {}
}
