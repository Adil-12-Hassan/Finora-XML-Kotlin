package com.example.finora.ui.dashboard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.lifecycle.lifecycleScope
import com.example.finora.R
import com.example.finora.data.db.FinoraDatabase
import com.example.finora.data.model.Transaction
import com.example.finora.databinding.BottomSheetAddTransactionBinding
import com.example.finora.util.SessionManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import com.example.finora.network.CategoryMapper
import com.example.finora.network.RetrofitClient
import com.example.finora.network.dto.AddExpenseRequest
import java.text.SimpleDateFormat
import java.util.*

class AddTransactionBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetAddTransactionBinding? = null
    private val binding get() = _binding!!

    private var editingTransactionId: Int = 0
    private var editingTransactionDate: Long = System.currentTimeMillis()

    private val categories = listOf(
        "Food & Dining", "Transport", "Shopping",
        "Health", "Entertainment", "Bills & Utilities",
        "Education", "Income", "Other"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            editingTransactionId = it.getInt(ARG_ID, 0)
            editingTransactionDate = it.getLong(ARG_DATE, System.currentTimeMillis())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = SessionManager(requireContext())
        val db = FinoraDatabase.getDatabase(requireContext())

        // Category dropdown
        val adapter = ArrayAdapter(requireContext(), R.layout.list_item_dropdown, categories)
        binding.actvCategory.setAdapter(adapter)

        // Type toggle — default to Expense
        binding.toggleType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                if (checkedId == R.id.btnIncome) {
                    binding.actvCategory.setText("Income", false)
                    binding.actvCategory.isEnabled = false
                    binding.tilCategory.isEnabled = false
                } else {
                    binding.actvCategory.setText("", false)
                    binding.actvCategory.isEnabled = true
                    binding.tilCategory.isEnabled = true
                }
            }
        }
        binding.toggleType.check(R.id.btnExpense)

        // If editing, prefill values
        arguments?.let {
            val amount = it.getDouble(ARG_AMOUNT, 0.0)
            val category = it.getString(ARG_CATEGORY, "")
            val note = it.getString(ARG_NOTE, "")
            val type = it.getString(ARG_TYPE, "expense")

            if (amount > 0.0) binding.etAmount.setText(amount.toString())
            if (category.isNotEmpty()) binding.actvCategory.setText(category, false)
            if (note.isNotEmpty()) binding.etNote.setText(note)
            if (type == "income") {
                binding.toggleType.check(R.id.btnIncome)
            } else {
                binding.toggleType.check(R.id.btnExpense)
            }
            if (editingTransactionId != 0) {
                binding.tvTitle.text = "Edit transaction"
                binding.btnSave.text = "Update transaction"
            }
        }

        binding.btnSave.setOnClickListener {
            val amountStr = binding.etAmount.text.toString().trim()
            val category = binding.actvCategory.text.toString().trim()
            val note = binding.etNote.text.toString().trim()
            val type = if (binding.toggleType.checkedButtonId == R.id.btnIncome) "income" else "expense"

            if (amountStr.isEmpty()) {
                binding.tilAmount.error = "Enter an amount"
                return@setOnClickListener
            }
            if (category.isEmpty()) {
                binding.tilCategory.error = "Select a category"
                return@setOnClickListener
            }

            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0.0) {
                binding.tilAmount.error = "Enter a valid amount"
                return@setOnClickListener
            }

            val transaction = Transaction(
                id = editingTransactionId,
                userId = session.getUserId(),
                amount = amount,
                category = category,
                note = note,
                date = editingTransactionDate,
                type = type,
                isSynced = false
            )

            lifecycleScope.launch(Dispatchers.IO) {
                if (editingTransactionId != 0) {
                    db.transactionDao().update(transaction)
                } else {
                    db.transactionDao().insert(transaction)
                }

                if (type == "expense") {
                    val apiCategory = CategoryMapper.toApiCategory(category)
                    val bearer = session.getBearerToken()
                    if (apiCategory != null && bearer != null) {
                        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                            try {
                                val isoDate = SimpleDateFormat(
                                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                                    Locale.US
                                ).apply {
                                    timeZone = TimeZone.getTimeZone("UTC")
                                }.format(Date(transaction.date))
                                val response = RetrofitClient.api.addExpense(
                                    bearer,
                                    AddExpenseRequest(amount, apiCategory, isoDate, note)
                                )
                                if (!response.isSuccessful) {
                                    Log.w("FinoraSync", "Expense sync failed: HTTP ${response.code()}")
                                }
                            } catch (e: Exception) {
                                Log.w("FinoraSync", "Expense saved locally but could not be synced", e)
                            }
                        }
                    }
                }

                launch(Dispatchers.Main) { dismiss() }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ID = "arg_id"
        private const val ARG_AMOUNT = "arg_amount"
        private const val ARG_CATEGORY = "arg_category"
        private const val ARG_NOTE = "arg_note"
        private const val ARG_DATE = "arg_date"
        private const val ARG_TYPE = "arg_type"

        fun newInstance(tx: Transaction? = null): AddTransactionBottomSheet {
            return AddTransactionBottomSheet().apply {
                if (tx != null) {
                    arguments = Bundle().apply {
                        putInt(ARG_ID, tx.id)
                        putDouble(ARG_AMOUNT, tx.amount)
                        putString(ARG_CATEGORY, tx.category)
                        putString(ARG_NOTE, tx.note)
                        putLong(ARG_DATE, tx.date)
                        putString(ARG_TYPE, tx.type)
                    }
                }
            }
        }
    }
}
