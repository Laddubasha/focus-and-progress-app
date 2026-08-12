package com.example.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object WorkManagerScheduler {

    private const val MOTIVATIONAL_WORK_NAME = "PeriodicMotivationalReminder"
    private const val NIGHTLY_SUMMARY_WORK_NAME = "NightlySummaryWorker"

    fun scheduleWork(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // 1. 30-Minute Motivational Reminder
        val motivationalWorkRequest = PeriodicWorkRequestBuilder<MotivationalReminderWorker>(
            30, TimeUnit.MINUTES
        ).build()

        workManager.enqueueUniquePeriodicWork(
            MOTIVATIONAL_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            motivationalWorkRequest
        )

        // 2. Nightly Summary Work at 9:00 PM (21:00)
        val initialDelayMillis = calculateInitialDelayToNightlyTime(21, 0)
        val nightlySummaryRequest = PeriodicWorkRequestBuilder<NightlySummaryWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            NIGHTLY_SUMMARY_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            nightlySummaryRequest
        )
    }

    private fun calculateInitialDelayToNightlyTime(targetHour: Int, targetMinute: Int): Long {
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.DAY_OF_MONTH, 1)
        }

        return dueDate.timeInMillis - currentDate.timeInMillis
    }
}
