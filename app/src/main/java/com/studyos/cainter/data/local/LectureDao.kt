package com.studyos.cainter.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studyos.cainter.data.local.entity.LectureTracker
import kotlinx.coroutines.flow.Flow

@Dao
interface LectureDao {
    @Query("SELECT * FROM lecture_tracker")
    fun getAllTrackers(): Flow<List<LectureTracker>>

    @Query("SELECT * FROM lecture_tracker WHERE subject = :subject LIMIT 1")
    fun getTracker(subject: String): Flow<LectureTracker?>

    @Query("SELECT * FROM lecture_tracker WHERE subject = :subject LIMIT 1")
    suspend fun getTrackerSync(subject: String): LectureTracker?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(tracker: LectureTracker)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(trackers: List<LectureTracker>)

    @Query("UPDATE lecture_tracker SET completedLectures = completedLectures + 1 WHERE subject = :subject")
    suspend fun incrementCompleted(subject: String)

    @Query("UPDATE lecture_tracker SET completedLectures = CASE WHEN completedLectures > 0 THEN completedLectures - 1 ELSE 0 END WHERE subject = :subject")
    suspend fun decrementCompleted(subject: String)

    @Query("SELECT COUNT(*) FROM lecture_tracker")
    suspend fun getTrackerCount(): Int
}
