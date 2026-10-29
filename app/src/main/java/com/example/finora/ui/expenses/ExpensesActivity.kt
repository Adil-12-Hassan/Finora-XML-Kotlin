package com.example.finora.ui.expenses

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finora.data.db.FinoraDatabase
import com.example.finora.databinding.ActivityExpensesBinding
import com.example.finora.network.SyncManager
import com.example.finora.ui.dashboard.TransactionAdapter
import com.example.finora.util.SessionManager
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ExpensesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExpensesBinding
    private lateinit var session: SessionManager
    private lateinit var db: FinoraDatabase
    private lateinit var adapter: TransactionAdapter
    private var selectedMonth: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExpensesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)
        db = FinoraDatabase.getDatabase(this)

        setupRecyclerView()
        setupMonthChips()
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
        binding.rvExpenses.layoutManager = LinearLayoutManager(this)
        binding.rvExpenses.adapter = adapter
    }

    private fun setupMonthChips() {
        val calendar = Calendar.getInstance()
        val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val labelFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())

        val months = mutableListOf<Pair<String, String>>()
        repeat(6) {
            months.add(
                Pair(
                    monthFormat.format(calendar.time),
                    labelFormat.format(calendar.time)
                )
            )
            calendar.add(Calendar.MONTH, -1)
        }

        months.forEachIndexed { index, (value, label) ->
            val chip = Chip(this)
            chip.text = label
            chip.isCheckable = true
            chip.isChecked = index == 0
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) loadTransactions(value)
            }
            binding.chipGroupMonths.addView(chip)
        }

        selectedMonth = months.first().first
        loadTransactions(selectedMonth)
    }

    private fun loadTransactions(monthYear: String) {
        selectedMonth = monthYear
        val userId = session.getUserId()

        lifecycleScope.launch {
            // 1. Fetch from MongoDB → store into Room
            SyncManager.syncTransactions(this@ExpensesActivity, monthYear)

            // 2. Read from Room (now has fresh data) → update UI
            db.transactionDao().getByMonth(userId, monthYear).collectLatest { list ->
                if (list.isEmpty()) {
                    binding.rvExpenses.visibility = View.GONE
                    binding.layoutEmpty.visibility = View.VISIBLE
                } else {
                    binding.rvExpenses.visibility = View.VISIBLE
                    binding.layoutEmpty.visibility = View.GONE
                    adapter.submitList(list)
                }
            }
        }
    }
}