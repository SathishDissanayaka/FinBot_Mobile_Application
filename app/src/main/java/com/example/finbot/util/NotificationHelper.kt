package com.example.finbot.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.finbot.MainActivity
import com.example.finbot.R
import java.util.*

class NotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID_BUDGET = "finbot_budget_channel"
        private const val CHANNEL_ID_REMINDER = "finbot_reminder_channel"
        private const val NOTIFICATION_ID_BUDGET = 1001
        private const val NOTIFICATION_ID_REMINDER = 1002
        private const val REMINDER_REQUEST_CODE = 2001
        
        @Volatile
        private var INSTANCE: NotificationHelper? = null
        
        fun getInstance(context: Context): NotificationHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NotificationHelper(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
    
    init {
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        // Create notification channels for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val budgetChannel = NotificationChannel(
                CHANNEL_ID_BUDGET,
                "Budget Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when you approach or exceed your budget"
            }
            
            val reminderChannel = NotificationChannel(
                CHANNEL_ID_REMINDER,
                "Daily Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily reminders to record your expenses"
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(budgetChannel)
            notificationManager.createNotificationChannel(reminderChannel)
        }
    }
    
    fun showBudgetAlert(currentSpending: Double, budget: Double, percentUsed: Int) {
        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val currency = SharedPreferencesManager.getInstance(context).getCurrency()
        val message = when {
            currentSpending >= budget -> "You have exceeded your monthly budget of $currency $budget"
            else -> "You've used $percentUsed% of your monthly budget ($currency $currentSpending of $currency $budget)"
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_BUDGET)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle("Budget Alert")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        with(NotificationManagerCompat.from(context)) {
            try {
                notify(NOTIFICATION_ID_BUDGET, notification)
            } catch (e: SecurityException) {
                // Handle missing notification permission
            }
        }
    }
    
    fun showDailyReminder() {
        val intent = Intent(context, MainActivity::class.java)
        intent.putExtra("OPEN_ADD_EXPENSE", true)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDER)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle("FinBot Reminder")
            .setContentText("Don't forget to record today's expenses!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        with(NotificationManagerCompat.from(context)) {
            try {
                notify(NOTIFICATION_ID_REMINDER, notification)
            } catch (e: SecurityException) {
                // Handle missing notification permission
            }
        }
    }
    
    fun scheduleDailyReminder() {
        val sharedPrefsManager = SharedPreferencesManager.getInstance(context)
        if (!sharedPrefsManager.isReminderEnabled()) return
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, REMINDER_REQUEST_CODE, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Set reminder for 8:00 PM every day
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        
        // If time has already passed today, set for tomorrow
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        // Schedule repeating alarm
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
    
    fun cancelDailyReminder() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, REMINDER_REQUEST_CODE, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }
    
    // Check and display budget alert if needed
    fun checkAndShowBudgetAlertIfNeeded() {
        val sharedPrefsManager = SharedPreferencesManager.getInstance(context)
        if (!sharedPrefsManager.areNotificationsEnabled()) return
        
        if (sharedPrefsManager.shouldTriggerBudgetAlert()) {
            val currentSpending = sharedPrefsManager.getCurrentMonthExpenses()
            val budget = sharedPrefsManager.getMonthlyBudget()
            val percentUsed = sharedPrefsManager.getCurrentBudgetUsagePercent()
            
            showBudgetAlert(currentSpending, budget, percentUsed)
        }
    }
    
    // Broadcast receiver for daily reminders
    class ReminderReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val notificationHelper = NotificationHelper.getInstance(context)
            notificationHelper.showDailyReminder()
        }
    }
}