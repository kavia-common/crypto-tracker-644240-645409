package org.example.app.ui.detail

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.example.app.R
import org.example.app.databinding.FragmentCoinDetailBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class CoinDetailFragment : Fragment() {
    private var _binding: FragmentCoinDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CoinDetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCoinDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupChart()
        setupTimeframeChips()
        observeUiState()
        observeWatchlistState()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupChart() {
        binding.priceChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            setDrawGridBackground(false)
            setDrawBorders(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                    private val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                    override fun getFormattedValue(value: Float): String {
                        return dateFormat.format(Date(value.toLong()))
                    }
                }
            }

            axisRight.isEnabled = false
            axisLeft.setDrawGridLines(false)
        }
    }

    private fun setupTimeframeChips() {
        binding.timeframeChipGroup.setOnCheckedChangeListener { group, checkedId ->
            val days = when (checkedId) {
                R.id.chip24h -> 1
                R.id.chip7d -> 7
                R.id.chip30d -> 30
                R.id.chip1y -> 365
                else -> 1
            }
            (viewModel.uiState.value as? CoinDetailUiState.Success)?.let { state ->
                viewModel.loadChartData(state.coin, days)
            }
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is CoinDetailUiState.Loading -> {
                        // Show loading state
                    }
                    is CoinDetailUiState.Success -> {
                        updateUI(state)
                        updateChart(state)
                    }
                    is CoinDetailUiState.Error -> {
                        // Show error state
                    }
                }
            }
        }
    }

    private fun updateUI(state: CoinDetailUiState.Success) {
        val formatter = NumberFormat.getCurrencyInstance()
        
        binding.apply {
            toolbar.title = state.coin.name
            
            Glide.with(requireContext())
                .load(state.coin.image)
                .circleCrop()
                .into(coinImage)

            symbolText.text = state.coin.symbol.uppercase()
            nameText.text = state.coin.name
            priceText.text = formatter.format(state.coin.currentPrice)

            val changeColor = if (state.coin.priceChangePercentage24h >= 0) {
                R.color.positive_green
            } else {
                R.color.negative_red
            }
            changeText.setTextColor(requireContext().getColor(changeColor))
            changeText.text = String.format("%+.2f%%", state.coin.priceChangePercentage24h)
        }
    }

    private fun updateChart(state: CoinDetailUiState.Success) {
        val entries = state.chartData.prices.map { price ->
            Entry(price[0].toFloat(), price[1].toFloat())
        }

        val dataSet = LineDataSet(entries, "Price").apply {
            color = requireContext().getColor(R.color.primary)
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 2f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            fillColor = requireContext().getColor(R.color.primary)
            fillAlpha = 30
            setDrawFilled(true)
        }

        binding.priceChart.data = LineData(dataSet)
        binding.priceChart.invalidate()
    }

    private fun observeWatchlistState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.inWatchlist.collect { inWatchlist ->
                binding.addToWatchlistButton.text = if (inWatchlist) {
                    "Remove from Watchlist"
                } else {
                    "Add to Watchlist"
                }
            }
        }

        binding.addToWatchlistButton.setOnClickListener {
            viewModel.toggleWatchlist()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
