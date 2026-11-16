package com.studyos.cainter.util

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import java.util.Calendar

data class AppUsageStat(
    val packageName: String,
    val appName: String,
    val iconDrawable: Drawable?,
    val foregroundTimeMillis: Long,
    val category: String,
    val isDistraction: Boolean
)

object AppUsageTracker {

    private val SYSTEM_IGNORE_LIST = setOf(
        "android",
        "com.android.systemui",
        "com.google.android.googlequicksearchbox",
        "com.android.launcher",
        "com.miui.home",
        "com.sec.android.app.launcher",
        "com.google.android.apps.nexuslauncher",
        "com.google.android.inputmethod.latin",
        "com.touchtype.swiftkey",
        "com.android.settings",
        "com.google.android.gms"
    )

    private val DISTRACTION_PACKAGES = setOf(
        "com.instagram.android",
        "com.google.android.youtube",
        "com.whatsapp",
        "com.reddit.frontpage",
        "com.twitter.android",
        "com.facebook.katana",
        "com.snapchat.android",
        "com.netflix.mediaclient",
        "com.discord",
        "com.zhiliaoapp.musically"
    )

    fun hasUsageAccessPermission(context: Context): Boolean {
        return ZeroDistractionGuard.hasUsagePermission(context)
    }

    fun getTodayAppUsage(context: Context): List<AppUsageStat> {
        if (!hasUsageAccessPermission(context)) return emptyList()

        val usageManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyList()

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = cal.timeInMillis
        val endTime = System.currentTimeMillis()

        val stats = usageManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: return emptyList()

        val pm = context.packageManager
        val myPackage = context.packageName
        val launcherPackage = getLauncherPackage(context)

        // Group by package in case multiple daily buckets are returned
        val aggregated = mutableMapOf<String, Long>()
        for (stat in stats) {
            val pkg = stat.packageName
            if (pkg == myPackage || pkg in SYSTEM_IGNORE_LIST || pkg == launcherPackage) continue
            val time = stat.totalTimeInForeground
            if (time > 15_000L) { // Filter out anything less than 15 seconds
                aggregated[pkg] = (aggregated[pkg] ?: 0L) + time
            }
        }

        val result = mutableListOf<AppUsageStat>()
        for ((pkg, timeMillis) in aggregated) {
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                // Filter out non-launchable system apps
                if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 && pm.getLaunchIntentForPackage(pkg) == null) {
                    continue
                }
                val label = pm.getApplicationLabel(appInfo).toString()
                val icon = pm.getApplicationIcon(appInfo)
                val isDistraction = pkg in DISTRACTION_PACKAGES

                val category = when {
                    isDistraction -> "Social & Entertainment"
                    pkg.contains("chrome") || pkg.contains("browser") || pkg.contains("firefox") -> "Browser"
                    pkg.contains("telegram") || pkg.contains("messaging") -> "Communication"
                    else -> "Productivity & Other"
                }

                result.add(
                    AppUsageStat(
                        packageName = pkg,
                        appName = label,
                        iconDrawable = icon,
                        foregroundTimeMillis = timeMillis,
                        category = category,
                        isDistraction = isDistraction
                    )
                )
            } catch (_: PackageManager.NameNotFoundException) {
                // App uninstalled
            }
        }

        return result.sortedByDescending { it.foregroundTimeMillis }
    }

    private fun getLauncherPackage(context: Context): String? {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            context.packageManager.resolveActivity(intent, 0)?.activityInfo?.packageName
        } catch (_: Exception) {
            null
        }
    }
}
