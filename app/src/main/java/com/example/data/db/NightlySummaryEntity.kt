package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nightly_summaries")
data class NightlySummaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateString: String,
    val summaryText: String,
    val totalActivities: Int,
    val completedActivities: Int,
    val completionPercentage: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)
