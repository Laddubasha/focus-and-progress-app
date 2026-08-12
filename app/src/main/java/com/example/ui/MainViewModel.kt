package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.db.AppDatabase
import com.example.data.db.HabitActivity
import com.example.data.db.MotivationalQuote
import com.example.data.db.NightlySummaryEntity
import com.example.data.repository.FocusRepository
import com.example.worker.MotivationalReminderWorker
import com.example.worker.NightlySummaryWorker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = FocusRepository(db)

    val customConfig = repository.config.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val quotes: StateFlow<List<MotivationalQuote>> = repository.quotes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val habits: StateFlow<List<HabitActivity>> = repository.habits.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val latestSummary: StateFlow<NightlySummaryEntity?> = repository.latestSummary.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val allSummaries: StateFlow<List<NightlySummaryEntity>> = repository.allSummaries.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            repository.checkAndResetDailyProgressIfNeeded()
            scheduleWorkManagerTasks()
        }
    }

    fun saveCustomMessage(message: String) {
        viewModelScope.launch {
            repository.saveCustomMessage(message)
        }
    }

    fun addQuote(text: String, author: String = "") {
        viewModelScope.launch {
            repository.addQuote(text, author)
        }
    }

    fun deleteQuote(quote: MotivationalQuote) {
        viewModelScope.launch {
            repository.deleteQuote(quote)
        }
    }

    fun addHabit(name: String, target: String, currentProgress: String = "0") {
        viewModelScope.launch {
            repository.addHabit(name, target, currentProgress)
        }
    }

    fun updateHabitProgress(habit: HabitActivity, newProgressStr: String) {
        viewModelScope.launch {
            repository.updateHabitProgress(habit, newProgressStr)
        }
    }

    fun updateHabitTarget(habit: HabitActivity, newTargetStr: String) {
        viewModelScope.launch {
            repository.updateHabitTarget(habit, newTargetStr)
        }
    }

    fun toggleHabitCompletion(habit: HabitActivity, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.toggleHabitCompletion(habit, isCompleted)
        }
    }

    fun deleteHabit(habit: HabitActivity) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
        }
    }

    fun triggerImmediateReminder() {
        viewModelScope.launch {
            val workRequest = OneTimeWorkRequestBuilder<MotivationalReminderWorker>().build()
            WorkManager.getInstance(getApplication()).enqueue(workRequest)
        }
    }

    fun triggerImmediateNightlySummary() {
        viewModelScope.launch {
            val workRequest = OneTimeWorkRequestBuilder<NightlySummaryWorker>().build()
            WorkManager.getInstance(getApplication()).enqueue(workRequest)
        }
    }

    private fun scheduleWorkManagerTasks() {
        val workManager = WorkManager.getInstance(getApplication())

        // 1. Schedule 30-minute periodic motivational reminder
        val reminderWorkRequest = PeriodicWorkRequestBuilder<MotivationalReminderWorker>(
            30, TimeUnit.MINUTES
        ).build()

        workManager.enqueueUniquePeriodicWork(
            WORK_30_MIN_REMINDER,
            ExistingPeriodicWorkPolicy.KEEP,
            reminderWorkRequest
        )

        // 2. Schedule 9:00 PM nightly summary
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 21) // 9:00 PM
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(currentDate)) {
                add(Calendar.HOUR_OF_DAY, 24)
            }
        }
        val initialDelay = dueDate.timeInMillis - currentDate.timeInMillis

        val nightlyWorkRequest = PeriodicWorkRequestBuilder<NightlySummaryWorker>(
            24, TimeUnit.HOURS
        ).setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NIGHTLY_SUMMARY,
            ExistingPeriodicWorkPolicy.KEEP,
            nightlyWorkRequest
        )
    }

    companion object {
        const val WORK_30_MIN_REMINDER = "work_30_min_reminder"
        const val WORK_NIGHTLY_SUMMARY = "work_nightly_summary"
    }
}
