package org.example.app.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.example.app.R
import org.example.app.data.model.Coin
import org.example.app.databinding.ItemCoinBinding
import java.text.NumberFormat
import kotlin.math.abs

class CoinAdapter(private val onItemClick: (Coin) -> Unit) :
    ListAdapter<Coin, CoinAdapter.CoinViewHolder>(CoinDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoinViewHolder {
        val binding = ItemCoinBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CoinViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: CoinViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CoinViewHolder(
        private val binding: ItemCoinBinding,
        private val onItemClick: (Coin) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(coin: Coin) {
            binding.apply {
                Glide.with(coinImage)
                    .load(coin.image)
                    .circleCrop()
                    .into(coinImage)

                symbolText.text = coin.symbol.uppercase()
                nameText.text = coin.name
                
                val formatter = NumberFormat.getCurrencyInstance()
                priceText.text = formatter.format(coin.currentPrice)

                val change = coin.priceChangePercentage24h
                val changeColor = if (change >= 0) R.color.positive_green else R.color.negative_red
                changeText.setTextColor(itemView.context.getColor(changeColor))
                changeText.text = String.format("%+.2f%%", change)

                root.setOnClickListener { onItemClick(coin) }
            }
        }
    }

    private class CoinDiffCallback : DiffUtil.ItemCallback<Coin>() {
        override fun areItemsTheSame(oldItem: Coin, newItem: Coin): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Coin, newItem: Coin): Boolean {
            return oldItem == newItem
        }
    }
}
