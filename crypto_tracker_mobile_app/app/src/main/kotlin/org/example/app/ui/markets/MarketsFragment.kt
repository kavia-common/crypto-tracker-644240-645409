package org.example.app.ui.markets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.example.app.databinding.FragmentMarketsBinding
import org.example.app.ui.adapters.CoinAdapter

class MarketsFragment : Fragment() {
    private var _binding: FragmentMarketsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MarketsViewModel by viewModels()
    private lateinit var coinAdapter: CoinAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMarketsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeUiState()
        setupSwipeRefresh()
    }

    private fun setupRecyclerView() {
        coinAdapter = CoinAdapter { coin ->
            findNavController().navigate(
                MarketsFragmentDirections.actionMarketsToCoinDetail(coin.id)
            )
        }
        binding.recyclerView.apply {
            adapter = coinAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is MarketsUiState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.recyclerView.visibility = View.GONE
                    }
                    is MarketsUiState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.recyclerView.visibility = View.VISIBLE
                        coinAdapter.submitList(state.coins)
                    }
                    is MarketsUiState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        // Show error message
                    }
                }
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadCoins()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
