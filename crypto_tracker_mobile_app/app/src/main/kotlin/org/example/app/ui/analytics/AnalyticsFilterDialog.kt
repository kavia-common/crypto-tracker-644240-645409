package org.example.app.ui.analytics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.chip.Chip
import org.example.app.R
import org.example.app.analytics.AnalyticsFilterOptions
import org.example.app.analytics.AnalyticsTimeRange
import org.example.app.databinding.DialogAnalyticsFilterBinding

class AnalyticsFilterDialog : DialogFragment() {
    private var _binding: DialogAnalyticsFilterBinding? = null
    private val binding get() = _binding!!

    private var currentTimeRange = AnalyticsTimeRange.LAST_7_DAYS
    private var currentFilters = AnalyticsFilterOptions()
    private var onFilterApplied: ((AnalyticsTimeRange, AnalyticsFilterOptions) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAnalyticsFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTimeRangeSelection()
        setupDeviceTypeChips()
        setupAppVersionChips()
        setupButtons()
        restoreCurrentSelection()
    }

    private fun setupTimeRangeSelection() {
        binding.timeRangeGroup.setOnCheckedChangeListener { _, checkedId ->
            currentTimeRange = when (checkedId) {
                R.id.last24Hours -> AnalyticsTimeRange.LAST_24_HOURS
                R.id.last7Days -> AnalyticsTimeRange.LAST_7_DAYS
                R.id.last30Days -> AnalyticsTimeRange.LAST_30_DAYS
                R.id.last90Days -> AnalyticsTimeRange.LAST_90_DAYS
                else -> AnalyticsTimeRange.LAST_7_DAYS
            }
        }
    }

    private fun setupDeviceTypeChips() {
        val deviceTypes = listOf("Android Phone", "Android Tablet", "Chrome OS")
        deviceTypes.forEach { deviceType ->
            binding.deviceTypeChips.addView(createFilterChip(deviceType))
        }
    }

    private fun setupAppVersionChips() {
        val appVersions = listOf("1.0.0", "1.1.0", "1.2.0")
        appVersions.forEach { version ->
            binding.appVersionChips.addView(createFilterChip(version))
        }
    }

    private fun createFilterChip(text: String): Chip {
        return Chip(requireContext()).apply {
            this.text = text
            isCheckable = true
            setChipBackgroundColorResource(R.color.white)
            setTextColor(ContextCompat.getColor(context, R.color.primary))
            checkedIcon = ContextCompat.getDrawable(context, R.drawable.ic_check)
        }
    }

    private fun setupButtons() {
        binding.resetButton.setOnClickListener {
            resetFilters()
        }

        binding.applyButton.setOnClickListener {
            applyFilters()
        }

        binding.includeTestUsersSwitch.setOnCheckedChangeListener { _, isChecked ->
            currentFilters = currentFilters.copy(includeTestUsers = isChecked)
        }
    }

    private fun resetFilters() {
        binding.timeRangeGroup.check(R.id.last7Days)
        binding.includeTestUsersSwitch.isChecked = false
        binding.deviceTypeChips.clearCheck()
        binding.appVersionChips.clearCheck()
        currentFilters = AnalyticsFilterOptions()
        currentTimeRange = AnalyticsTimeRange.LAST_7_DAYS
    }

    private fun applyFilters() {
        val selectedDeviceTypes = binding.deviceTypeChips.checkedChipIds.mapNotNull { id ->
            binding.deviceTypeChips.findViewById<Chip>(id)?.text?.toString()
        }.toSet()

        val selectedVersions = binding.appVersionChips.checkedChipIds.mapNotNull { id ->
            binding.appVersionChips.findViewById<Chip>(id)?.text?.toString()
        }.toSet()

        currentFilters = currentFilters.copy(
            deviceTypes = selectedDeviceTypes,
            appVersions = selectedVersions
        )

        onFilterApplied?.invoke(currentTimeRange, currentFilters)
        dismiss()
    }

    private fun restoreCurrentSelection() {
        binding.includeTestUsersSwitch.isChecked = currentFilters.includeTestUsers

        // Restore time range selection
        val timeRangeButtonId = when (currentTimeRange) {
            AnalyticsTimeRange.LAST_24_HOURS -> R.id.last24Hours
            AnalyticsTimeRange.LAST_7_DAYS -> R.id.last7Days
            AnalyticsTimeRange.LAST_30_DAYS -> R.id.last30Days
            AnalyticsTimeRange.LAST_90_DAYS -> R.id.last90Days
        }
        binding.timeRangeGroup.check(timeRangeButtonId)

        // Restore device type selections
        currentFilters.deviceTypes.forEach { deviceType ->
            binding.deviceTypeChips.findViewWithTag<Chip>(deviceType)?.isChecked = true
        }

        // Restore app version selections
        currentFilters.appVersions.forEach { version ->
            binding.appVersionChips.findViewWithTag<Chip>(version)?.isChecked = true
        }
    }

    fun setOnFilterAppliedListener(listener: (AnalyticsTimeRange, AnalyticsFilterOptions) -> Unit) {
        onFilterApplied = listener
    }

    companion object {
        fun newInstance(
            currentTimeRange: AnalyticsTimeRange,
            currentFilters: AnalyticsFilterOptions
        ): AnalyticsFilterDialog {
            return AnalyticsFilterDialog().apply {
                this.currentTimeRange = currentTimeRange
                this.currentFilters = currentFilters
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
