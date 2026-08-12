package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.db.HabitActivity
import com.example.data.db.MotivationalConfig
import com.example.data.db.MotivationalQuote
import com.example.data.db.NightlySummaryEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FocusRepository(private val db: AppDatabase) {

    val config: Flow<MotivationalConfig?> = db.configDao().getConfig()
    val quotes: Flow<List<MotivationalQuote>> = db.quoteDao().getAllQuotes()
    val habits: Flow<List<HabitActivity>> = db.habitDao().getAllHabits()
    val latestSummary: Flow<NightlySummaryEntity?> = db.summaryDao().getLatestSummary()
    val allSummaries: Flow<List<NightlySummaryEntity>> = db.summaryDao().getAllSummaries()

    suspend fun saveCustomMessage(message: String) {
        val existing = db.configDao().getConfigDirect()
        val nextIdx = existing?.nextQuoteIndex ?: 0
        db.configDao().saveConfig(MotivationalConfig(id = 1, customMessage = message, nextQuoteIndex = nextIdx))
    }

    suspend fun updateNextQuoteIndex(index: Int) {
        val existing = db.configDao().getConfigDirect()
        val msg = existing?.customMessage ?: "Stay focused on your journey today!"
        db.configDao().saveConfig(MotivationalConfig(id = 1, customMessage = msg, nextQuoteIndex = index))
    }

    suspend fun addQuote(text: String, author: String = "") {
        if (text.isNotBlank()) {
            db.quoteDao().insertQuote(MotivationalQuote(quoteText = text.trim(), author = author.trim()))
        }
    }

    suspend fun deleteQuote(quote: MotivationalQuote) {
        db.quoteDao().deleteQuote(quote)
    }

    suspend fun addHabit(name: String, target: String, currentProgress: String = "0") {
        if (name.isNotBlank()) {
            val habit = createHabitActivity(name = name.trim(), target = target.trim(), progress = currentProgress.trim())
            db.habitDao().insertHabit(habit)
            generateNightlySummary()
        }
    }

    suspend fun updateHabitProgress(habit: HabitActivity, newProgressStr: String) {
        val updatedHabit = habit.copy(
            currentProgress = newProgressStr,
            progressValue = parseNumericValue(newProgressStr),
            isCompleted = checkIfCompleted(newProgressStr, habit.target),
            lastUpdated = System.currentTimeMillis()
        )
        db.habitDao().updateHabit(updatedHabit)
        generateNightlySummary()
    }

    suspend fun updateHabitTarget(habit: HabitActivity, newTargetStr: String) {
        val updatedHabit = habit.copy(
            target = newTargetStr,
            targetValue = parseNumericValue(newTargetStr),
            isCompleted = checkIfCompleted(habit.currentProgress, newTargetStr),
            lastUpdated = System.currentTimeMillis()
        )
        db.habitDao().updateHabit(updatedHabit)
        generateNightlySummary()
    }

    suspend fun toggleHabitCompletion(habit: HabitActivity, isCompleted: Boolean) {
        val targetVal = if (habit.targetValue > 0) habit.targetValue else 1.0
        val newProgress = if (isCompleted) habit.target else "0"
        val updated = habit.copy(
            isCompleted = isCompleted,
            currentProgress = newProgress,
            progressValue = if (isCompleted) targetVal else 0.0,
            lastUpdated = System.currentTimeMillis()
        )
        db.habitDao().updateHabit(updated)
        generateNightlySummary()
    }

    suspend fun deleteHabit(habit: HabitActivity) {
        db.habitDao().deleteHabit(habit)
        generateNightlySummary()
    }

    suspend fun generateNightlySummary(): NightlySummaryEntity {
        val allHabits = db.habitDao().getAllHabitsDirect()
        val total = allHabits.size

        var totalCompletedRatio = 0.0
        val completedList = mutableListOf<HabitActivity>()
        val partialProgressList = mutableListOf<HabitActivity>()

        for (habit in allHabits) {
            val ratio = when {
                habit.isCompleted -> 1.0
                habit.targetValue > 0 -> (habit.progressValue / habit.targetValue).coerceIn(0.0, 1.0)
                habit.progressValue > 0 -> 1.0
                else -> 0.0
            }
            totalCompletedRatio += ratio
            if (habit.isCompleted || ratio >= 1.0) {
                completedList.add(habit)
            } else if (ratio > 0.0) {
                partialProgressList.add(habit)
            }
        }

        val summarySb = StringBuilder()
        if (total == 0) {
            summarySb.append("No active habits tracked today. Add your habits and targets to stay accountable!")
        } else if (completedList.size == total) {
            summarySb.append("Outstanding work today! 🎉 You hit 100% of your targets across all $total activities including ")
            summarySb.append(completedList.joinToString(", ") { it.name })
            summarySb.append(". Keep up the momentum!")
        } else if (completedList.isNotEmpty()) {
            summarySb.append("Great job today! You hit your target for ")
            summarySb.append(completedList.joinToString(", ") { it.name })
            if (partialProgressList.isNotEmpty()) {
                summarySb.append(" and made progress on ")
                summarySb.append(partialProgressList.joinToString(", ") { it.name })
            }
            summarySb.append(". Keep pushing!")
        } else if (partialProgressList.isNotEmpty()) {
            summarySb.append("Good effort today! You made progress on ")
            summarySb.append(partialProgressList.joinToString(", ") { it.name })
            summarySb.append(". Tomorrow is a fresh opportunity to hit those targets!")
        } else {
            summarySb.append("Tomorrow is a new day! Set aside time for ")
            summarySb.append(allHabits.take(3).joinToString(", ") { it.name })
            summarySb.append(" and finish strong.")
        }

        val percentage = if (total > 0) (totalCompletedRatio.toFloat() / total.toFloat()) * 100f else 0f

        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val existingToday = db.summaryDao().getSummaryByDate(dateStr)

        val entity = NightlySummaryEntity(
            id = existingToday?.id ?: 0,
            dateString = dateStr,
            summaryText = summarySb.toString(),
            totalActivities = total,
            completedActivities = totalCompletedRatio.toInt(),
            completionPercentage = percentage,
            timestamp = System.currentTimeMillis()
        )
        db.summaryDao().insertSummary(entity)
        return entity
    }

    suspend fun checkAndResetDailyProgressIfNeeded() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayDateStr = sdf.format(Date())

        val habitsList = db.habitDao().getAllHabitsDirect()
        if (habitsList.isEmpty()) {
            generateNightlySummary()
            return
        }

        val needsReset = habitsList.any { habit ->
            val habitDateStr = sdf.format(Date(habit.lastUpdated))
            habitDateStr != todayDateStr
        }

        if (needsReset) {
            val currentTime = System.currentTimeMillis()
            for (habit in habitsList) {
                val resetHabit = habit.copy(
                    currentProgress = "0",
                    progressValue = 0.0,
                    isCompleted = false,
                    lastUpdated = currentTime
                )
                db.habitDao().updateHabit(resetHabit)
            }
            generateNightlySummary()
        } else {
            generateNightlySummary()
        }
    }

    suspend fun getDirectConfig(): MotivationalConfig? = db.configDao().getConfigDirect()
    suspend fun getDirectQuotes(): List<MotivationalQuote> = db.quoteDao().getAllQuotesDirect()

    private fun createHabitActivity(name: String, target: String, progress: String): HabitActivity {
        val tVal = parseNumericValue(target)
        val pVal = parseNumericValue(progress)
        return HabitActivity(
            name = name,
            target = target,
            currentProgress = progress,
            targetValue = if (tVal <= 0.0) 1.0 else tVal,
            progressValue = pVal,
            isCompleted = (pVal > 0 && pVal >= tVal)
        )
    }

    private fun parseNumericValue(text: String): Double {
        val numericString = text.replace(Regex("[^0-9.]"), "")
        return numericString.toDoubleOrNull() ?: 0.0
    }

    private fun checkIfCompleted(progressStr: String, targetStr: String): Boolean {
        val p = parseNumericValue(progressStr)
        val t = parseNumericValue(targetStr)
        return if (t > 0) p >= t else p > 0
    }
}
