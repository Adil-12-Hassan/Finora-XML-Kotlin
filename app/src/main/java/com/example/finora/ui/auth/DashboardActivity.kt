package com.example.finora.ui.dashboard

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finora.data.db.FinoraDatabase
import com.example.finora.databinding.ActivityDashboardBinding
import com.example.finora.util.SessionManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import com.example.finora.R
import com.example.finora.network.SyncManager
import com.example.finora.ui.expenses.ExpensesActivity
import com.example.finora.ui.reports.ReportsActivity
import com.example.finora.ui.budget.BudgetActivity
import com.example.finora.ui.settings.SettingsActivity
import kotlinx.coroutines.Dispatchers
import com.example.finora.util.UpdateManager
class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var session: SessionManager
    private lateinit var db: FinoraDatabase
    private lateinit var adapter: TransactionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)
        db = FinoraDatabase.getDatabase(this)

        lifecycleScope.launch {
            SyncManager.syncAll(this@DashboardActivity)
        }

        UpdateManager.checkForUpdate(this)

        setupUI()
        setupRecyclerView()
        observeTransactions()
    }

    private fun setupUI() {
        // Greeting
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        binding.tvGreeting.text = when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
        binding.tvUserName.text = session.getUserName()

        // Month label
        val monthYear = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
        binding.tvMonthLabel.text = "$monthYear — Monthly budget"

        // FAB
        binding.fab.setOnClickListener {
            AddTransactionBottomSheet().show(supportFragmentManager, "add_tx")
        }

        // Bottom nav (placeholder — other screens coming soon)
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_expenses -> {
                    startActivity(Intent(this, ExpensesActivity::class.java))
                    true
                }
                R.id.nav_reports -> {
                    startActivity(Intent(this, ReportsActivity::class.java))
                    true
                }
                R.id.nav_budget -> {
                    startActivity(Intent(this, BudgetActivity::class.java))
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> true
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter { tx ->
            AlertDialog.Builder(this)
                .setTitle("Delete Transaction")
                .setMessage("Are you sure you want to delete this transaction?")
                .setPositiveButton("Delete") { _, _ ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        db.transactionDao().delete(tx)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        binding.rvTransactions.layoutManager = LinearLayoutManager(this)
        binding.rvTransactions.adapter = adapter
    }

    private fun observeTransactions() {
        val userId = session.getUserId()
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        lifecycleScope.launch {
            db.transactionDao().getByMonth(userId, currentMonth).collectLatest { list ->
                // Show/hide empty state
                if (list.isEmpty()) {
                    binding.layoutEmpty.visibility = View.VISIBLE
                    binding.rvTransactions.visibility = View.GONE
                } else {
                    binding.layoutEmpty.visibility = View.GONE
                    binding.rvTransactions.visibility = View.VISIBLE
                    adapter.submitList(list.take(5))  // show 5 most recent on dashboard
                }

                // Calculate totals
                val baseBudget = session.getMonthlyBudget().toDouble()
                val income = list.filter { it.type == "income" }.sumOf { it.amount }
                val spent = list.filter { it.type == "expense" }.sumOf { it.amount }
                val budget = baseBudget + income
                val remaining = budget - spent
                val percent = if (budget > 0) ((spent / budget) * 100).toInt().coerceIn(0, 100) else 0

                binding.tvBudgetAmount.text = "PKR ${String.format("%,.0f", budget)}"
                binding.tvTotalSpent.text = "PKR ${String.format("%,.0f", spent)}"
                binding.tvRemaining.text = "PKR ${String.format("%,.0f", remaining)}"
                binding.tvSpentLabel.text = "Spent: PKR ${String.format("%,.0f", spent)}"
                binding.tvPercentLabel.text = "$percent% used"
                binding.progressBudget.progress = percent
            }
        }
    }
}