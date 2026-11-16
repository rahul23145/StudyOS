package com.studyos.cainter.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lecture_tracker")
data class LectureTracker(
    @PrimaryKey
    val subject: String, // "COSTING" or "AUDIT"
    val displayName: String,
    val totalLectures: Int,
    val completedLectures: Int,
    val dailyTarget: Int, // Costing: 2, Audit: 2 (Mon-Sat)
    val lectureDurationMinutes: Int, // Costing: 138 (2h 18m), Audit: 90 (1h 30m)
    val lastUpdatedDate: String = ""
)
