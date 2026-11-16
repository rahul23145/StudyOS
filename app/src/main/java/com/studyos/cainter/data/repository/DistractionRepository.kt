package com.studyos.cainter.data.repository

import com.studyos.cainter.data.local.DistractionDao
import com.studyos.cainter.data.local.entity.DistractionRecord
import com.studyos.cainter.util.DateUtils
import kotlinx.coroutines.flow.Flow

class DistractionRepository(private val distractionDao: DistractionDao) {

    fun getTodayDistractionRecord(todayDate: String = DateUtils.getTodayDateString()): Flow<DistractionRecord?> {
        return distractionDao.getRecordForDate(todayDate)
    }

    suspend fun getTodayRecordSync(todayDate: String = DateUtils.getTodayDateString()): DistractionRecord {
        return distractionDao.getRecordForDateSync(todayDate) ?: DistractionRecord(date = todayDate)
    }

    suspend fun incrementReels(count: Int = 1, addedSeconds: Long = 0L, todayDate: String = DateUtils.getTodayDateString()) {
        val current = getTodayRecordSync(todayDate)
        val updated = current.copy(
            reelsCount = current.reelsCount + count,
            totalReelsSeconds = current.totalReelsSeconds + addedSeconds
        )
        distractionDao.insertOrUpdate(updated)
    }

    suspend fun incrementShorts(count: Int = 1, addedSeconds: Long = 0L, todayDate: String = DateUtils.getTodayDateString()) {
        val current = getTodayRecordSync(todayDate)
        val updated = current.copy(
            shortsCount = current.shortsCount + count,
            totalShortsSeconds = current.totalShortsSeconds + addedSeconds
        )
        distractionDao.insertOrUpdate(updated)
    }

    suspend fun updateOtherAppsSeconds(seconds: Long, todayDate: String = DateUtils.getTodayDateString()) {
        val current = getTodayRecordSync(todayDate)
        val updated = current.copy(otherAppsSeconds = seconds)
        distractionDao.insertOrUpdate(updated)
    }

    suspend fun getWeeklyRecordsSync(): List<DistractionRecord> {
        return distractionDao.getWeeklyRecordsSync()
    }
}
