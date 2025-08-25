package org.example.app.ui.watchlist

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
import org.example.app.databinding.FragmentWatchlistBinding
import org.example.app.ui.adapters.CoinAdapter

class WatchlistFragment : Fragment() {
    private var _binding: FragmentWatchlistBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WatchlistViewModel by viewModels()
    private lateinit var coinAdapter: CoinAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWatchlistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeUiState()
        setupRefresh()
        setupAddButton()
    }

    private fun setupRecyclerView() {
        coinAdapter = CoinAdapter { coin ->
            findNavController().navigate(
                WatchlistFragmentDirections.actionWatchlistToDetail(coin.id)
            )
        }
        binding.watchlistRecyclerView.apply {
            adapter = coinAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is WatchlistUiState.Loading -> {
                        binding.swipeRefresh.isRefreshing = true
                        binding.emptyText.visibility = View.GONE
                    }
                    is WatchlistUiState.Empty -> {
                        binding.swipeRefresh.isRefreshing = false
                        binding.emptyText.visibility = View.VISIBLE
                        coinAdapter.submitList(emptyList())
                    }
                    is WatchlistUiState.Success -> {
                        binding.swipeRefresh.isRefreshing = false
                        binding.emptyText.visibility = View.GONE
                        coinAdapter.submitList(state.coins)
                    }
                    is WatchlistUiState.Error -> {
                        binding.swipeRefresh.isRefreshing = false
                        // Show error message
                    }
                }
            }
        }
    }

    private fun setupRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadWatchlist()
        }
    }

    private fun setupAddButton() {
        binding.addFab.setOnClickListener {
            // Show add to watchlist dialog
            showAddToWatchlistDialog()
        }
    }

    private fun showAddToWatchlistDialog() {
        // Implement dialog to add new watchlist item
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
