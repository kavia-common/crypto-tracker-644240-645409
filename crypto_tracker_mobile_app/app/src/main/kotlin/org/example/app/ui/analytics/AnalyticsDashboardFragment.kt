package org.example.app.ui.analytics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.example.app.databinding.FragmentAnalyticsDashboardBinding
import org.example.app.ui.base.AnalyticsFragment
import java.text.NumberFormat

class AnalyticsDashboardFragment : AnalyticsFragment() {
    private var _binding: FragmentAnalyticsDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AnalyticsDashboardViewModel by viewModels()
    private lateinit var experimentAdapter: ExperimentResultsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalyticsDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCharts()
        setupExperimentsList()
        observeUiState()
    }

    private fun setupCharts() {
        ChartUtils.setupEngagementChart(binding.engagementChart, requireContext())
        ChartUtils.setupCrashChart(binding.crashTypesChart, requireContext())
    }

    private fun setupExperimentsList() {
        experimentAdapter = ExperimentResultsAdapter()
        binding.experimentsRecyclerView.apply {
            adapter = experimentAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is AnalyticsDashboardState.Loading -> {
                        // Show loading state
                    }
                    is AnalyticsDashboardState.Success -> {
                        updateDashboard(state)
                    }
                    is AnalyticsDashboardState.Error -> {
                        showError(state.message)
                    }
                }
            }
        }
    }

    private fun updateDashboard(state: AnalyticsDashboardState.Success) {
        // Update engagement chart
        ChartUtils.updateEngagementChart(
            binding.engagementChart,
            state.engagementData,
            requireContext()
        )

        // Update experiments list
        experimentAdapter.submitList(state.experimentResults)

        // Update crash analytics
        val percentFormat = NumberFormat.getPercentInstance()
        binding.crashRateText.text = "Crash Rate: ${percentFormat.format(state.crashAnalytics.crashRate)}"
        
        ChartUtils.updateCrashChart(
            binding.crashTypesChart,
            state.crashAnalytics.crashTypes,
            requireContext()
        )
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun getScreenName(): String = "Analytics Dashboard"

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
