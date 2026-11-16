package com.studyos.cainter.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.studyos.cainter.data.local.entity.CompletionRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface CompletionDao {
    @Query("SELECT * FROM completion_records WHERE date = :date")
    fun getCompletionsForDate(date: String): Flow<List<CompletionRecord>>

    @Query("SELECT * FROM completion_records WHERE date = :date AND scheduleItemId = :scheduleItemId LIMIT 1")
    suspend fun getCompletion(date: String, scheduleItemId: Long): CompletionRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCompletion(record: CompletionRecord)

    @Query("DELETE FROM completion_records WHERE date = :date AND scheduleItemId = :scheduleItemId")
    suspend fun removeCompletion(date: String, scheduleItemId: Long)

    @Query("SELECT COUNT(DISTINCT date) FROM completion_records WHERE isCompleted = 1")
    fun getTotalActiveDays(): Flow<Int>

    @Query("SELECT DISTINCT date FROM completion_records WHERE isCompleted = 1 ORDER BY date DESC")
    suspend fun getAllCompletedDates(): List<String>
}
