package org.example.app.analytics.report.template.merge.visualization

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.example.app.R
import org.example.app.analytics.report.template.merge.ConflictType
import org.example.app.analytics.report.template.merge.MergeHistoryManager
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class MergeHistoryVisualizationFragment : Fragment() {
    @Inject
    lateinit var historyManager: MergeHistoryManager

    private lateinit var mergeActivityChart: LineChart
    private lateinit var conflictTypeChart: PieChart
    private lateinit var resolutionTrendChart: LineChart

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_merge_history_visualization, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        mergeActivityChart = view.findViewById(R.id.mergeActivityChart)
        conflictTypeChart = view.findViewById(R.id.conflictTypeChart)
        resolutionTrendChart = view.findViewById(R.id.resolutionTrendChart)

        setupCharts()
        observeMergeHistory()
    }

    private fun setupCharts() {
        setupMergeActivityChart()
        setupConflictTypeChart()
        setupResolutionTrendChart()
    }

    private fun setupMergeActivityChart() {
        with(mergeActivityChart) {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            
            xAxis.valueFormatter = DateAxisValueFormatter()
            axisLeft.setDrawGridLines(false)
            axisRight.isEnabled = false
            
            legend.isEnabled = true
            setDrawGridBackground(false)
        }
    }

    private fun setupConflictTypeChart() {
        with(conflictTypeChart) {
            description.isEnabled = false
            isRotationEnabled = true
            legend.isEnabled = true
            setUsePercentValues(true)
            setDrawEntryLabels(true)
        }
    }

    private fun setupResolutionTrendChart() {
        with(resolutionTrendChart) {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            
            xAxis.valueFormatter = DateAxisValueFormatter()
            axisLeft.setDrawGridLines(false)
            axisRight.isEnabled = false
            
            legend.isEnabled = true
            setDrawGridBackground(false)
        }
    }

    private fun observeMergeHistory() {
        viewLifecycleOwner.lifecycleScope.launch {
            historyManager.mergeHistory.collect { history ->
                updateMergeActivityChart(history)
                updateConflictTypeChart(history)
                updateResolutionTrendChart(history)
            }
        }
    }

    private fun updateMergeActivityChart(history: List<MergeRecord>) {
        val entries = history
            .groupBy { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.timestamp)) }
            .map { (date, records) ->
                Entry(
                    date.toFloat(),
                    records.size.toFloat()
                )
            }
            .sortedBy { it.x }

        val dataSet = LineDataSet(entries, "Merge Activity").apply {
            setDrawValues(false)
            setDrawFilled(true)
            setDrawCircles(true)
            lineWidth = 2f
        }

        mergeActivityChart.data = LineData(dataSet)
        mergeActivityChart.invalidate()
    }

    private fun updateConflictTypeChart(history: List<MergeRecord>) {
        val conflictsByType = history
            .flatMap { it.conflicts }
            .groupBy { it.type }
            .mapValues { it.value.size }

        val entries = conflictsByType.map { (type, count) ->
            PieEntry(
                count.toFloat(),
                when (type) {
                    ConflictType.SECTION_DELETION -> "Section Deletion"
                    ConflictType.SECTION_MODIFICATION -> "Section Modification"
                    ConflictType.STYLE_CONFLICT -> "Style Conflict"
                }
            )
        }

        val dataSet = PieDataSet(entries, "Conflict Types").apply {
            colors = listOf(
                R.color.conflict_high,
                R.color.conflict_medium,
                R.color.conflict_low
            ).map { requireContext().getColor(it) }
            valueTextSize = 14f
        }

        conflictTypeChart.data = PieData(dataSet)
        conflictTypeChart.invalidate()
    }

    private fun updateResolutionTrendChart(history: List<MergeRecord>) {
        val resolutionRates = history
            .groupBy { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.timestamp)) }
            .map { (date, records) ->
                val totalConflicts = records.sumOf { it.conflicts.size }
                val resolvedConflicts = records.sumOf { it.resolutions.size }
                Entry(
                    date.toFloat(),
                    if (totalConflicts > 0) 
                        resolvedConflicts.toFloat() / totalConflicts.toFloat() * 100
                    else 
                        100f
                )
            }
            .sortedBy { it.x }

        val dataSet = LineDataSet(resolutionRates, "Resolution Rate (%)").apply {
            setDrawValues(false)
            setDrawCircles(true)
            lineWidth = 2f
        }

        resolutionTrendChart.data = LineData(dataSet)
        resolutionTrendChart.invalidate()
    }
}

private class DateAxisValueFormatter : com.github.mikephil.charting.formatter.ValueFormatter() {
    private val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())

    override fun getFormattedValue(value: Float): String {
        return dateFormat.format(Date(value.toLong()))
    }
}
