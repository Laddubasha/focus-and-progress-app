package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        MotivationalConfig::class,
        MotivationalQuote::class,
        HabitActivity::class,
        NightlySummaryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun configDao(): ConfigDao
    abstract fun quoteDao(): QuoteDao
    abstract fun habitDao(): HabitDao
    abstract fun summaryDao(): SummaryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "focus_progress_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }

            private suspend fun populateInitialData(db: AppDatabase) {
                db.configDao().saveConfig(
                    MotivationalConfig(
                        id = 1,
                        customMessage = "Small daily steps lead to massive long-term results!"
                    )
                )

                val defaultQuotes = listOf(
                    MotivationalQuote(quoteText = "The secret of getting ahead is getting started.", author = "Mark Twain"),
                    MotivationalQuote(quoteText = "Do what you can, with what you have, where you are.", author = "Teddy Roosevelt"),
                    MotivationalQuote(quoteText = "Consistency is what transforms average into excellence.", author = "Anonymous"),
                    MotivationalQuote(quoteText = "Don't count the days, make the days count.", author = "Muhammad Ali")
                )
                db.quoteDao().insertQuotes(defaultQuotes)

                val defaultHabits = listOf(
                    HabitActivity(
                        name = "Reading",
                        target = "30 mins",
                        currentProgress = "20 mins",
                        targetValue = 30.0,
                        progressValue = 20.0,
                        unit = "mins",
                        isCompleted = false
                    ),
                    HabitActivity(
                        name = "Python Practice",
                        target = "1 module",
                        currentProgress = "1 module",
                        targetValue = 1.0,
                        progressValue = 1.0,
                        unit = "module",
                        isCompleted = true
                    ),
                    HabitActivity(
                        name = "Workouts",
                        target = "45 mins",
                        currentProgress = "30 mins",
                        targetValue = 45.0,
                        progressValue = 30.0,
                        unit = "mins",
                        isCompleted = false
                    )
                )
                db.habitDao().insertHabits(defaultHabits)
            }
        }
    }
}
