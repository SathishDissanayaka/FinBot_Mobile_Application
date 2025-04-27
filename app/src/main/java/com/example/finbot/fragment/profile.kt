package com.example.finbot.fragment

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.example.finbot.R
import com.example.finbot.util.BackupRestoreUtil
import com.example.finbot.util.NotificationHelper
import com.example.finbot.util.SharedPreferencesManager
import com.google.android.material.snackbar.Snackbar

class profileFragment : Fragment() {

    private lateinit var sharedPrefsManager: SharedPreferencesManager
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var backupRestoreUtil: BackupRestoreUtil

    // UI components
    private lateinit var userNameInput: EditText
    private lateinit var monthlyBudgetInput: EditText
    private lateinit var currencySpinner: Spinner
    private lateinit var budgetAlertsSwitch: SwitchCompat
    private lateinit var dailyReminderSwitch: SwitchCompat
    private lateinit var alertThresholdSeekBar: SeekBar
    private lateinit var thresholdTextView: TextView
    private lateinit var alertThresholdLayout: LinearLayout

    private val currencies = arrayOf("LKR", "USD", "EUR", "GBP", "INR", "AUD")

    private val restoreLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            if (backupRestoreUtil.restoreFromJson(selectedUri)) {
                showSnackbar("Data restored successfully")
                loadSettings() // Reload settings from restored data
            } else {
                showSnackbar("Failed to restore data")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.profile, container, false)

        // Initialize managers
        sharedPrefsManager = SharedPreferencesManager.getInstance(requireContext())
        notificationHelper = NotificationHelper.getInstance(requireContext())
        backupRestoreUtil = BackupRestoreUtil.getInstance(requireContext())

        // Initialize views
        initializeViews(view)
        loadSettings()
        setupListeners()

        return view
    }

    private fun initializeViews(view: View) {
        // User profile
        userNameInput = view.findViewById(R.id.userNameInput)
        view.findViewById<Button>(R.id.saveProfileButton).setOnClickListener { saveUserProfile() }

        // Budget settings
        monthlyBudgetInput = view.findViewById(R.id.monthlyBudgetInput)
        currencySpinner = view.findViewById(R.id.currencySpinner)
        val currencyAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, currencies)
        currencySpinner.adapter = currencyAdapter
        view.findViewById<Button>(R.id.saveBudgetButton).setOnClickListener { saveBudgetSettings() }

        // Notification settings
        budgetAlertsSwitch = view.findViewById(R.id.budgetAlertsSwitch)
        dailyReminderSwitch = view.findViewById(R.id.dailyReminderSwitch)
        alertThresholdSeekBar = view.findViewById(R.id.alertThresholdSeekBar)
        thresholdTextView = view.findViewById(R.id.thresholdTextView)
        alertThresholdLayout = view.findViewById(R.id.alertThresholdLayout)
        view.findViewById<Button>(R.id.saveNotificationButton).setOnClickListener { saveNotificationSettings() }

        // Backup & Restore
        view.findViewById<Button>(R.id.exportJsonButton).setOnClickListener { exportToJson() }
        view.findViewById<Button>(R.id.restoreDataButton).setOnClickListener { restoreFromBackup() }
    }

    private fun loadSettings() {
        // Load user profile
        userNameInput.setText(sharedPrefsManager.getUserName())

        // Load budget settings
        monthlyBudgetInput.setText(sharedPrefsManager.getMonthlyBudget().toString())
        val currencyIndex = currencies.indexOf(sharedPrefsManager.getCurrency())
        if (currencyIndex >= 0) {
            currencySpinner.setSelection(currencyIndex)
        }

        // Load notification settings
        budgetAlertsSwitch.isChecked = sharedPrefsManager.areNotificationsEnabled()
        dailyReminderSwitch.isChecked = sharedPrefsManager.isReminderEnabled()

        val threshold = sharedPrefsManager.getBudgetAlertPercent()
        alertThresholdSeekBar.progress = threshold
        updateThresholdText(threshold)

        // Update UI state based on settings
        alertThresholdLayout.visibility = if (budgetAlertsSwitch.isChecked) View.VISIBLE else View.GONE
    }

    private fun setupListeners() {
        // Notification switches
        budgetAlertsSwitch.setOnCheckedChangeListener { _, isChecked ->
            alertThresholdLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Threshold seekbar
        alertThresholdSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                updateThresholdText(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    private fun updateThresholdText(progress: Int) {
        thresholdTextView.text = "$progress% of monthly budget"
    }

    private fun saveUserProfile() {
        val userName = userNameInput.text.toString().trim()
        if (userName.isEmpty()) {
            showSnackbar("Please enter your name")
            return
        }

        sharedPrefsManager.setUserName(userName)
        showSnackbar("User profile saved")
    }

    private fun saveBudgetSettings() {
        val budgetStr = monthlyBudgetInput.text.toString().trim()
        if (budgetStr.isEmpty()) {
            showSnackbar("Please enter monthly budget")
            return
        }

        try {
            val budget = budgetStr.toDouble()
            sharedPrefsManager.setMonthlyBudget(budget)

            val currency = currencies[currencySpinner.selectedItemPosition]
            sharedPrefsManager.setCurrency(currency)

            showSnackbar("Budget settings saved")
        } catch (e: NumberFormatException) {
            showSnackbar("Please enter a valid number for budget")
        }
    }

    private fun saveNotificationSettings() {
        sharedPrefsManager.setNotificationsEnabled(budgetAlertsSwitch.isChecked)
        sharedPrefsManager.setReminderEnabled(dailyReminderSwitch.isChecked)
        sharedPrefsManager.setBudgetAlertPercent(alertThresholdSeekBar.progress)

        if (dailyReminderSwitch.isChecked) {
            notificationHelper.scheduleDailyReminder()
        } else {
            notificationHelper.cancelDailyReminder()
        }

        showSnackbar("Notification settings saved")
    }

    private fun exportToJson() {
        val file = backupRestoreUtil.exportToJson()
        if (file != null) {
            shareFile(file, "application/json")
        } else {
            showSnackbar("Failed to create JSON backup")
        }
    }


    private fun shareFile(file: java.io.File, mimeType: String) {
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            file
        )

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = mimeType
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, "Share backup file"))
    }

    private fun restoreFromBackup() {
        restoreLauncher.launch("application/json")
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
    }
}