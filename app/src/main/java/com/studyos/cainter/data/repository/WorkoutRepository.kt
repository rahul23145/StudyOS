package com.studyos.cainter.data.repository

import com.studyos.cainter.data.local.WorkoutDao
import com.studyos.cainter.data.local.entity.WorkoutEntry
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(private val workoutDao: WorkoutDao) {

    fun getAllWorkouts(): Flow<List<WorkoutEntry>> = workoutDao.getAllWorkouts()

    fun getWorkoutsForDate(date: String): Flow<List<WorkoutEntry>> = workoutDao.getWorkoutsForDate(date)

    suspend fun getWorkoutsForDateSync(date: String): List<WorkoutEntry> = workoutDao.getWorkoutsForDateSync(date)

    fun getTodayWorkoutCount(date: String): Flow<Int> = workoutDao.getTodayWorkoutCount(date)

    fun getTotalWorkoutDays(): Flow<Int> = workoutDao.getTotalWorkoutDaysCount()

    fun getTotalExercisesLogged(): Flow<Int> = workoutDao.getTotalExercisesLoggedCount()

    suspend fun logExercise(
        date: String,
        muscleGroup: String,
        exerciseName: String,
        sets: Int,
        reps: Int,
        weightKg: Float,
        notes: String = ""
    ): Long {
        val entry = WorkoutEntry(
            date = date,
            timestamp = System.currentTimeMillis(),
            muscleGroup = muscleGroup,
            exerciseName = exerciseName,
            sets = sets,
            reps = reps,
            weightKg = weightKg,
            notes = notes
        )
        return workoutDao.insertWorkout(entry)
    }

    suspend fun deleteExercise(id: Long) = workoutDao.deleteWorkoutById(id)
}
