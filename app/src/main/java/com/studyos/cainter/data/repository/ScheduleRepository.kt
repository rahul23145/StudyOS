package com.studyos.cainter.data.repository

import com.studyos.cainter.data.default.DefaultScheduleProvider
import com.studyos.cainter.data.local.CompletionDao
import com.studyos.cainter.data.local.ScheduleDao
import com.studyos.cainter.data.local.entity.CompletionRecord
import com.studyos.cainter.data.local.entity.ScheduleItem
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ScheduleRepository(
    private val scheduleDao: ScheduleDao,
    private val completionDao: CompletionDao
) {

    fun getScheduleForDay(dayOfWeek: Int): Flow<List<ScheduleItem>> {
        return scheduleDao.getScheduleForDay(dayOfWeek)
    }

    fun getAllSchedule(): Flow<List<ScheduleItem>> {
        return scheduleDao.getAllScheduleItems()
    }

    suspend fun insertItem(item: ScheduleItem): Long {
        return scheduleDao.insertItem(item)
    }

    suspend fun updateItem(item: ScheduleItem) {
        scheduleDao.updateItem(item)
    }

    suspend fun deleteItem(item: ScheduleItem) {
        scheduleDao.deleteItem(item)
    }

    suspend fun resetToDefaultSchedule() {
        scheduleDao.clearAll()
        scheduleDao.insertAll(DefaultScheduleProvider.getDefaultSchedule())
    }

    fun getCompletionsForDate(date: String): Flow<List<CompletionRecord>> {
        return completionDao.getCompletionsForDate(date)
    }

    suspend fun setCompletion(date: String, scheduleItemId: Long, isCompleted: Boolean) {
        if (isCompleted) {
            completionDao.saveCompletion(
                CompletionRecord(
                    date = date,
                    scheduleItemId = scheduleItemId,
                    isCompleted = true,
                    completedAt = System.currentTimeMillis()
                )
            )
        } else {
            completionDao.removeCompletion(date, scheduleItemId)
        }
    }

    suspend fun calculateStreak(): Int {
        val completedDates = completionDao.getAllCompletedDates()
        if (completedDates.isEmpty()) return 0

        var streak = 0
        var checkDate = LocalDate.now()
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE

        // Check if today is completed or yesterday
        val dateSet = completedDates.toSet()
        if (!dateSet.contains(checkDate.format(formatter))) {
            checkDate = checkDate.minusDays(1)
        }

        while (dateSet.contains(checkDate.format(formatter))) {
            streak++
            checkDate = checkDate.minusDays(1)
        }

        return streak
    }
}
