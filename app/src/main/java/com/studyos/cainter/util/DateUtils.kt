package com.studyos.cainter.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
    private val displayDateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.US)
    val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    private val timeFormatter12: DateTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)

    /**
     * Converts a 24-hour time string ("13:00", "07:45") or existing string into clean 12-hour AM/PM format ("01:00 PM", "07:45 AM").
     */
    fun formatTo12Hour(timeStr: String?): String {
        if (timeStr.isNullOrBlank()) return ""
        val trimmed = timeStr.trim()
        if (trimmed.contains("AM", ignoreCase = true) || trimmed.contains("PM", ignoreCase = true)) {
            return trimmed
        }
        return try {
            val parsed = LocalTime.parse(trimmed, timeFormatter)
            parsed.format(timeFormatter12)
        } catch (_: Exception) {
            try {
                val parts = trimmed.split(":")
                val h = parts[0].toInt()
                val m = if (parts.size > 1) parts[1].take(2).toInt() else 0
                val amPm = if (h < 12) "AM" else "PM"
                val h12 = when {
                    h == 0 -> 12
                    h > 12 -> h - 12
                    else -> h
                }
                String.format(Locale.US, "%02d:%02d %s", h12, m, amPm)
            } catch (_: Exception) {
                trimmed
            }
        }
    }

    /**
     * Converts any 12-hour AM/PM or 24-hour time string to standard normalized 24-hour "HH:mm" for database storage.
     */
    fun to24Hour(timeStr: String?): String {
        if (timeStr.isNullOrBlank()) return "09:00"
        val trimmed = timeStr.trim()
        return try {
            if (trimmed.contains("AM", ignoreCase = true) || trimmed.contains("PM", ignoreCase = true)) {
                val parsed = LocalTime.parse(trimmed.uppercase(Locale.US), timeFormatter12)
                parsed.format(timeFormatter)
            } else {
                val parsed = LocalTime.parse(trimmed, timeFormatter)
                parsed.format(timeFormatter)
            }
        } catch (_: Exception) {
            try {
                val isPm = trimmed.contains("PM", ignoreCase = true)
                val clean = trimmed.replace("AM", "", ignoreCase = true).replace("PM", "", ignoreCase = true).trim()
                val parts = clean.split(":")
                var h = parts[0].toInt()
                val m = if (parts.size > 1) parts[1].toInt() else 0
                if (isPm && h < 12) h += 12
                if (!isPm && h == 12) h = 0
                String.format(Locale.US, "%02d:%02d", h, m)
            } catch (_: Exception) {
                trimmed
            }
        }
    }

    fun formatTimeRange12Hour(start: String?, end: String?): String {
        val s = formatTo12Hour(start)
        val e = formatTo12Hour(end)
        return if (s.isNotBlank() && e.isNotBlank()) "$s – $e" else s.ifBlank { e }
    }

    fun getRemainingSecondsUntil(timeStr: String): Long {
        return try {
            val now = LocalTime.now()
            val target = LocalTime.parse(timeStr, timeFormatter)
            if (target.isAfter(now)) {
                java.time.temporal.ChronoUnit.SECONDS.between(now, target).coerceAtLeast(0)
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    fun getTodayDateString(): String {
        return LocalDate.now().format(dateFormatter)
    }

    fun getFormattedDisplayDate(): String {
        return LocalDate.now().format(displayDateFormatter)
    }

    /**
     * Returns 1 for Monday, 2 for Tuesday, ..., 7 for Sunday
     */
    fun getTodayDayOfWeek(): Int {
        return LocalDate.now().dayOfWeek.value
    }

    fun getDayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            1 -> "Monday"
            2 -> "Tuesday"
            3 -> "Wednesday"
            4 -> "Thursday"
            5 -> "Friday"
            6 -> "Saturday"
            7 -> "Sunday"
            else -> "Day $dayOfWeek"
        }
    }

    fun formatMinutesToHoursAndMinutes(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0 && minutes > 0) {
            "${hours}h ${minutes}m"
        } else if (hours > 0) {
            "${hours}h"
        } else {
            "${minutes}m"
        }
    }

    fun isCurrentTimeBetween(start: String, end: String): Boolean {
        return try {
            val now = LocalTime.now()
            val startTime = LocalTime.parse(start, timeFormatter)
            val endTime = LocalTime.parse(end, timeFormatter)
            if (startTime.isBefore(endTime)) {
                !now.isBefore(startTime) && now.isBefore(endTime)
            } else {
                // Overnight activity (e.g., Sleep 23:30 to 07:00)
                !now.isBefore(startTime) || now.isBefore(endTime)
            }
        } catch (e: Exception) {
            false
        }
    }

    fun isUpcoming(start: String): Boolean {
        return try {
            val now = LocalTime.now()
            val startTime = LocalTime.parse(start, timeFormatter)
            startTime.isAfter(now)
        } catch (e: Exception) {
            false
        }
    }
}
