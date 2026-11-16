package com.studyos.cainter.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.studyos.cainter.ui.distraction.BlockedAppActivity

data class DistractingAppInfo(
    val packageName: String,
    val appName: String,
    val isBlocked: Boolean
)

object ZeroDistractionGuard {
    private const val PREFS_NAME = "studyos_zero_distraction_prefs"
    private const val KEY_ENABLED = "key_distraction_guard_enabled"
    private const val KEY_BLOCKED_PACKAGES = "key_blocked_packages"
    private const val KEY_SESSION_ACTIVE = "key_session_active"
    private const val KEY_SESSION_TITLE = "key_session_title"
    private const val KEY_SESSION_REMAINING = "key_session_remaining"

    val DEFAULT_APPS = listOf(
        DistractingAppInfo("com.instagram.android", "Instagram", true),
        DistractingAppInfo("com.google.android.youtube", "YouTube", true),
        DistractingAppInfo("com.whatsapp", "WhatsApp", true),
        DistractingAppInfo("com.reddit.frontpage", "Reddit", true),
        DistractingAppInfo("com.twitter.android", "X (Twitter)", true),
        DistractingAppInfo("com.facebook.katana", "Facebook", true),
        DistractingAppInfo("com.snapchat.android", "Snapchat", true),
        DistractingAppInfo("com.netflix.mediaclient", "Netflix", true),
        DistractingAppInfo("com.discord", "Discord", true),
        DistractingAppInfo("com.zhiliaoapp.musically", "TikTok", true),
        DistractingAppInfo("com.android.chrome", "Chrome Browser", false)
    )

    fun hasAccessibilityPermission(context: Context): Boolean {
        return try {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
            val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            enabledServices.any { it.resolveInfo?.serviceInfo?.packageName == context.packageName }
        } catch (e: Exception) {
            false
        }
    }

    fun openAccessibilitySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            openUsageSettings(context)
        }
    }

    fun hasUsagePermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    fun openUsageSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    fun isEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ENABLED, false)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }

    fun setSessionActive(context: Context, active: Boolean, title: String = "", remaining: String = "") {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SESSION_ACTIVE, active)
            .putString(KEY_SESSION_TITLE, title)
            .putString(KEY_SESSION_REMAINING, remaining)
            .apply()
    }

    fun isFocusSessionRunning(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SESSION_ACTIVE, false)
    }

    fun getActiveSessionTitle(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SESSION_TITLE, "Deep Focus Session") ?: "Deep Focus Session"
    }

    fun getActiveSessionRemainingTime(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SESSION_REMAINING, "--:--") ?: "--:--"
    }

    fun getBlockedPackages(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getStringSet(KEY_BLOCKED_PACKAGES, null)
        if (saved != null) return saved
        return DEFAULT_APPS.filter { it.isBlocked }.map { it.packageName }.toSet()
    }

    fun setBlockedPackages(context: Context, packages: Set<String>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_BLOCKED_PACKAGES, packages)
            .apply()
    }

    fun checkAndBlockForegroundApp(
        context: Context,
        taskTitle: String,
        remainingTime: String
    ): Boolean {
        if (!isEnabled(context)) return false

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return false
        val now = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(now - 3000, now)
        val event = UsageEvents.Event()
        var latestForegroundPackage: String? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                latestForegroundPackage = event.packageName
            }
        }

        if (latestForegroundPackage != null && latestForegroundPackage != context.packageName) {
            val blocked = getBlockedPackages(context)
            if (blocked.contains(latestForegroundPackage)) {
                val appLabel = DEFAULT_APPS.firstOrNull { it.packageName == latestForegroundPackage }?.appName
                    ?: latestForegroundPackage

                val intent = Intent(context, BlockedAppActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("BLOCKED_APP_NAME", appLabel)
                    putExtra("BLOCKED_PACKAGE", latestForegroundPackage)
                    putExtra("TASK_TITLE", taskTitle)
                    putExtra("REMAINING_TIME", remainingTime)
                }
                context.startActivity(intent)
                return true
            }
        }
        return false
    }
}
