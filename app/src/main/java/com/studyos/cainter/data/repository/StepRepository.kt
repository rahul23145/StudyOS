package com.studyos.cainter.data.repository

import com.studyos.cainter.data.local.StepDao
import com.studyos.cainter.data.local.entity.StepRecord
import com.studyos.cainter.util.DateUtils
import kotlinx.coroutines.flow.Flow

class StepRepository(private val stepDao: StepDao) {

    fun getTodayStepRecord(todayDate: String = DateUtils.getTodayDateString()): Flow<StepRecord?> {
        return stepDao.getStepRecordForDate(todayDate)
    }

    suspend fun getTodayStepRecordSync(todayDate: String = DateUtils.getTodayDateString()): StepRecord {
        return stepDao.getStepRecordForDateSync(todayDate) ?: StepRecord(date = todayDate)
    }

    suspend fun recordSteps(
        totalSteps: Int,
        walkingSteps: Int,
        runningSteps: Int,
        distanceMeters: Float,
        caloriesBurned: Float,
        todayDate: String = DateUtils.getTodayDateString()
    ) {
        val record = StepRecord(
            date = todayDate,
            totalSteps = totalSteps,
            walkingSteps = walkingSteps,
            runningSteps = runningSteps,
            distanceMeters = distanceMeters,
            caloriesBurned = caloriesBurned,
            lastUpdatedTimestamp = System.currentTimeMillis()
        )
        stepDao.insertOrUpdate(record)
    }

    suspend fun getWeeklyRecordsSync(): List<StepRecord> {
        return stepDao.getWeeklyStepRecordsSync()
    }
}
