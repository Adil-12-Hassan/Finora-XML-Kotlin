package com.example.finora.ui.reports

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.finora.data.db.FinoraDatabase
import com.example.finora.data.model.Transaction
import com.example.finora.databinding.ActivityReportsBinding
import com.example.finora.util.SessionManager
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReportsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportsBinding
    private lateinit var session: SessionManager
    private lateinit var db: FinoraDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)
        db = FinoraDatabase.getDatabase(this)

        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val monthLabel = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
        binding.tvReportMonth.text = monthLabel

        loadReports(currentMonth)
    }

    private fun loadReports(monthYear: String) {
        val userId = session.getUserId()

        lifecycleScope.launch {
            db.transactionDao().getByMonth(userId, monthYear).collectLatest { list ->
                val expenses = list.filter { it.type == "expense" }
                val incomes = list.filter { it.type == "income" }

                val totalExpense = expenses.sumOf { it.amount }
                val totalIncome = incomes.sumOf { it.amount }

                binding.tvTotalExpense.text = "PKR ${String.format("%,.0f", totalExpense)}"
                binding.tvTotalIncome.text = "PKR ${String.format("%,.0f", totalIncome)}"

                setupBarChart(expenses)
                setupPieChart(expenses)
            }
        }
    }

    private fun setupBarChart(expenses: List<Transaction>) {
        val calendar = Calendar.getInstance()
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Group expenses by day
        val dailyTotals = FloatArray(daysInMonth)
        val dayFormat = SimpleDateFormat("d", Locale.getDefault())

        expenses.forEach { tx ->
            val day = dayFormat.format(Date(tx.date)).toIntOrNull() ?: return@forEach
            if (day in 1..daysInMonth) {
                dailyTotals[day - 1] += tx.amount.toFloat()
            }
        }

        val entries = dailyTotals.mapIndexed { index, value ->
            BarEntry((index + 1).toFloat(), value)
        }

        val dataSet = BarDataSet(entries, "Daily spending").apply {
            color = Color.parseColor("#4F46E5")
            setDrawValues(false)
        }

        binding.barChart.apply {
            data = BarData(dataSet).apply { barWidth = 0.6f }
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)
            setDrawGridBackground(false)

            xAxis.apply {
                setDrawGridLines(false)
                textColor = Color.parseColor("#8892A0")
                textSize = 9f
                granularity = 5f
                labelCount = 6
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#E7E9ED")
                textColor = Color.parseColor("#8892A0")
                textSize = 9f
                axisMinimum = 0f
            }

            axisRight.isEnabled = false
            invalidate()
        }
    }

    private fun setupPieChart(expenses: List<Transaction>) {
        if (expenses.isEmpty()) {
            binding.pieChart.clear()
            binding.pieChart.invalidate()
            return
        }

        // Group by category
        val categoryTotals = expenses
            .groupBy { it.category }
            .map { (cat, txList) -> cat to txList.sumOf { it.amount }.toFloat() }
            .sortedByDescending { it.second }

        val entries = categoryTotals.map { (cat, total) ->
            PieEntry(total, cat)
        }

        val colors = listOf(
            Color.parseColor("#4F46E5"),
            Color.parseColor("#FBBF24"),
            Color.parseColor("#EC4899"),
            Color.parseColor("#34D399"),
            Color.parseColor("#F87171"),
            Color.parseColor("#60A5FA"),
            Color.parseColor("#A78BFA"),
            Color.parseColor("#FB923C")
        )

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            sliceSpace = 2f
            setDrawValues(false)
        }

        binding.pieChart.apply {
            data = PieData(dataSet)
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleRadius(45f)
            setTransparentCircleRadius(48f)
            setHoleColor(Color.WHITE)
            setDrawCenterText(false)
            setTouchEnabled(false)

            legend.apply {
                isEnabled = true
                orientation = Legend.LegendOrientation.VERTICAL
                horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                verticalAlignment = Legend.LegendVerticalAlignment.CENTER
                textColor = Color.parseColor("#4B5563")
                textSize = 11f
                form = Legend.LegendForm.CIRCLE
            }

            notifyDataSetChanged()
            invalidate()
        }
    }
}