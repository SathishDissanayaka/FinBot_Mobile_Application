package com.example.finbot.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat.getColor
import androidx.fragment.app.Fragment
import com.example.finbot.R
import com.example.finbot.model.Earning
import com.example.finbot.model.Expense
import com.example.finbot.util.SharedPreferencesManager
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.chip.ChipGroup
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class statFragment : Fragment() {

    private lateinit var sharedPrefsManager: SharedPreferencesManager
    private lateinit var pieChart: PieChart
    private lateinit var lineChart: LineChart
    private lateinit var earningsLineChart: LineChart
    private lateinit var totalSpentText: TextView
    private lateinit var highestCategoryText: TextView
    private lateinit var periodChipGroup: ChipGroup
    private lateinit var legendContainer: FlexboxLayout
    
    private var currentPeriod = PERIOD_WEEK
    
    companion object {
        const val PERIOD_WEEK = 7
        const val PERIOD_MONTH = 30
        const val PERIOD_YEAR = 365
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout
        val view = inflater.inflate(R.layout.stat, container, false)

        // Initialize SharedPreferencesManager
        sharedPrefsManager = SharedPreferencesManager.getInstance(requireContext())

        // Initialize views
        pieChart = view.findViewById(R.id.pieChart)
        lineChart = view.findViewById(R.id.lineChart)
        earningsLineChart = view.findViewById(R.id.earningsLineChart)
        totalSpentText = view.findViewById(R.id.totalSpentText)
        highestCategoryText = view.findViewById(R.id.highestCategoryText)
        periodChipGroup = view.findViewById(R.id.periodChipGroup)
        legendContainer = view.findViewById(R.id.legendContainer)

        // Setup period selection listeners
        setupPeriodSelectionListeners()
        
        // Load data for the default period (week)
        loadData(PERIOD_WEEK)

        return view
    }

    override fun onResume() {
        super.onResume()
        // Reload data when coming back to this fragment
        loadData(currentPeriod)
    }

    private fun setupPeriodSelectionListeners() {
        periodChipGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.chipWeek -> {
                    currentPeriod = PERIOD_WEEK
                    loadData(PERIOD_WEEK)
                }
                R.id.chipMonth -> {
                    currentPeriod = PERIOD_MONTH
                    loadData(PERIOD_MONTH)
                }
                R.id.chipYear -> {
                    currentPeriod = PERIOD_YEAR
                    loadData(PERIOD_YEAR)
                }
            }
        }
    }

    private fun loadData(days: Int) {
        try {
            // Get filtered expenses
            val filteredExpenses = getFilteredExpenses(days)
            
            // Get filtered earnings
            val filteredEarnings = getFilteredEarnings(days)
            
            // Update summary cards
            updateSummaryCards(filteredExpenses)
            
            // Update charts
            setupPieChart(pieChart, filteredExpenses)
            setupLineChart(lineChart, filteredExpenses, days)
            setupEarningsLineChart(earningsLineChart, filteredEarnings, days)
        } catch (e: Exception) {
            // Log the error
            e.printStackTrace()
            
            // Show empty state with error message
            totalSpentText.text = "${sharedPrefsManager.getCurrency()} 0.00"
            highestCategoryText.text = "None"
            
            // Set up empty charts
            setupEmptyPieChart()
            setupEmptyLineChart()
            setupEmptyEarningsLineChart()
        }
    }
    
    private fun setupEmptyPieChart() {
        try {
            val entries = ArrayList<PieEntry>()
            entries.add(PieEntry(100f, "No Data"))
            val dataSet = PieDataSet(entries, "Expense Distribution")
            dataSet.colors = listOf(Color.LTGRAY)
            dataSet.valueTextColor = Color.WHITE
            dataSet.valueTextSize = 12f
            
            val pieData = PieData(dataSet)
            pieChart.data = pieData
            pieChart.setUsePercentValues(true)
            pieChart.description.isEnabled = false
            pieChart.legend.isEnabled = false
            pieChart.centerText = "No expense data"
            pieChart.animateY(1000)
            pieChart.invalidate()
            
            // Clear legend
            legendContainer.removeAllViews()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun setupEmptyLineChart() {
        try {
            lineChart.setNoDataText("No expense data available")
            lineChart.invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun setupEmptyEarningsLineChart() {
        try {
            earningsLineChart.setNoDataText("No earnings data available")
            earningsLineChart.invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getFilteredExpenses(days: Int): List<Expense> {
        val allExpenses = sharedPrefsManager.getExpenses()
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        // Calculate the cutoff date
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        val cutoffDate = calendar.time
        
        return allExpenses.filter { expense ->
            try {
                val expenseDate = dateFormat.parse(expense.date) ?: Date()
                expenseDate.after(cutoffDate) || expenseDate == cutoffDate
            } catch (e: Exception) {
                false
            }
        }
    }
    
    private fun getFilteredEarnings(days: Int): List<Earning> {
        val allEarnings = sharedPrefsManager.getEarnings()
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        // Calculate the cutoff date
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        val cutoffDate = calendar.time
        
        return allEarnings.filter { earning ->
            try {
                val earningDate = dateFormat.parse(earning.date) ?: Date()
                earningDate.after(cutoffDate) || earningDate == cutoffDate
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun updateSummaryCards(expenses: List<Expense>) {
        // Calculate total spent
        val totalSpent = expenses.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
        val currency = sharedPrefsManager.getCurrency()
        
        // Update total spent text
        totalSpentText.text = "$currency ${String.format("%.2f", totalSpent)}"
        
        // Find highest spending category
        val categorySums = HashMap<String, Double>()
        expenses.forEach { expense ->
            val amount = expense.amount.toDoubleOrNull() ?: 0.0
            categorySums[expense.category] = (categorySums[expense.category] ?: 0.0) + amount
        }
        
        val highestCategory = categorySums.maxByOrNull { it.value }?.key ?: "None"
        highestCategoryText.text = highestCategory
    }

    private fun setupPieChart(pieChart: PieChart, expenses: List<Expense>) {
        try {
            // Group expenses by category
            val categoryMap = HashMap<String, Double>()
            expenses.forEach { expense ->
                val amount = expense.amount.toDoubleOrNull() ?: 0.0
                categoryMap[expense.category] = (categoryMap[expense.category] ?: 0.0) + amount
            }
            
            // Create pie chart entries
            val entries = ArrayList<PieEntry>()
            val totalAmount = categoryMap.values.sum()
            
            // If no data, show empty state
            if (totalAmount <= 0) {
                setupEmptyPieChart()
                return
            }
            
            // Define colors for categories
            val colorMap = mapOf(
                "Food" to getColor(requireContext(), R.color.food),
                "Shopping" to getColor(requireContext(), R.color.shopping),
                "Transport" to getColor(requireContext(), R.color.transport),
                "Health" to getColor(requireContext(), R.color.health),
                "Utility" to getColor(requireContext(), R.color.Blue),
                "Other" to Color.DKGRAY
            )
            
            // Add entries for each category with percentage
            val colors = ArrayList<Int>()
            categoryMap.forEach { (category, amount) ->
                val percentage = (amount / totalAmount * 100).toFloat()
                entries.add(PieEntry(percentage, category))
                
                // Use predefined color or default to dark gray
                colors.add(colorMap[category] ?: Color.DKGRAY)
            }
            
            // Create dataset
            val dataSet = PieDataSet(entries, "Expense Distribution")
            dataSet.colors = colors
            dataSet.valueTextColor = Color.WHITE
            dataSet.valueTextSize = 12f
            
            // Create PieData object
            val pieData = PieData(dataSet)
            
            // Customize the Pie Chart
            pieChart.data = pieData
            pieChart.setUsePercentValues(true)
            pieChart.description.isEnabled = false
            pieChart.legend.isEnabled = false
            pieChart.animateY(1000)
            pieChart.invalidate()
            
            // Update legend using FlexboxLayout
            legendContainer.removeAllViews()
            
            // Add legend items directly to FlexboxLayout
            entries.forEachIndexed { index, entry ->
                // Create legend item
                val legendItem = createLegendItem(colors[index], entry.label, "${entry.value.toInt()}%")
                val itemParams = FlexboxLayout.LayoutParams(
                    FlexboxLayout.LayoutParams.WRAP_CONTENT,
                    FlexboxLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = 16

                }
                legendItem.layoutParams = itemParams
                
                // Add directly to FlexboxLayout
                legendContainer.addView(legendItem)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            setupEmptyPieChart()
        }
    }

    private fun createLegendItem(color: Int, label: String, percentage: String): View {
        // Inflate a layout for the legend item
        val inflater = LayoutInflater.from(requireContext())
        val legendItem = inflater.inflate(R.layout.legend_item, null) as LinearLayout

        // Set the color indicator
        val colorView = legendItem.findViewById<View>(R.id.legendColor)
        colorView.setBackgroundColor(color)

        // Set the label
        val labelView = legendItem.findViewById<TextView>(R.id.legendLabel)
        labelView.text = label

        // Set the percentage
        val percentageView = legendItem.findViewById<TextView>(R.id.legendPercentage)
        percentageView.text = percentage

        return legendItem
    }

    private fun setupLineChart(lineChart: LineChart, expenses: List<Expense>, days: Int) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val entries = ArrayList<Entry>()
        
        // Create a map to store daily expenses
        val dailyExpenses = HashMap<Int, Float>()
        
        // Initialize with zeros for all days in the selected period
        for (i in 0 until days) {
            dailyExpenses[i] = 0f
        }
        
        // Process expenses
        expenses.forEach { expense ->
            try {
                val expenseDate = dateFormat.parse(expense.date) ?: return@forEach
                val expenseCalendar = Calendar.getInstance().apply { time = expenseDate }
                
                // Calculate days difference
                val diff = (calendar.timeInMillis - expenseCalendar.timeInMillis) / (1000 * 60 * 60 * 24)
                
                if (diff < days) {
                    val dayIndex = days - diff.toInt() - 1
                    val amount = expense.amount.toFloatOrNull() ?: 0f
                    // Direct update of the map value without intermediate val
                    dailyExpenses[dayIndex] = (dailyExpenses[dayIndex] ?: 0f) + amount
                }
            } catch (e: Exception) {
                // Skip if date parsing fails
            }
        }
        
        // Create entries from the daily expenses map
        for (i in 0 until days) {
            entries.add(Entry(i.toFloat(), dailyExpenses[i] ?: 0f))
        }
        
        // If no data, show empty state
        if (entries.isEmpty() || entries.all { it.y <= 0f }) {
            lineChart.setNoDataText("No expense data available")
            lineChart.invalidate()
            return
        }
        
        // Create dataset
        val dataSet = LineDataSet(entries, "Spending Trends")
        dataSet.color = getColor(requireContext(), R.color.Blue)
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 10f
        dataSet.setCircleColor(getColor(requireContext(), R.color.Blue))
        dataSet.circleRadius = 4f
        
        // Create LineData object
        val lineData = LineData(dataSet)
        
        // Customize the Line Chart
        lineChart.data = lineData
        lineChart.description.isEnabled = false
        lineChart.axisLeft.axisMinimum = 0f
        lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        lineChart.xAxis.granularity = 1f
        
        // Adjust x-axis labels based on the period
        when (days) {
            PERIOD_WEEK -> {
                lineChart.xAxis.valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(
                    arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                )
                lineChart.xAxis.labelCount = 7
            }
            PERIOD_MONTH -> {
                lineChart.xAxis.labelCount = 10
            }
            PERIOD_YEAR -> {
                lineChart.xAxis.labelCount = 12
            }
        }
        
        lineChart.legend.isEnabled = true
        lineChart.animateX(1000)
        lineChart.invalidate()
    }
    
    private fun setupEarningsLineChart(lineChart: LineChart, earnings: List<Earning>, days: Int) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val entries = ArrayList<Entry>()
        
        // Create a map to store daily earnings
        val dailyEarnings = HashMap<Int, Float>()
        
        // Initialize with zeros for all days in the selected period
        for (i in 0 until days) {
            dailyEarnings[i] = 0f
        }
        
        // Process earnings
        earnings.forEach { earning ->
            try {
                val earningDate = dateFormat.parse(earning.date) ?: return@forEach
                val earningCalendar = Calendar.getInstance().apply { time = earningDate }
                
                // Calculate days difference
                val diff = (calendar.timeInMillis - earningCalendar.timeInMillis) / (1000 * 60 * 60 * 24)
                
                if (diff < days) {
                    val dayIndex = days - diff.toInt() - 1
                    val amount = earning.amount.toFloat()
                    // Direct update of the map value without intermediate val
                    dailyEarnings[dayIndex] = (dailyEarnings[dayIndex] ?: 0f) + amount
                }
            } catch (e: Exception) {
                // Skip if date parsing fails
            }
        }
        
        // Create entries from the daily earnings map
        for (i in 0 until days) {
            entries.add(Entry(i.toFloat(), dailyEarnings[i] ?: 0f))
        }
        
        // If no data, show empty state
        if (entries.isEmpty() || entries.all { it.y <= 0f }) {
            lineChart.setNoDataText("No earnings data available")
            lineChart.invalidate()
            return
        }
        
        // Create dataset
        val dataSet = LineDataSet(entries, "Earnings Trends")
        dataSet.color = getColor(requireContext(), R.color.progress)
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 10f
        dataSet.setCircleColor(getColor(requireContext(), R.color.progress))
        dataSet.circleRadius = 4f
        
        // Create LineData object
        val lineData = LineData(dataSet)
        
        // Customize the Line Chart
        lineChart.data = lineData
        lineChart.description.isEnabled = false
        lineChart.axisLeft.axisMinimum = 0f
        lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        lineChart.xAxis.granularity = 1f
        
        // Adjust x-axis labels based on the period
        when (days) {
            PERIOD_WEEK -> {
                lineChart.xAxis.valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(
                    arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                )
                lineChart.xAxis.labelCount = 7
            }
            PERIOD_MONTH -> {
                lineChart.xAxis.labelCount = 10
            }
            PERIOD_YEAR -> {
                lineChart.xAxis.labelCount = 12
            }
        }
        
        lineChart.legend.isEnabled = true
        lineChart.animateX(1000)
        lineChart.invalidate()
    }
}