package com.example.coinsage.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.coinsage.MainActivity
import com.example.coinsage.R
import java.math.BigDecimal

class NotificationHelper(private val context: Context) {
    companion object {
        private const val CHANNEL_ID = "budget_alerts"
        private const val BASE_NOTIFICATION_ID = 1000
        private const val TAG = "NotificationHelper"
        
        private val BUDGET_THRESHOLDS = mapOf(
            80 to NotificationCompat.PRIORITY_LOW,
            90 to NotificationCompat.PRIORITY_DEFAULT,
            100 to NotificationCompat.PRIORITY_HIGH
        )
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val notifiedThresholds = mutableSetOf<Int>()

    init {
        createNotificationChannel()
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_channel_name)
            val description = context.getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                this.description = description
            }
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }

    fun checkBudgetThreshold(totalSpent: BigDecimal, budget: BigDecimal) {
        if (!PrefsHelper(context).getNotificationsEnabled()) {
            Log.d(TAG, "Notifications are disabled")
            return
        }
        if (budget <= BigDecimal.ZERO) {
            Log.d(TAG, "Budget is zero or negative")
            return
        }

        val spentPercentage = (totalSpent.multiply(BigDecimal(100)))
            .divide(budget, 2, BigDecimal.ROUND_HALF_UP)
            .toInt()

        Log.d(TAG, "Checking budget threshold: Spent $spentPercentage% of budget")
        Log.d(TAG, "Total spent: $totalSpent, Budget: $budget")
        Log.d(TAG, "Already notified thresholds: $notifiedThresholds")

        // Check each threshold
        for ((threshold, priority) in BUDGET_THRESHOLDS) {
            if (spentPercentage >= threshold && !notifiedThresholds.contains(threshold)) {
                Log.d(TAG, "Triggering notification for $threshold% threshold")
                showBudgetAlert(spentPercentage, threshold, priority)
                notifiedThresholds.add(threshold)
            }
        }
    }

    fun resetNotifications() {
        Log.d(TAG, "Resetting notifications")
        notifiedThresholds.clear()
    }

    private fun showBudgetAlert(progressPercentage: Int, threshold: Int, priority: Int) {
        if (!PrefsHelper(context).getNotificationsEnabled()) {
            Log.d(TAG, "Notifications are disabled, not showing alert")
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (threshold) {
            100 -> context.getString(R.string.notification_budget_exceeded)
            else -> context.getString(R.string.budget_alert_title)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.budget_alert_message, progressPercentage))
            .setPriority(priority)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // Use different notification ID for each threshold
        val notificationId = BASE_NOTIFICATION_ID + threshold
        notificationManager.notify(notificationId, notification)
        Log.d(TAG, "Notification shown for $threshold% threshold with ID $notificationId")
    }
} 