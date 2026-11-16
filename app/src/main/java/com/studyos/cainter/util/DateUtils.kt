package com.studyos.cainter.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.getDefault())
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

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
