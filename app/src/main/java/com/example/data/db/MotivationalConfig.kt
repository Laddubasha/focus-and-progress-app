package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "motivational_config")
data class MotivationalConfig(
    @PrimaryKey val id: Int = 1,
    val customMessage: String = "Stay focused on your journey today!",
    val nextQuoteIndex: Int = 0
)
