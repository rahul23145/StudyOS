package com.studyos.cainter.data.repository

import com.studyos.cainter.data.local.LectureDao
import com.studyos.cainter.data.local.StudySessionDao
import com.studyos.cainter.data.local.entity.LectureTracker
import com.studyos.cainter.data.local.entity.StudySession
import com.studyos.cainter.ui.common.DayStudyData
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

class StudyRepository(
    private val lectureDao: LectureDao,
    private val sessionDao: StudySessionDao
) {

    fun getAllLectureTrackers(): Flow<List<LectureTracker>> = lectureDao.getAllTrackers()

    fun getLectureTracker(subject: String): Flow<LectureTracker?> = lectureDao.getTracker(subject)

    suspend fun incrementLecture(subject: String) {
        lectureDao.incrementCompleted(subject)
    }

    suspend fun decrementLecture(subject: String) {
        lectureDao.decrementCompleted(subject)
    }

    suspend fun updateLectureTracker(tracker: LectureTracker) {
        lectureDao.insertOrUpdate(tracker)
    }

    suspend fun logSession(
        subject: String,
        category: String,
        durationMinutes: Int,
        notes: String = ""
    ): Long {
        val session = StudySession(
            subject = subject,
            category = category,
            durationMinutes = durationMinutes,
            timestamp = System.currentTimeMillis(),
            notes = notes
        )
        return sessionDao.insertSession(session)
    }

    fun getRecentSessions(): Flow<List<StudySession>> = sessionDao.getAllSessions()

    fun getTodayStudyMinutes(): Flow<Int> {
        val startOfToday = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return sessionDao.getTotalMinutesSince(startOfToday)
    }

    fun getWeekStudyMinutes(): Flow<Int> {
        val startOfWeek = LocalDate.now().minusDays(7)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return sessionDao.getTotalMinutesSince(startOfWeek)
    }

    suspend fun getWeeklyChartData(targetHoursPerDay: Float = 8.0f): List<DayStudyData> {
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()

        // Days from 6 days ago up to today (7 days window)
        val result = mutableListOf<DayStudyData>()
        for (i in 6 downTo 0) {
            val date = today.minusDays(i.toLong())
            val startOfDay = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val endOfDay = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

            val minutes = sessionDao.getMinutesBetween(startOfDay, endOfDay)
            val hours = minutes / 60.0f

            val dayLabel = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            result.add(
                DayStudyData(
                    dayLabel = dayLabel,
                    hours = hours,
                    targetHours = targetHoursPerDay,
                    isToday = (date == today)
                )
            )
        }
        return result
    }
}
