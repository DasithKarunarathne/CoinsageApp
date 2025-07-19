package com.example.coinsage.utils

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

class ReminderWorker(appContext: Context, params: WorkerParameters) : Worker(appContext, params) {
    override fun doWork(): Result {
        NotificationUtils.showBudgetAlert(applicationContext, "Don't forget to check your budget today!")
        return Result.success()
    }
}

object WorkManagerUtils {
    private const val REMINDER_WORK = "daily_budget_reminder"

    fun scheduleDailyReminder(context: Context) {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(1, TimeUnit.DAYS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            REMINDER_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancelDailyReminder(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(REMINDER_WORK)
    }
}