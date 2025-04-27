package com.example.finbot.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.example.finbot.model.Earning
import com.example.finbot.model.Expense
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.json.JSONObject
import org.json.JSONArray
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class BackupRestoreUtil(private val context: Context) {

    companion object {
        private const val BACKUP_FOLDER_NAME = "FinBot/Backups"
        private const val DEFAULT_BACKUP_FILENAME = "finbot_backup"
        private const val FILE_EXTENSION_JSON = ".json"
        private const val FILE_EXTENSION_TXT = ".txt"

        @Volatile
        private var INSTANCE: BackupRestoreUtil? = null

        fun getInstance(context: Context): BackupRestoreUtil {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BackupRestoreUtil(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    // Create a JSON backup object containing all data
    private fun createBackupJsonObject(): JSONObject {
        val sharedPrefsManager = SharedPreferencesManager.getInstance(context)
        val expenses = sharedPrefsManager.getExpenses()
        val earnings = sharedPrefsManager.getEarnings()

        val backupJson = JSONObject().apply {
            put("timestamp", System.currentTimeMillis())
            put("version", "1.0")
            put("currency", sharedPrefsManager.getCurrency())
            put("budget", sharedPrefsManager.getMonthlyBudget())
            put("username", sharedPrefsManager.getUserName())

            val expensesJson = Gson().toJson(expenses)
            put("expenses", JSONArray(expensesJson))

            val earningsJson = Gson().toJson(earnings)
            put("earnings", JSONArray(earningsJson))

            // Add preferences
            val preferencesJson = JSONObject().apply {
                put("notificationsEnabled", sharedPrefsManager.areNotificationsEnabled())
                put("reminderEnabled", sharedPrefsManager.isReminderEnabled())
                put("budgetAlertPercent", sharedPrefsManager.getBudgetAlertPercent())
            }
            put("preferences", preferencesJson)
        }

        return backupJson
    }

    // Export as JSON to internal storage
    fun exportToJson(): File? {
        val backupDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!backupDir.exists()) {
            if (!backupDir.mkdirs()) {
                return null
            }
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val backupFile = File(backupDir, "${DEFAULT_BACKUP_FILENAME}_$timestamp$FILE_EXTENSION_JSON")

        return try {
            val jsonObject = createBackupJsonObject()
            FileWriter(backupFile).use { writer ->
                writer.write(jsonObject.toString(4))
            }
            backupFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Import data from JSON backup file
    fun restoreFromJson(uri: Uri): Boolean {
        return try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            } ?: return false

            val jsonObject = JSONObject(jsonString)
            val sharedPrefsManager = SharedPreferencesManager.getInstance(context)

            // Restore user preferences
            jsonObject.optString("currency")?.let { sharedPrefsManager.setCurrency(it) }
            jsonObject.optDouble("budget", 0.0).let { sharedPrefsManager.setMonthlyBudget(it) }
            jsonObject.optString("username")?.let { sharedPrefsManager.setUserName(it) }

            // Restore expenses
            val expensesJson = jsonObject.optJSONArray("expenses")
            if (expensesJson != null) {
                val gson = Gson()
                val expenseList = gson.fromJson<List<Expense>>(
                    expensesJson.toString(),
                    gson.getType<List<Expense>>()
                )
                sharedPrefsManager.saveExpenses(expenseList)
            }

            // Restore earnings
            val earningsJson = jsonObject.optJSONArray("earnings")
            if (earningsJson != null) {
                val gson = Gson()
                val earningList = gson.fromJson<List<Earning>>(
                    earningsJson.toString(),
                    gson.getType<List<Earning>>()
                )
                sharedPrefsManager.saveEarnings(earningList)
            }

            // Restore preferences
            val preferencesJson = jsonObject.optJSONObject("preferences")
            if (preferencesJson != null) {
                sharedPrefsManager.setNotificationsEnabled(
                    preferencesJson.optBoolean("notificationsEnabled", true)
                )
                sharedPrefsManager.setReminderEnabled(
                    preferencesJson.optBoolean("reminderEnabled", false)
                )
                preferencesJson.optInt("budgetAlertPercent", 80).let {
                    sharedPrefsManager.setBudgetAlertPercent(it)
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Get backup directory for listing available backups
    fun getBackupDirectory(): File? {
        val backupDir = File(context.getExternalFilesDir(null), BACKUP_FOLDER_NAME)
        return if (backupDir.exists()) backupDir else null
    }

    // List all available backups
    fun listAvailableBackups(): List<File> {
        val backupDir = getBackupDirectory() ?: return emptyList()
        return backupDir.listFiles()?.filter {
            it.isFile && (it.name.endsWith(FILE_EXTENSION_JSON) || it.name.endsWith(FILE_EXTENSION_TXT))
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    // Extension function for Gson to handle type erasure
    private inline fun <reified T> Gson.getType() = object : com.google.gson.reflect.TypeToken<T>() {}.type
    private inline fun <reified T> Gson.fromJson(json: String, type: java.lang.reflect.Type): T = fromJson(json, type)
}