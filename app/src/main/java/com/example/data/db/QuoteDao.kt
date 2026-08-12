package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QuoteDao {
    @Query("SELECT * FROM motivational_quotes ORDER BY id ASC")
    fun getAllQuotes(): Flow<List<MotivationalQuote>>

    @Query("SELECT * FROM motivational_quotes ORDER BY id ASC")
    suspend fun getAllQuotesDirect(): List<MotivationalQuote>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuote(quote: MotivationalQuote): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuotes(quotes: List<MotivationalQuote>)

    @Delete
    suspend fun deleteQuote(quote: MotivationalQuote)

    @Query("DELETE FROM motivational_quotes WHERE id = :id")
    suspend fun deleteQuoteById(id: Long)
}
