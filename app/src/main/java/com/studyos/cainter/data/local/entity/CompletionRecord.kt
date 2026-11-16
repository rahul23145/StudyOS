package com.studyos.cainter.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "completion_records",
    indices = [Index(value = ["date", "scheduleItemId"], unique = true)]
)
data class CompletionRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val scheduleItemId: Long,
    val isCompleted: Boolean = true,
    val completedAt: Long = System.currentTimeMillis()
)
