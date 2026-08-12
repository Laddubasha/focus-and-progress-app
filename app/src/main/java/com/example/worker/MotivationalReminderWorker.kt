package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.data.db.AppDatabase
import com.example.data.repository.FocusRepository
import java.util.Calendar

class MotivationalReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val calendar = Calendar.getInstance()
            val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY) // 0-23

            // Reminders run only from 6:00 AM (inclusive) up to 9:00 PM (exclusive, hour 21)
            if (hourOfDay < 6 || hourOfDay >= 21) {
                return Result.success()
            }

            val db = AppDatabase.getDatabase(context)
            val repository = FocusRepository(db)

            val config = repository.getDirectConfig()
            val quotes = repository.getDirectQuotes() // Quotes sorted by id ASC

            val customMessage = config?.customMessage?.ifBlank {
                "Keep pushing towards your daily targets!"
            } ?: "Keep pushing towards your daily targets!"

            val selectedQuote: String
            if (quotes.isNotEmpty()) {
                val currentIndex = (config?.nextQuoteIndex ?: 0) % quotes.size
                val q = quotes[currentIndex]
                selectedQuote = if (q.author.isNotBlank()) "\"${q.quoteText}\" — ${q.author}" else "\"${q.quoteText}\""

                // Advance to next quote in circular chain
                val nextIndex = (currentIndex + 1) % quotes.size
                repository.updateNextQuoteIndex(nextIndex)
            } else {
                selectedQuote = "\"Success is the sum of small efforts repeated day in and day out.\""
            }

            val fullNotificationContent = "$customMessage\n\n$selectedQuote"

            showNotification(
                context = context,
                channelId = CHANNEL_ID,
                channelName = "30-Min Motivational Reminders",
                notificationId = NOTIFICATION_ID,
                title = "⚡ Focus & Progress Reminder",
                content = fullNotificationContent
            )

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        const val CHANNEL_ID = "motivational_reminders_channel"
        const val NOTIFICATION_ID = 1001

        fun showNotification(
            context: Context,
            channelId: String,
            channelName: String,
            notificationId: Int,
            title: String,
            content: String
        ) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Periodic motivational reminders and progress updates"
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(content.take(100))
                .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(notificationId, notification)
        }
    }
}
