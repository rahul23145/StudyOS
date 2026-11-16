package com.studyos.cainter.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_entries")
data class WorkoutEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // Format: YYYY-MM-DD
    val timestamp: Long = System.currentTimeMillis(),
    val muscleGroup: String, // Chest, Back, Legs, Shoulders, Arms, Core, Cardio, Full Body
    val exerciseName: String, // e.g. Bench Press, Squats
    val sets: Int = 3,
    val reps: Int = 10,
    val weightKg: Float = 0f,
    val notes: String = ""
)
