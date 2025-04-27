package com.example.finbot.fragments

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finbot.R
import com.example.finbot.adapter.ExpenseAdapter
import com.example.finbot.model.Expense
import com.example.finbot.util.NotificationHelper
import com.example.finbot.util.SharedPreferencesManager
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

class homeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateTextView: TextView
    private lateinit var totalExpenseTextView: TextView
    private lateinit var budgetTextView: TextView
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var progressPercentage: TextView
    private lateinit var spentPercentText: TextView
    private lateinit var limitText: TextView
    private lateinit var sharedPrefsManager: SharedPreferencesManager
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var adapter: ExpenseAdapter
    
    // Categories used for expense spinner
    private val categories = arrayOf("Food", "Shopping", "Transport", "Health", "Utility", "Other")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.home, container, false)

        // Initialize managers
        sharedPrefsManager = SharedPreferencesManager.getInstance(requireContext())
        notificationHelper = NotificationHelper.getInstance(requireContext())

        // Initialize views
        recyclerView = view.findViewById(R.id.expensesRecyclerView)
        emptyStateTextView = view.findViewById(R.id.emptyStateText)
        totalExpenseTextView = view.findViewById(R.id.totalExpenseText)
        budgetTextView = view.findViewById(R.id.budgetText)
        progressBar = view.findViewById(R.id.progressBar)
        progressPercentage = view.findViewById(R.id.progressPercentage)
        spentPercentText = view.findViewById(R.id.spentPercentText)
        limitText = view.findViewById(R.id.limitText)
        
        // Set up RecyclerView with fixed height issue
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.isNestedScrollingEnabled = true

        return view
    }

    override fun onResume() {
        super.onResume()
        loadExpenses()
        updateBudgetInfo()
    }

    private fun loadExpenses() {
        // Get expenses from SharedPreferencesManager
        val expenses = sharedPrefsManager.getExpenses()
        
        // Show empty state if no expenses
        if (expenses.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyStateTextView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyStateTextView.visibility = View.GONE
            
            // Set up adapter with click listener for edit/delete
            adapter = ExpenseAdapter(requireContext(), expenses) { expense ->
                showExpenseOptionsDialog(expense)
            }
            recyclerView.adapter = adapter
        }
    }
    
    private fun updateBudgetInfo() {
        val currency = sharedPrefsManager.getCurrency()
        val totalExpenses = sharedPrefsManager.getCurrentMonthExpenses()
        val budget = sharedPrefsManager.getMonthlyBudget()
        val percentUsed = sharedPrefsManager.getCurrentBudgetUsagePercent()
        
        // Update text views
        totalExpenseTextView.text = "$currency $totalExpenses"
        budgetTextView.text = "Budget: $currency $budget ($percentUsed% used)"
        
        // Update progress bar and related text
        progressBar.progress = percentUsed
        progressPercentage.text = "$percentUsed%"
        spentPercentText.text = "Spent: $percentUsed%"
        limitText.text = "of $currency $budget limit"
        
        // Change colors based on budget status
        if (percentUsed >= 100) {
            // Use red color (transport) when budget is reached or exceeded
            budgetTextView.setTextColor(requireContext().getColor(R.color.transport)) // Red
            progressBar.setIndicatorColor(requireContext().getColor(R.color.transport))
            limitText.setTextColor(requireContext().getColor(R.color.transport))
        } else if (percentUsed > 90) {
            // Use purple color (shopping) for high percentage but not over limit
            budgetTextView.setTextColor(requireContext().getColor(R.color.shopping)) // Purple
            progressBar.setIndicatorColor(requireContext().getColor(R.color.shopping))
        } else if (percentUsed > 75) {
            budgetTextView.setTextColor(requireContext().getColor(R.color.transport)) // Orange
            progressBar.setIndicatorColor(requireContext().getColor(R.color.transport))
        } else {
            budgetTextView.setTextColor(requireContext().getColor(R.color.food)) // Green
            progressBar.setIndicatorColor(requireContext().getColor(R.color.Blue))
        }
    }
    
    private fun showExpenseOptionsDialog(expense: Expense) {
        val options = arrayOf("Edit", "Delete")
        
        AlertDialog.Builder(requireContext())
            .setTitle("Expense Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> editExpense(expense)
                    1 -> deleteExpense(expense)
                }
            }
            .show()
    }
    
    private fun editExpense(expense: Expense) {
        // Create a dialog view for editing expense
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.fragment_add_expense, null)
        
        // Initialize views in the dialog
        val nameInput = dialogView.findViewById<EditText>(R.id.expenseNameInput)
        val categorySpinner = dialogView.findViewById<Spinner>(R.id.categorySpinner)
        val dateInput = dialogView.findViewById<TextView>(R.id.dateInput)
        val amountInput = dialogView.findViewById<EditText>(R.id.amountInput)
        
        // Setup spinner
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)
        categorySpinner.adapter = adapter
        
        // Pre-fill with existing expense data
        nameInput.setText(expense.name)
        val categoryIndex = categories.indexOf(expense.category)
        if (categoryIndex >= 0) {
            categorySpinner.setSelection(categoryIndex)
        }
        dateInput.text = expense.date
        amountInput.setText(expense.amount)
        
        // Set text colors to ensure visibility
        dateInput.setTextColor(resources.getColor(R.color.black, null))
        amountInput.setTextColor(resources.getColor(R.color.black, null))
        
        // Setup date picker
        val calendar = Calendar.getInstance()
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        try {
            // Parse existing date
            val date = dateFormatter.parse(expense.date)
            if (date != null) {
                calendar.time = date
            }
        } catch (e: Exception) {
            // Use current date if parsing fails
        }
        
        dateInput.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                dateInput.text = dateFormatter.format(calendar.time)
            }, year, month, day).show()
        }
        
        // Show dialog
        AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
            .setTitle("Edit Expense")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val name = nameInput.text.toString().trim()
                val category = categorySpinner.selectedItem.toString()
                val date = dateInput.text.toString()
                val amount = amountInput.text.toString().trim()
                
                if (name.isNotEmpty() && amount.isNotEmpty()) {
                    // Create updated expense with same time but updated other fields
                    val iconResId = getCategoryIconResId(category)
                    val categoryId = getCategoryId(category)
                    val updatedExpense = Expense(iconResId, name, category, date, expense.time, amount, categoryId)
                    
                    // Update expense in SharedPreferencesManager
                    sharedPrefsManager.updateExpense(expense, updatedExpense)
                    
                    // Show success message and refresh the UI
                    Snackbar.make(requireView(), "Expense updated successfully", Snackbar.LENGTH_SHORT).show()
                    loadExpenses()
                    updateBudgetInfo()
                    
                    // Check if budget alert status changed
                    notificationHelper.checkAndShowBudgetAlertIfNeeded()
                } else {
                    Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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
    
    private fun deleteExpense(expense: Expense) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Expense")
            .setMessage("Are you sure you want to delete this expense?")
            .setPositiveButton("Delete") { _, _ ->
                sharedPrefsManager.deleteExpense(expense)
                Snackbar.make(requireView(), "Expense deleted", Snackbar.LENGTH_SHORT).show()
                loadExpenses()
                updateBudgetInfo()
                
                // Check budget status after deletion
                notificationHelper.checkAndShowBudgetAlertIfNeeded()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}