package com.studyos.cainter.data.repository

import com.studyos.cainter.BuildConfig
import com.studyos.cainter.data.default.DefaultScheduleProvider
import com.studyos.cainter.data.local.CompletionDao
import com.studyos.cainter.data.local.ScheduleDao
import com.studyos.cainter.data.local.ScheduleTypeDao
import com.studyos.cainter.data.local.StudySessionDao
import com.studyos.cainter.data.local.entity.CompletionRecord
import com.studyos.cainter.data.local.entity.ScheduleItem
import com.studyos.cainter.data.local.entity.ScheduleType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ScheduleRepository(
    private val scheduleDao: ScheduleDao,
    private val completionDao: CompletionDao,
    private val scheduleTypeDao: ScheduleTypeDao,
    private val studySessionDao: StudySessionDao
) {

    fun getScheduleForDay(dayOfWeek: Int): Flow<List<ScheduleItem>> {
        return scheduleDao.getScheduleForDay(dayOfWeek)
    }

    fun getScheduleForDay(dayOfWeek: Int, scheduleTypeId: Long): Flow<List<ScheduleItem>> {
        return scheduleDao.getScheduleForDayAndType(dayOfWeek, scheduleTypeId)
    }

    suspend fun getScheduleForDaySync(dayOfWeek: Int, scheduleTypeId: Long): List<ScheduleItem> {
        return scheduleDao.getScheduleForDayAndTypeSync(dayOfWeek, scheduleTypeId)
    }

    fun getAllSchedule(): Flow<List<ScheduleItem>> {
        return scheduleDao.getAllScheduleItems()
    }

    fun getAllScheduleForType(scheduleTypeId: Long): Flow<List<ScheduleItem>> {
        return scheduleDao.getAllScheduleItemsForType(scheduleTypeId)
    }

    suspend fun insertItem(item: ScheduleItem): Long {
        return scheduleDao.insertItem(item)
    }

    suspend fun updateItem(item: ScheduleItem) {
        scheduleDao.updateItem(item)
    }

    suspend fun deleteItem(item: ScheduleItem) {
        scheduleDao.deleteItemById(item.id)
    }

    suspend fun deleteItemById(id: Long) {
        scheduleDao.deleteItemById(id)
    }

    suspend fun removeDuplicates() {
        scheduleDao.removeDuplicates()
    }

    suspend fun resetToDefaultSchedule() {
        val activeId = getActiveScheduleTypeSync()?.id ?: 1L
        resetScheduleForType(activeId)
    }

    suspend fun resetScheduleForType(typeId: Long) {
        scheduleDao.clearForType(typeId)
        val defaultItems = DefaultScheduleProvider.getScheduleForType(typeId)
        scheduleDao.insertAll(defaultItems)
    }

    fun getCompletionsForDate(date: String): Flow<List<CompletionRecord>> {
        return completionDao.getCompletionsForDate(date)
    }

    suspend fun getCompletionsForDateSync(date: String): List<CompletionRecord> {
        return completionDao.getCompletionsForDateSync(date)
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

    fun getAllScheduleTypes(): Flow<List<ScheduleType>> = flow {
        if (scheduleTypeDao.getScheduleTypeCount() == 0) {
            scheduleTypeDao.insertAll(DefaultScheduleProvider.getDefaultScheduleTypes(BuildConfig.IS_CUSTOM_APP))
        }
        emitAll(scheduleTypeDao.getAllScheduleTypes())
    }

    fun getActiveScheduleType(): Flow<ScheduleType?> {
        return scheduleTypeDao.getActiveScheduleType()
    }

    suspend fun getActiveScheduleTypeSync(): ScheduleType? {
        return scheduleTypeDao.getActiveScheduleTypeSync()
            ?: scheduleTypeDao.getAllScheduleTypesSync().firstOrNull()
    }

    suspend fun createScheduleType(scheduleType: ScheduleType): Long {
        val id = scheduleTypeDao.insert(scheduleType)
        val starterSchedule = DefaultScheduleProvider.createStarterScheduleForType(
            id,
            scheduleType.wakeTime,
            scheduleType.sleepTime
        )
        scheduleDao.insertAll(starterSchedule)
        return id
    }

    suspend fun deleteScheduleType(scheduleType: ScheduleType) {
        scheduleTypeDao.delete(scheduleType)
        scheduleDao.clearForType(scheduleType.id)
    }

    suspend fun setActiveScheduleType(typeId: Long) {
        scheduleTypeDao.clearActiveStatus()
        scheduleTypeDao.setActiveStatus(typeId)
        if (scheduleDao.getScheduleCountForType(typeId) == 0) {
            val items = DefaultScheduleProvider.getScheduleForType(typeId)
            scheduleDao.insertAll(items)
        }
    }

    suspend fun calculateStreak(): Int {
        val completedTaskDates = completionDao.getAllCompletedDates()
        val studyDates = studySessionDao.getAllStudyDates()
        val allActiveDates = (completedTaskDates + studyDates).toSet()
        if (allActiveDates.isEmpty()) return 0

        var streak = 0
        var checkDate = LocalDate.now()
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE

        // Check if today is completed or yesterday
        if (!allActiveDates.contains(checkDate.format(formatter))) {
            checkDate = checkDate.minusDays(1)
        }

        while (allActiveDates.contains(checkDate.format(formatter))) {
            streak++
            checkDate = checkDate.minusDays(1)
        }

        return streak
    }
}
