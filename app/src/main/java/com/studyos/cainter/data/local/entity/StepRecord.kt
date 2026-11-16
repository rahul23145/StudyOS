package com.studyos.cainter.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_step_records")
data class StepRecord(
    @PrimaryKey
    val date: String, // "YYYY-MM-DD"
    val totalSteps: Int = 0,
    val walkingSteps: Int = 0,
    val runningSteps: Int = 0,
    val distanceMeters: Float = 0f,
    val caloriesBurned: Float = 0f,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
)
