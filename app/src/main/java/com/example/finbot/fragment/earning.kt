package com.example.finbot.fragment

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finbot.R
import com.example.finbot.adapter.EarningsAdapter
import com.example.finbot.model.Earning
import com.example.finbot.util.SharedPreferencesManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class earningFragment : Fragment() {

    private lateinit var earningsRecyclerView: RecyclerView
    private lateinit var totalEarningsText: TextView
    private lateinit var totalSavingsText: TextView
    private lateinit var noEarningsText: TextView
    private lateinit var sharedPrefsManager: SharedPreferencesManager
    private lateinit var earningsAdapter: EarningsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.earning, container, false)

        // Initialize SharedPreferencesManager
        sharedPrefsManager = SharedPreferencesManager.getInstance(requireContext())

        // Initialize views
        earningsRecyclerView = view.findViewById(R.id.earningsRecyclerView)
        totalEarningsText = view.findViewById(R.id.totalEarningsText)
        totalSavingsText = view.findViewById(R.id.totalSavingsText)
        noEarningsText = view.findViewById(R.id.noEarningsText)

        // Set up RecyclerView
        earningsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Add Earning Button
        view.findViewById<Button>(R.id.addEarningButton).setOnClickListener {
            showAddEarningDialog()
        }

        return view
    }
    
    override fun onResume() {
        super.onResume()
        loadEarnings()
    }

    private fun loadEarnings() {
        val earnings = sharedPrefsManager.getEarnings()
        
        // Show/hide empty state
        if (earnings.isEmpty()) {
            earningsRecyclerView.visibility = View.GONE
            noEarningsText.visibility = View.VISIBLE
        } else {
            earningsRecyclerView.visibility = View.VISIBLE
            noEarningsText.visibility = View.GONE
            
            // Initialize adapter with earnings from SharedPreferences
            earningsAdapter = EarningsAdapter(
                requireContext(),
                earnings,
                { earning -> showEditEarningDialog(earning) },
                { earning -> showDeleteEarningDialog(earning) }
            )
            earningsRecyclerView.adapter = earningsAdapter
        }
        
        // Update totals
        updateTotals()
    }

    private fun showAddEarningDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_earning, null)
        val categoryInput = dialogView.findViewById<EditText>(R.id.categoryInput)
        val amountInput = dialogView.findViewById<EditText>(R.id.amountInput)
        val dateInput = dialogView.findViewById<TextView>(R.id.dateInput)
        dateInput.setTextColor(resources.getColor(R.color.black, null))
        amountInput.setTextColor(resources.getColor(R.color.black, null))

        // Set up calendar for date selection
        val calendar = Calendar.getInstance()
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        // Set default date
        dateInput.text = dateFormatter.format(calendar.time)
        
        // Setup date picker when clicking on the date field
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
            .setTitle("Add Earning")
            .setView(dialogView)
            .setPositiveButton("Submit") { _, _ ->
                val category = categoryInput.text.toString().trim()
                val amount = amountInput.text.toString().toDoubleOrNull()
                val date = dateInput.text.toString()

                if (category.isNotEmpty() && amount != null) {
                    addEarning(category, amount, date)
                    Toast.makeText(requireContext(), "Earning added successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Please enter valid details.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditEarningDialog(earning: Earning) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_earning, null)
        val categoryInput = dialogView.findViewById<EditText>(R.id.categoryInput)
        val amountInput = dialogView.findViewById<EditText>(R.id.amountInput)
        val dateInput = dialogView.findViewById<TextView>(R.id.dateInput)
        
        // Pre-fill with existing values
        categoryInput.setText(earning.category)
        amountInput.setText(earning.amount.toString())
        dateInput.text = earning.date
        
        dateInput.setTextColor(resources.getColor(R.color.black, null))
        amountInput.setTextColor(resources.getColor(R.color.black, null))
        
        // Set up calendar for date selection
        val calendar = Calendar.getInstance()
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        try {
            // Parse existing date
            val date = dateFormatter.parse(earning.date)
            if (date != null) {
                calendar.time = date
            }
        } catch (e: Exception) {
            // Use current date if parsing fails
        }
        
        // Setup date picker when clicking on the date field
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
            .setTitle("Edit Earning")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val category = categoryInput.text.toString().trim()
                val amount = amountInput.text.toString().toDoubleOrNull()
                val date = dateInput.text.toString()

                if (category.isNotEmpty() && amount != null) {
                    val updatedEarning = Earning(earning.id, category, amount, date)
                    updateEarning(earning, updatedEarning)
                    Toast.makeText(requireContext(), "Earning updated!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Please enter valid details.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteEarningDialog(earning: Earning) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Earning")
            .setMessage("Are you sure you want to delete this earning?")
            .setPositiveButton("Delete") { _, _ ->
                deleteEarning(earning)
                Toast.makeText(requireContext(), "Earning deleted!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addEarning(category: String, amount: Double, date: String) {
        // Generate new ID based on existing earnings
        val earnings = sharedPrefsManager.getEarnings()
        val newId = if (earnings.isEmpty()) 1 else earnings.maxOf { it.id } + 1
        
        // Create and save the new earning
        val newEarning = Earning(newId, category, amount, date)
        sharedPrefsManager.addEarning(newEarning)
        
        // Reload earnings from SharedPreferences
        loadEarnings()
    }
    
    private fun updateEarning(oldEarning: Earning, newEarning: Earning) {
        sharedPrefsManager.updateEarning(oldEarning, newEarning)
        loadEarnings()
    }
    
    private fun deleteEarning(earning: Earning) {
        sharedPrefsManager.deleteEarning(earning)
        loadEarnings()
    }

    private fun updateTotals() {
        val earnings = sharedPrefsManager.getEarnings()
        val totalEarnings = earnings.sumOf { it.amount }
        val currency = sharedPrefsManager.getCurrency()
        
        totalEarningsText.text = "$currency ${String.format("%.2f", totalEarnings)}"

        // Calculate real savings: total earnings - total expenses
        val totalExpenses = sharedPrefsManager.getCurrentMonthExpenses().toDouble()
        val totalSavings = totalEarnings - totalExpenses
        totalSavingsText.text = "$currency ${String.format("%.2f", totalSavings)}"
    }
}
