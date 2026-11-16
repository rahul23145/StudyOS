package com.studyos.cainter.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule_types")
data class ScheduleType(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val targetDailyHours: Float = 8.0f,
    val wakeTime: String = "07:00",
    val sleepTime: String = "23:30",
    val isActive: Boolean = false
)
