package com.example.finora.ui.dashboard

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.finora.R
import com.example.finora.data.model.Transaction
import com.example.finora.databinding.ItemTransactionBinding

class TransactionAdapter(
    private val onItemClick: (Transaction) -> Unit = {}
) : ListAdapter<Transaction, TransactionAdapter.ViewHolder>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Transaction>() {
            override fun areItemsTheSame(a: Transaction, b: Transaction) = a.id == b.id
            override fun areContentsTheSame(a: Transaction, b: Transaction) = a == b
        }
    }

    inner class ViewHolder(val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tx = getItem(position)
        val ctx = holder.itemView.context

        holder.binding.tvTransactionName.text = if (tx.note.isNotEmpty()) tx.note else tx.category
        holder.binding.tvTransactionCategory.text = tx.category

        // Amount color
        if (tx.type == "income") {
            holder.binding.tvTransactionAmount.text = "+ PKR ${String.format("%,.0f", tx.amount)}"
            holder.binding.tvTransactionAmount.setTextColor(
                ContextCompat.getColor(ctx, R.color.income_text)
            )
        } else {
            holder.binding.tvTransactionAmount.text = "- PKR ${String.format("%,.0f", tx.amount)}"
            holder.binding.tvTransactionAmount.setTextColor(
                ContextCompat.getColor(ctx, R.color.ink)
            )
        }

        // Icon background color by category
        val style = getCategoryStyle(tx.category, ctx)
        val bgColor = style.first
        val iconRes = style.second

        val shape = GradientDrawable()
        shape.shape = GradientDrawable.RECTANGLE
        shape.cornerRadius = 24f
        shape.setColor(bgColor)
        holder.binding.iconBg.background = shape
        holder.binding.ivCategoryIcon.setImageResource(iconRes)

        holder.itemView.setOnClickListener { onItemClick(tx) }
    }

    private fun getCategoryStyle(category: String, ctx: Context): Pair<Int, Int> {
        return when (category) {
            "Food & Dining" -> Pair(
                ContextCompat.getColor(ctx, R.color.cat_food_bg), R.drawable.ic_receipt
            )
            "Transport" -> Pair(
                ContextCompat.getColor(ctx, R.color.cat_transport_bg), R.drawable.ic_receipt
            )
            "Shopping" -> Pair(
                ContextCompat.getColor(ctx, R.color.cat_shopping_bg), R.drawable.ic_receipt
            )
            "Health" -> Pair(
                ContextCompat.getColor(ctx, R.color.cat_health_bg), R.drawable.ic_receipt
            )
            "Entertainment" -> Pair(
                ContextCompat.getColor(ctx, R.color.cat_entertainment_bg), R.drawable.ic_receipt
            )
            "Bills & Utilities" -> Pair(
                ContextCompat.getColor(ctx, R.color.cat_bills_bg), R.drawable.ic_receipt
            )
            "Income" -> Pair(
                ContextCompat.getColor(ctx, R.color.income_bg), R.drawable.ic_receipt
            )
            else -> Pair(
                ContextCompat.getColor(ctx, R.color.cat_other_bg), R.drawable.ic_receipt
            )
        }
    }
}
