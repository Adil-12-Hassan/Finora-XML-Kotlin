package com.example.finora.ui.budget

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.finora.databinding.ItemBudgetCategoryBinding

data class CategoryBudgetItem(
    val category: String,
    val spent: Double,
    val limit: Double
)

class BudgetAdapter(
    private val onEditClick: (CategoryBudgetItem) -> Unit
) : ListAdapter<CategoryBudgetItem, BudgetAdapter.ViewHolder>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<CategoryBudgetItem>() {
            override fun areItemsTheSame(a: CategoryBudgetItem, b: CategoryBudgetItem) =
                a.category == b.category
            override fun areContentsTheSame(a: CategoryBudgetItem, b: CategoryBudgetItem) =
                a == b
        }
    }

    inner class ViewHolder(val binding: ItemBudgetCategoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBudgetCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val percent = if (item.limit > 0)
            ((item.spent / item.limit) * 100).toInt().coerceIn(0, 100) else 0

        holder.binding.tvCategory.text = item.category
        holder.binding.tvPercent.text = "$percent%"
        holder.binding.tvAmounts.text =
            "PKR ${String.format("%,.0f", item.spent)} / ${String.format("%,.0f", item.limit)}"
        holder.binding.progressCategory.progress = percent

        // Red when over 90%
        val color = if (percent >= 90)
            Color.parseColor("#F87171") else Color.parseColor("#4F46E5")
        holder.binding.progressCategory.progressTintList =
            android.content.res.ColorStateList.valueOf(color)

        holder.itemView.setOnClickListener { onEditClick(item) }
    }
}