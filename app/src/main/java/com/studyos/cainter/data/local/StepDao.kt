package com.studyos.cainter.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studyos.cainter.data.local.entity.StepRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface StepDao {
    @Query("SELECT * FROM daily_step_records WHERE date = :date LIMIT 1")
    fun getStepRecordForDate(date: String): Flow<StepRecord?>

    @Query("SELECT * FROM daily_step_records WHERE date = :date LIMIT 1")
    suspend fun getStepRecordForDateSync(date: String): StepRecord?

    @Query("SELECT * FROM daily_step_records ORDER BY date DESC LIMIT 7")
    fun getWeeklyStepRecords(): Flow<List<StepRecord>>

    @Query("SELECT * FROM daily_step_records ORDER BY date DESC LIMIT 7")
    suspend fun getWeeklyStepRecordsSync(): List<StepRecord>

    @Query("SELECT * FROM daily_step_records ORDER BY date DESC")
    suspend fun getAllStepRecordsSync(): List<StepRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: StepRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<StepRecord>)

    @Query("DELETE FROM daily_step_records")
    suspend fun clearAll()
}
