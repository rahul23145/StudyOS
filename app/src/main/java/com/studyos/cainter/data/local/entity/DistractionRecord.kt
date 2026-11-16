package com.studyos.cainter.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_distraction_records")
data class DistractionRecord(
    @PrimaryKey
    val date: String, // "YYYY-MM-DD"
    val reelsCount: Int = 0,
    val shortsCount: Int = 0,
    val totalReelsSeconds: Long = 0L,
    val totalShortsSeconds: Long = 0L,
    val otherAppsSeconds: Long = 0L
)
