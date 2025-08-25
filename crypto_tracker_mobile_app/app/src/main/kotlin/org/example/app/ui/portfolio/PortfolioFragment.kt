package org.example.app.ui.portfolio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.example.app.R
import org.example.app.databinding.FragmentPortfolioBinding
import org.example.app.ui.adapters.PortfolioAdapter
import java.text.NumberFormat

class PortfolioFragment : Fragment() {
    private var _binding: FragmentPortfolioBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PortfolioViewModel by viewModels()
    private lateinit var portfolioAdapter: PortfolioAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPortfolioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeUiState()
        setupAddButton()
    }

    private fun setupRecyclerView() {
        portfolioAdapter = PortfolioAdapter()
        binding.portfolioRecyclerView.apply {
            adapter = portfolioAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is PortfolioUiState.Loading -> {
                        // Show loading state
                    }
                    is PortfolioUiState.Success -> {
                        updatePortfolioUI(state)
                    }
                    is PortfolioUiState.Error -> {
                        // Show error message
                    }
                }
            }
        }
    }

    private fun updatePortfolioUI(state: PortfolioUiState.Success) {
        val formatter = NumberFormat.getCurrencyInstance()
        binding.totalValueText.text = formatter.format(state.totalValue)
        
        val profitLossColor = if (state.totalProfitLoss >= 0) {
            R.color.positive_green
        } else {
            R.color.negative_red
        }
        binding.profitLossText.setTextColor(requireContext().getColor(profitLossColor))
        binding.profitLossText.text = String.format(
            "%s (%+.2f%%)",
            formatter.format(state.totalProfitLoss),
            state.totalProfitLoss / (state.totalValue - state.totalProfitLoss) * 100
        )
        
        portfolioAdapter.submitList(state.items)
    }

    private fun setupAddButton() {
        binding.addFab.setOnClickListener {
            // Show add portfolio item dialog
            showAddPortfolioItemDialog()
        }
    }

    private fun showAddPortfolioItemDialog() {
        // Implement dialog to add new portfolio item
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
