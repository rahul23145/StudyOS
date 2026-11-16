package com.studyos.cainter.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studyos.cainter.data.local.entity.WorkoutEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Query("SELECT * FROM workout_entries ORDER BY timestamp DESC")
    fun getAllWorkouts(): Flow<List<WorkoutEntry>>

    @Query("SELECT * FROM workout_entries WHERE date = :date ORDER BY timestamp DESC")
    fun getWorkoutsForDate(date: String): Flow<List<WorkoutEntry>>

    @Query("SELECT * FROM workout_entries WHERE date = :date ORDER BY timestamp DESC")
    suspend fun getWorkoutsForDateSync(date: String): List<WorkoutEntry>

    @Query("SELECT COUNT(*) FROM workout_entries WHERE date = :date")
    fun getTodayWorkoutCount(date: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(entry: WorkoutEntry): Long

    @Update
    suspend fun updateWorkout(entry: WorkoutEntry)

    @Delete
    suspend fun deleteWorkout(entry: WorkoutEntry)

    @Query("DELETE FROM workout_entries WHERE id = :id")
    suspend fun deleteWorkoutById(id: Long)

    @Query("SELECT COUNT(DISTINCT date) FROM workout_entries")
    fun getTotalWorkoutDaysCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM workout_entries")
    fun getTotalExercisesLoggedCount(): Flow<Int>

    @Query("SELECT * FROM workout_entries")
    suspend fun getAllWorkoutsSync(): List<WorkoutEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkouts(entries: List<WorkoutEntry>)
}
