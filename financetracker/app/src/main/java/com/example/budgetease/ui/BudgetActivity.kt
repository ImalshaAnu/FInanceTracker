package com.example.budgetease.ui


import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.budgetease.R
import com.example.budgetease.data.PreferencesManager
import com.example.budgetease.data.TransactionRepository
import com.example.budgetease.databinding.ActivityBudgetBinding
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BudgetActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBudgetBinding
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var preferencesManager: PreferencesManager

    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBudgetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        transactionRepository = TransactionRepository(this)
        preferencesManager = PreferencesManager(this)

        setupBottomNavigation()
        setupMonthSelector()
        setupBudgetDisplay()
        setupBudgetChart()

        binding.btnSetBudget.setOnClickListener {
            val intent = Intent(this, SetBudgetActivity::class.java).apply {
                putExtra("month", calendar.get(Calendar.MONTH))
                putExtra("year", calendar.get(Calendar.YEAR))
            }
            startActivity(intent)
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.nav_budget
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_transactions -> {
                    startActivity(Intent(this, TransactionsActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_budget -> true
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupMonthSelector() {
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        binding.tvCurrentMonth.text = dateFormat.format(calendar.time)

        binding.btnPreviousMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateBudgetDisplay()
            binding.tvCurrentMonth.text = dateFormat.format(calendar.time)
        }

        binding.btnNextMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateBudgetDisplay()
            binding.tvCurrentMonth.text = dateFormat.format(calendar.time)
        }
    }

    private fun setupBudgetDisplay() {
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)
        val budget = preferencesManager.getBudget()
        val currency = preferencesManager.getCurrency()

        if (budget.month == month && budget.year == year && budget.amount > 0) {
            binding.tvNoBudget.visibility = android.view.View.GONE
            binding.cardBudgetInfo.visibility = android.view.View.VISIBLE

            val totalExpenses = transactionRepository.getTotalExpensesForMonth(month, year)
            val remaining = budget.amount - totalExpenses
            val percentage = (totalExpenses / budget.amount) * 100

            binding.tvBudgetAmount.text = String.format("%s %.2f", currency, budget.amount)
            binding.tvExpensesAmount.text = String.format("%s %.2f", currency, totalExpenses)
            binding.tvRemainingAmount.text = String.format("%s %.2f", currency, remaining)

            binding.progressBudget.progress = percentage.toInt().coerceAtMost(100)
            binding.tvBudgetPercentage.text = String.format("%.1f%%", percentage)

            if (percentage >= 100) {
                binding.tvBudgetStatus.text = getString(R.string.budget_exceeded)
                binding.tvBudgetStatus.setTextColor(Color.RED)
                binding.tvRemainingAmount.setTextColor(Color.RED)
            } else if (percentage >= 80) {
                binding.tvBudgetStatus.text = getString(R.string.budget_warning)
                binding.tvBudgetStatus.setTextColor(Color.parseColor("#FFA500")) // Orange
                binding.tvRemainingAmount.setTextColor(Color.parseColor("#FFA500"))
            } else {
                binding.tvBudgetStatus.text = getString(R.string.budget_good)
                binding.tvBudgetStatus.setTextColor(Color.GREEN)
                binding.tvRemainingAmount.setTextColor(Color.GREEN)
            }
        } else {
            binding.tvNoBudget.visibility = android.view.View.VISIBLE
            binding.cardBudgetInfo.visibility = android.view.View.GONE
        }
    }

    private fun setupBudgetChart() {
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)
        val incomeByCategory = transactionRepository.getIncomeByCategory(month, year)

        if (incomeByCategory.isEmpty()) {
            binding.barChart.setNoDataText(getString(R.string.no_income_this_month))
            binding.barChart.invalidate()
            return
        }

        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()

        incomeByCategory.entries.forEachIndexed { index, entry ->
            entries.add(PieEntry(entry.value.toFloat(), entry.key))
            colors.add(ColorTemplate.MATERIAL_COLORS[index % ColorTemplate.MATERIAL_COLORS.size])
        }

        val dataSet = PieDataSet(entries, "Income by Category")
        dataSet.colors = colors
        dataSet.valueTextSize = 12f

        val pieData = PieData(dataSet)
        binding.barChart.data = pieData
        binding.barChart.description.isEnabled = false
        binding.barChart.animateY(1000)
        binding.barChart.setDrawHoleEnabled(true)
        binding.barChart.setHoleColor(Color.WHITE)
        binding.barChart.setTransparentCircleRadius(30f)
        binding.barChart.setHoleRadius(30f)
        binding.barChart.invalidate()
    }

    private fun updateBudgetDisplay() {
        setupBudgetDisplay()
        setupBudgetChart()
    }

    override fun onResume() {
        super.onResume()
        updateBudgetDisplay()
    }
}