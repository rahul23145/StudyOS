package com.studyos.cainter.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule_items")
data class ScheduleItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val scheduleTypeId: Long = 1L,
    val dayOfWeek: Int, // 1 = Monday, ..., 7 = Sunday
    val title: String,
    val category: String, // from ScheduleCategory enum name
    val startTime: String, // HH:mm format
    val endTime: String,   // HH:mm format
    val durationMinutes: Int,
    val notes: String = "",
    val sortOrder: Int = 0,
    val isCustom: Boolean = false
) {
    fun getCategoryEnum(): ScheduleCategory = ScheduleCategory.fromString(category)
}
