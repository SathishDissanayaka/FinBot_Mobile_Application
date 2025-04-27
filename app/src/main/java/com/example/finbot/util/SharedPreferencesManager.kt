package com.example.finbot.util

import android.content.Context
import android.content.SharedPreferences
import com.example.finbot.model.Expense
import com.example.finbot.model.Earning
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.util.*

class SharedPreferencesManager(context: Context) {
    
    companion object {
        private const val PREF_NAME = "FinBotPreferences"
        private const val KEY_EXPENSES = "expenses_list"
        private const val KEY_EARNINGS = "earnings_list"
        private const val KEY_CURRENCY = "currency_type"
        private const val KEY_MONTHLY_BUDGET = "monthly_budget"
        private const val KEY_NOTIFICATION_ENABLED = "notifications_enabled"
        private const val KEY_BUDGET_ALERT_PERCENT = "budget_alert_percent"
        private const val KEY_REMINDER_ENABLED = "reminder_enabled"
        private const val KEY_USER_NAME = "user_name"
        
        // Default values
        private const val DEFAULT_CURRENCY = "LKR"
        private const val DEFAULT_MONTHLY_BUDGET = 30000.0
        private const val DEFAULT_BUDGET_ALERT_PERCENT = 80 // Alert at 80% of budget
        private const val DEFAULT_USER_NAME = "User"
        
        @Volatile
        private var INSTANCE: SharedPreferencesManager? = null
        
        fun getInstance(context: Context): SharedPreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SharedPreferencesManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    // User Preferences
    fun setCurrency(currency: String) {
        sharedPreferences.edit().putString(KEY_CURRENCY, currency).apply()
    }
    
    fun getCurrency(): String {
        return sharedPreferences.getString(KEY_CURRENCY, DEFAULT_CURRENCY) ?: DEFAULT_CURRENCY
    }
    
    fun setMonthlyBudget(budget: Double) {
        sharedPreferences.edit().putFloat(KEY_MONTHLY_BUDGET, budget.toFloat()).apply()
    }
    
    fun getMonthlyBudget(): Double {
        return sharedPreferences.getFloat(KEY_MONTHLY_BUDGET, DEFAULT_MONTHLY_BUDGET.toFloat()).toDouble()
    }
    
    fun setUserName(name: String) {
        sharedPreferences.edit().putString(KEY_USER_NAME, name).apply()
    }
    
    fun getUserName(): String {
        return sharedPreferences.getString(KEY_USER_NAME, DEFAULT_USER_NAME) ?: DEFAULT_USER_NAME
    }
    
    // Notification Settings
    fun setNotificationsEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_NOTIFICATION_ENABLED, enabled).apply()
    }
    
    fun areNotificationsEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_NOTIFICATION_ENABLED, true)
    }
    
    fun setBudgetAlertPercent(percent: Int) {
        sharedPreferences.edit().putInt(KEY_BUDGET_ALERT_PERCENT, percent).apply()
    }
    
    fun getBudgetAlertPercent(): Int {
        return sharedPreferences.getInt(KEY_BUDGET_ALERT_PERCENT, DEFAULT_BUDGET_ALERT_PERCENT)
    }
    
    fun setReminderEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_REMINDER_ENABLED, enabled).apply()
    }
    
    fun isReminderEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_REMINDER_ENABLED, false)
    }
    
    // Expenses Management
    fun saveExpenses(expenses: List<Expense>) {
        val json = gson.toJson(expenses)
        sharedPreferences.edit().putString(KEY_EXPENSES, json).apply()
    }
    
    fun getExpenses(): List<Expense> {
        val json = sharedPreferences.getString(KEY_EXPENSES, null)
        return if (json.isNullOrEmpty()) {
            emptyList()
        } else {
            val type: Type = object : TypeToken<List<Expense>>() {}.type
            gson.fromJson(json, type)
        }
    }
    
    fun addExpense(expense: Expense) {
        val expenses = getExpenses().toMutableList()
        expenses.add(expense)
        saveExpenses(expenses)
    }
    
    fun updateExpense(oldExpense: Expense, newExpense: Expense) {
        val expenses = getExpenses().toMutableList()
        val index = expenses.indexOfFirst { 
            it.name == oldExpense.name && 
            it.category == oldExpense.category && 
            it.date == oldExpense.date && 
            it.time == oldExpense.time && 
            it.amount == oldExpense.amount 
        }
        if (index != -1) {
            expenses[index] = newExpense
            saveExpenses(expenses)
        }
    }
    
    fun deleteExpense(expense: Expense) {
        val expenses = getExpenses().toMutableList()
        expenses.removeIf { 
            it.name == expense.name && 
            it.category == expense.category && 
            it.date == expense.date && 
            it.time == expense.time && 
            it.amount == expense.amount 
        }
        saveExpenses(expenses)
    }
    
    // Earnings Management
    fun saveEarnings(earnings: List<Earning>) {
        val json = gson.toJson(earnings)
        sharedPreferences.edit().putString(KEY_EARNINGS, json).apply()
    }
    
    fun getEarnings(): List<Earning> {
        val json = sharedPreferences.getString(KEY_EARNINGS, null)
        return if (json.isNullOrEmpty()) {
            emptyList()
        } else {
            val type: Type = object : TypeToken<List<Earning>>() {}.type
            gson.fromJson(json, type)
        }
    }
    
    fun addEarning(earning: Earning) {
        val earnings = getEarnings().toMutableList()
        earnings.add(earning)
        saveEarnings(earnings)
    }
    
    fun updateEarning(oldEarning: Earning, newEarning: Earning) {
        val earnings = getEarnings().toMutableList()
        val index = earnings.indexOfFirst { 
            it.id == oldEarning.id 
        }
        if (index != -1) {
            earnings[index] = newEarning
            saveEarnings(earnings)
        }
    }
    
    fun deleteEarning(earning: Earning) {
        val earnings = getEarnings().toMutableList()
        earnings.removeIf { it.id == earning.id }
        saveEarnings(earnings)
    }
    
    fun getCurrentMonthExpenses(): Double {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        return getExpenses()
            .filter {
                try {
                    val parts = it.date.split("-")
                    // Date format is expected to be yyyy-MM-dd or MM-dd-yyyy
                    // Adjust parsing based on your actual format
                    val expMonth = parts[1].toInt() - 1 // Month is 0-based in Calendar
                    val expYear = if (parts[0].length == 4) parts[0].toInt() else parts[2].toInt()
                    expMonth == currentMonth && expYear == currentYear
                } catch (e: Exception) {
                    false
                }
            }
            .sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
    }
    
    // Budget percentage used
    fun getCurrentBudgetUsagePercent(): Int {
        val currentExpenses = getCurrentMonthExpenses()
        val budget = getMonthlyBudget()
        return if (budget > 0) {
            ((currentExpenses / budget) * 100).toInt()
        } else {
            0
        }
    }
    
    // Check if budget alert should be triggered
    fun shouldTriggerBudgetAlert(): Boolean {
        val currentPercentage = getCurrentBudgetUsagePercent()
        val alertThreshold = getBudgetAlertPercent()
        return currentPercentage >= alertThreshold
    }
    
    // Clear all data (for logging out)
    fun clearAllData() {
        sharedPreferences.edit().clear().apply()
    }
}