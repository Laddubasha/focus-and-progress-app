package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfigDao {
    @Query("SELECT * FROM motivational_config WHERE id = 1 LIMIT 1")
    fun getConfig(): Flow<MotivationalConfig?>

    @Query("SELECT * FROM motivational_config WHERE id = 1 LIMIT 1")
    suspend fun getConfigDirect(): MotivationalConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: MotivationalConfig)
}
