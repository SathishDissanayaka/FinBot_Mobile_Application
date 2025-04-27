package com.example.finbot.fragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.finbot.MainActivity
import com.example.finbot.R
import com.example.finbot.model.Expense
import com.example.finbot.util.NotificationHelper
import com.example.finbot.util.SharedPreferencesManager
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

class AddExpenseFragment : Fragment() {

    private lateinit var expenseNameInput: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var dateInput: TextView
    private lateinit var amountInput: EditText
    private lateinit var submitButton: Button
    private lateinit var cancelButton: Button
    private val calendar = Calendar.getInstance()
    
    private lateinit var sharedPrefsManager: SharedPreferencesManager
    private lateinit var notificationHelper: NotificationHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_expense, container, false)

        // Initialize managers
        sharedPrefsManager = SharedPreferencesManager.getInstance(requireContext())
        notificationHelper = NotificationHelper.getInstance(requireContext())

        // Initialize views
        expenseNameInput = view.findViewById(R.id.expenseNameInput)
        categorySpinner = view.findViewById(R.id.categorySpinner)
        dateInput = view.findViewById(R.id.dateInput)
        amountInput = view.findViewById(R.id.amountInput)
        submitButton = view.findViewById(R.id.submitButton)
        cancelButton = view.findViewById(R.id.cancelButton)

        setupViews()
        return view
    }

    private fun setupViews() {
        // Setup Category spinner
        val categories = arrayOf("Food", "Shopping", "Transport", "Health", "Utility", "Other")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)
        categorySpinner.adapter = adapter

        // Setup date picker
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        dateInput.text = dateFormatter.format(calendar.time)

        dateInput.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                dateInput.text = dateFormatter.format(calendar.time)
            }, year, month, day)

            // Prevent selecting a future date
            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

            datePickerDialog.show()
        }

        // Setup buttons
        submitButton.setOnClickListener {
            saveExpense()
        }

        cancelButton.setOnClickListener {
            // Navigate back to previous fragment
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    private fun saveExpense() {
        val name = expenseNameInput.text.toString()
        val category = categorySpinner.selectedItem.toString()
        val date = dateInput.text.toString()
        val amount = amountInput.text.toString()

        if (name.isNotEmpty() && amount.isNotEmpty()) {
            val iconResId = getCategoryIconResId(category)
            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val categoryId = getCategoryId(category)

            // Create expense with name included
            val expense = Expense(iconResId, name, category, date, time, amount, categoryId)

            // Save expense using SharedPreferencesManager
            sharedPrefsManager.addExpense(expense)
            
            // Show success message
            Snackbar.make(requireView(), "Expense saved successfully", Snackbar.LENGTH_SHORT).show()
            
            // Check if budget alert should be shown
            notificationHelper.checkAndShowBudgetAlertIfNeeded()
            
            // Navigate to home fragment
            (requireActivity() as MainActivity).loadFragment((requireActivity() as MainActivity).supportFragmentManager.findFragmentByTag("homeFragment") 
                ?: com.example.finbot.fragments.homeFragment())
        } else {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCategoryIconResId(category: String): Int {
        return when (category) {
            "Food" -> R.drawable.food
            "Shopping" -> R.drawable.shopping
            "Transport" -> R.drawable.transport
            "Health" -> R.drawable.health
            "Utility" -> R.drawable.utility
            else -> R.drawable.other
        }
    }

    private fun getCategoryId(category: String): Int {
        return when (category) {
            "Food" -> 1
            "Shopping" -> 2
            "Transport" -> 3
            "Health" -> 4
            "Utility" -> 5
            else -> 6
        }
    }
}