package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SummaryDao {
    @Query("SELECT * FROM nightly_summaries ORDER BY timestamp DESC LIMIT 1")
    fun getLatestSummary(): Flow<NightlySummaryEntity?>

    @Query("SELECT * FROM nightly_summaries ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSummaryDirect(): NightlySummaryEntity?

    @Query("SELECT * FROM nightly_summaries ORDER BY timestamp DESC")
    fun getAllSummaries(): Flow<List<NightlySummaryEntity>>

    @Query("SELECT * FROM nightly_summaries WHERE dateString = :date ORDER BY timestamp DESC LIMIT 1")
    suspend fun getSummaryByDate(date: String): NightlySummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: NightlySummaryEntity): Long
}
