package org.example.app.ui.analytics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.example.app.R
import org.example.app.databinding.ItemExperimentResultBinding
import java.text.NumberFormat
import kotlin.math.abs

class ExperimentResultsAdapter :
    ListAdapter<ExperimentResult, ExperimentResultsAdapter.ViewHolder>(ExperimentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExperimentResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemExperimentResultBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(experiment: ExperimentResult) {
            binding.apply {
                experimentNameText.text = formatExperimentName(experiment.id)
                variantText.text = "Variant: ${experiment.variant}"

                val percentFormat = NumberFormat.getPercentInstance()
                conversionRateText.text = "Conversion: ${percentFormat.format(experiment.conversionRate)}"

                val improvement = experiment.improvement
                val improvementColor = when {
                    improvement > 0 -> R.color.positive_green
                    improvement < 0 -> R.color.negative_red
                    else -> R.color.gray
                }

                improvementText.setTextColor(
                    ContextCompat.getColor(root.context, improvementColor)
                )
                improvementText.text = buildImprovementText(improvement)
            }
        }

        private fun formatExperimentName(id: String): String {
            return id.split("_")
                .joinToString(" ") { it.capitalize() }
        }

        private fun buildImprovementText(improvement: Double): String {
            val percentFormat = NumberFormat.getPercentInstance()
            return when {
                improvement > 0 -> "↑ ${percentFormat.format(abs(improvement))}"
                improvement < 0 -> "↓ ${percentFormat.format(abs(improvement))}"
                else -> "No change"
            }
        }
    }

    private class ExperimentDiffCallback : DiffUtil.ItemCallback<ExperimentResult>() {
        override fun areItemsTheSame(oldItem: ExperimentResult, newItem: ExperimentResult): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ExperimentResult, newItem: ExperimentResult): Boolean {
            return oldItem == newItem
        }
    }
}
