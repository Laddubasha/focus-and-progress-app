package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.db.AppDatabase
import com.example.data.repository.FocusRepository

class NightlySummaryWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(context)
            val repository = FocusRepository(db)

            val summary = repository.generateNightlySummary()

            val title = "🌙 Daily Progress Summary (${String.format("%.0f", summary.completionPercentage)}%)"
            val content = summary.summaryText

            MotivationalReminderWorker.showNotification(
                context = context,
                channelId = "nightly_summary_channel",
                channelName = "9:00 PM Nightly Summary",
                notificationId = 1002,
                title = title,
                content = content
            )

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
