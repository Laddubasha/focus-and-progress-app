package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habit_activities ORDER BY id ASC")
    fun getAllHabits(): Flow<List<HabitActivity>>

    @Query("SELECT * FROM habit_activities ORDER BY id ASC")
    suspend fun getAllHabitsDirect(): List<HabitActivity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitActivity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabits(habits: List<HabitActivity>)

    @Update
    suspend fun updateHabit(habit: HabitActivity)

    @Delete
    suspend fun deleteHabit(habit: HabitActivity)

    @Query("DELETE FROM habit_activities WHERE id = :id")
    suspend fun deleteHabitById(id: Long)
}
