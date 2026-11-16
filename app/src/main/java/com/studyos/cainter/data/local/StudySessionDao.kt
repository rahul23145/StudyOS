package com.studyos.cainter.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.studyos.cainter.data.local.entity.StudySession
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySessionDao {
    @Query("SELECT * FROM study_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    fun getSessionsSince(sinceTimestamp: Long): Flow<List<StudySession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySession): Long

    @Query("SELECT COALESCE(SUM(durationMinutes), 0) FROM study_sessions WHERE timestamp >= :sinceTimestamp")
    fun getTotalMinutesSince(sinceTimestamp: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(durationMinutes), 0) FROM study_sessions WHERE timestamp >= :fromTimestamp AND timestamp < :toTimestamp")
    suspend fun getMinutesBetween(fromTimestamp: Long, toTimestamp: Long): Int

    @Query("SELECT DISTINCT strftime('%Y-%m-%d', timestamp / 1000, 'unixepoch', 'localtime') FROM study_sessions WHERE durationMinutes >= 5")
    suspend fun getAllStudyDates(): List<String>
}
