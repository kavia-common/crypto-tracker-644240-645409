package org.example.app.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.example.app.R
import org.example.app.databinding.ItemPortfolioBinding
import org.example.app.ui.portfolio.PortfolioItemUi
import java.text.NumberFormat

class PortfolioAdapter :
    ListAdapter<PortfolioItemUi, PortfolioAdapter.PortfolioViewHolder>(PortfolioDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PortfolioViewHolder {
        val binding = ItemPortfolioBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PortfolioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PortfolioViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PortfolioViewHolder(
        private val binding: ItemPortfolioBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PortfolioItemUi) {
            val formatter = NumberFormat.getCurrencyInstance()
            
            binding.apply {
                Glide.with(coinImage)
                    .load(item.image)
                    .circleCrop()
                    .into(coinImage)

                symbolText.text = item.symbol.uppercase()
                nameText.text = item.name
                quantityText.text = String.format("%.4f", item.quantity)
                valueText.text = formatter.format(item.totalValue)

                val profitLossColor = if (item.profitLoss >= 0) {
                    R.color.positive_green
                } else {
                    R.color.negative_red
                }
                
                profitLossText.setTextColor(itemView.context.getColor(profitLossColor))
                profitLossText.text = String.format(
                    "%s (%+.2f%%)",
                    formatter.format(item.profitLoss),
                    item.profitLossPercentage
                )
            }
        }
    }

    private class PortfolioDiffCallback : DiffUtil.ItemCallback<PortfolioItemUi>() {
        override fun areItemsTheSame(oldItem: PortfolioItemUi, newItem: PortfolioItemUi): Boolean {
            return oldItem.coinId == newItem.coinId
        }

        override fun areContentsTheSame(oldItem: PortfolioItemUi, newItem: PortfolioItemUi): Boolean {
            return oldItem == newItem
        }
    }
}
