package com.studyos.cainter.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_sessions")
data class StudySession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subject: String,
    val category: String,
    val durationMinutes: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)
