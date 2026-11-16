package com.studyos.cainter.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.studyos.cainter.data.local.entity.DistractionRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface DistractionDao {
    @Query("SELECT * FROM daily_distraction_records WHERE date = :date LIMIT 1")
    fun getRecordForDate(date: String): Flow<DistractionRecord?>

    @Query("SELECT * FROM daily_distraction_records WHERE date = :date LIMIT 1")
    suspend fun getRecordForDateSync(date: String): DistractionRecord?

    @Query("SELECT * FROM daily_distraction_records ORDER BY date DESC LIMIT 7")
    suspend fun getWeeklyRecordsSync(): List<DistractionRecord>

    @Query("SELECT * FROM daily_distraction_records ORDER BY date DESC")
    suspend fun getAllRecordsSync(): List<DistractionRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: DistractionRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<DistractionRecord>)

    @Query("DELETE FROM daily_distraction_records")
    suspend fun clearAll()
}
