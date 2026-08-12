package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habit_activities")
data class HabitActivity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val target: String,
    val currentProgress: String = "0",
    val targetValue: Double = 1.0,
    val progressValue: Double = 0.0,
    val unit: String = "",
    val isCompleted: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)
