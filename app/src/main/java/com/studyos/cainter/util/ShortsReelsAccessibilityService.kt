package com.studyos.cainter.util

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import com.studyos.cainter.StudyOSApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ShortsReelsAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastScrollTime = 0L
    private var isInsideInstagramReels = false
    private var isInsideYouTubeShorts = false

    companion object {
        private const val SCROLL_DEBOUNCE_MS = 1000L

        fun isServiceEnabled(context: Context): Boolean {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabledServices.contains(context.packageName)
        }

        fun openAccessibilitySettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkg = event.packageName?.toString() ?: return
        val cls = event.className?.toString() ?: ""

        when (pkg) {
            "com.instagram.android" -> handleInstagramEvent(event, cls)
            "com.google.android.youtube" -> handleYouTubeEvent(event, cls)
        }
    }

    private fun handleInstagramEvent(event: AccessibilityEvent, className: String) {
        val eventType = event.eventType

        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            isInsideInstagramReels = className.contains("ModalActivity", ignoreCase = true) ||
                    className.contains("Clips", ignoreCase = true) ||
                    className.contains("Reel", ignoreCase = true)
        }

        if (eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            val now = System.currentTimeMillis()
            if (isInsideInstagramReels && (now - lastScrollTime) > SCROLL_DEBOUNCE_MS) {
                lastScrollTime = now
                recordReelScrolled()
            }
        }
    }

    private fun handleYouTubeEvent(event: AccessibilityEvent, className: String) {
        val eventType = event.eventType

        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            isInsideYouTubeShorts = className.contains("ReelWatchActivity", ignoreCase = true) ||
                    className.contains("Shorts", ignoreCase = true)
        }

        if (eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            val now = System.currentTimeMillis()
            if (isInsideYouTubeShorts && (now - lastScrollTime) > SCROLL_DEBOUNCE_MS) {
                lastScrollTime = now
                recordShortScrolled()
            }
        }
    }

    private fun recordReelScrolled() {
        serviceScope.launch {
            try {
                val app = applicationContext as? StudyOSApplication ?: return@launch
                app.distractionRepository.incrementReels(count = 1)
            } catch (_: Exception) {}
        }
    }

    private fun recordShortScrolled() {
        serviceScope.launch {
            try {
                val app = applicationContext as? StudyOSApplication ?: return@launch
                app.distractionRepository.incrementShorts(count = 1)
            } catch (_: Exception) {}
        }
    }

    override fun onInterrupt() {
        // Accessibility service interrupted
    }
}
