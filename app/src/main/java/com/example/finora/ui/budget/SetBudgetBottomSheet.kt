package com.example.finora.ui.budget

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.example.finora.data.db.FinoraDatabase
import com.example.finora.data.model.Budget
import com.example.finora.databinding.BottomSheetSetBudgetBinding
import com.example.finora.network.CategoryMapper
import com.example.finora.network.RetrofitClient
import com.example.finora.network.dto.SetBudgetRequest
import com.example.finora.util.SessionManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SetBudgetBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSetBudgetBinding? = null
    private val binding get() = _binding!!

    private var categoryName: String = ""
    private var currentLimit: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            categoryName = it.getString(ARG_CATEGORY, "")
            currentLimit = it.getDouble(ARG_LIMIT, 0.0)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSetBudgetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = SessionManager(requireContext())
        val db = FinoraDatabase.getDatabase(requireContext())

        binding.tvSheetCategory.text = categoryName

        // Pre-fill existing limit
        if (currentLimit > 0) {
            binding.etLimit.setText(String.format("%.0f", currentLimit))
        }

        binding.btnSaveLimit.setOnClickListener {
            val limitStr = binding.etLimit.text.toString().trim()
            val limit = limitStr.toDoubleOrNull()
            if (limit == null || limit <= 0.0) {
                binding.tilLimit.error = "Enter a valid limit"
                return@setOnClickListener
            }
            binding.tilLimit.error = null

            val monthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                .format(Date())

            val budget = Budget(
                userId = session.getUserId(),
                monthYear = monthYear,
                category = categoryName,
                limitAmount = limit
            )

            lifecycleScope.launch(Dispatchers.IO) {
                db.budgetDao().insertOrUpdate(budget)
                val apiCategory = CategoryMapper.toApiCategory(categoryName)
                val bearer = session.getBearerToken()
                if (apiCategory != null && bearer != null) {
                    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                        try {
                            val response = RetrofitClient.api.setBudget(
                                bearer,
                                SetBudgetRequest(apiCategory, limit, monthYear)
                            )
                            if (!response.isSuccessful) {
                                Log.w("FinoraSync", "Budget sync failed: HTTP ${response.code()}")
                            }
                        } catch (e: Exception) {
                            Log.w("FinoraSync", "Budget saved locally but could not be synced", e)
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
        private const val ARG_CATEGORY = "arg_category"
        private const val ARG_LIMIT = "arg_limit"

        fun newInstance(item: CategoryBudgetItem): SetBudgetBottomSheet {
            return SetBudgetBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_CATEGORY, item.category)
                    putDouble(ARG_LIMIT, item.limit)
                }
            }
        }
    }
}
