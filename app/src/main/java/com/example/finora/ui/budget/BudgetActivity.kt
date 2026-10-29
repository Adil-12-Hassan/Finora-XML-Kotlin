package com.example.finora.ui.budget

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finora.data.db.FinoraDatabase
import com.example.finora.databinding.ActivityBudgetBinding
import com.example.finora.util.SessionManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BudgetActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBudgetBinding
    private lateinit var session: SessionManager
    private lateinit var db: FinoraDatabase
    private lateinit var adapter: BudgetAdapter

    private val categories = listOf(
        "Food & Dining", "Transport", "Shopping",
        "Health", "Entertainment", "Bills & Utilities",
        "Education", "Other"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBudgetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)
        db = FinoraDatabase.getDatabase(this)

        val monthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val monthLabel = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
        binding.tvBudgetMonth.text = monthLabel

        setupRecyclerView()
        observeData(monthYear)
    }

    private fun setupRecyclerView() {
        adapter = BudgetAdapter { item ->
            SetBudgetBottomSheet.newInstance(item)
                .show(supportFragmentManager, "set_budget")
        }
        binding.rvBudget.layoutManager = LinearLayoutManager(this)
        binding.rvBudget.adapter = adapter
        binding.rvBudget.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )
    }

    private fun observeData(monthYear: String) {
        val userId = session.getUserId()

        lifecycleScope.launch {
            // Observe transactions
            db.transactionDao().getByMonth(userId, monthYear).collectLatest { transactions ->
                val expenses = transactions.filter { it.type == "expense" }

                // Spending per category
                val spentByCategory = expenses
                    .groupBy { it.category }
                    .mapValues { (_, list) -> list.sumOf { it.amount } }

                // Observe budgets
                db.budgetDao().getBudgetsByMonth(userId, monthYear).collectLatest { budgets ->
                    val limitByCategory = budgets.associate { it.category to it.limitAmount }

                    // Build items for all categories
                    val items = categories.map { cat ->
                        CategoryBudgetItem(
                            category = cat,
                            spent = spentByCategory[cat] ?: 0.0,
                            limit = limitByCategory[cat] ?: 0.0
                        )
                    }

                    adapter.submitList(items)

                    // Overall totals
                    val totalSpent = expenses.sumOf { it.amount }
                    val totalLimit = budgets.sumOf { it.limitAmount }
                    val remaining = totalLimit - totalSpent
                    val percent = if (totalLimit > 0)
                        ((totalSpent / totalLimit) * 100).toInt().coerceIn(0, 100) else 0

                    binding.tvOverallSpent.text =
                        "PKR ${String.format("%,.0f", totalSpent)}"
                    binding.tvOverallLimit.text =
                        "of PKR ${String.format("%,.0f", totalLimit)}"
                    binding.tvOverallRemaining.text =
                        "PKR ${String.format("%,.0f", remaining)} remaining"
                    binding.progressOverall.progress = percent
                }
            }
        }
    }
}