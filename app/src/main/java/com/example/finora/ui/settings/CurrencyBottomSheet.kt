package com.example.finora.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finora.R
import com.example.finora.databinding.BottomSheetCurrencyBinding
import com.example.finora.util.SessionManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CurrencyBottomSheet(
    private val onSelected: (String, String) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCurrencyBinding? = null
    private val binding get() = _binding!!

    private val currencies = listOf(
        Pair("PKR", "Pakistani Rupee"),
        Pair("USD", "US Dollar"),
        Pair("EUR", "Euro"),
        Pair("GBP", "British Pound"),
        Pair("AED", "UAE Dirham"),
        Pair("SAR", "Saudi Riyal"),
        Pair("INR", "Indian Rupee"),
        Pair("CAD", "Canadian Dollar"),
        Pair("AUD", "Australian Dollar")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetCurrencyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = SessionManager(requireContext())

        binding.rvCurrencies.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCurrencies.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val tv = TextView(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    setPadding(16, 40, 16, 40)
                    textSize = 14f
                    setTextColor(resources.getColor(R.color.ink, null))
                    background = context.obtainStyledAttributes(
                        intArrayOf(android.R.attr.selectableItemBackground)
                    ).getDrawable(0)
                }
                return object : RecyclerView.ViewHolder(tv) {}
            }

            override fun getItemCount() = currencies.size

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val (code, name) = currencies[position]
                (holder.itemView as TextView).apply {
                    text = "$code — $name"
                    setOnClickListener {
                        session.setCurrency(code, name)
                        onSelected(code, name)
                        dismiss()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}