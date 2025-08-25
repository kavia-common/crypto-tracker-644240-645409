package org.example.app.ui.analytics

import android.content.Context
import android.graphics.Color
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import org.example.app.R
import java.text.SimpleDateFormat
import java.util.*

object ChartUtils {
    
    fun setupEngagementChart(chart: LineChart, context: Context) {
        chart.apply {
            description.isEnabled = false
            legend.isEnabled = true
            setTouchEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                valueFormatter = object : IndexAxisValueFormatter() {
                    private val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                    override fun getFormattedValue(value: Float): String {
                        return dateFormat.format(Date(value.toLong()))
                    }
                }
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                setDrawZeroLine(false)
            }
            
            axisRight.isEnabled = false
        }
    }

    fun updateEngagementChart(
        chart: LineChart,
        data: List<EngagementDataPoint>,
        context: Context
    ) {
        val activeUsersEntries = data.mapIndexed { index, point ->
            Entry(point.timestamp.toFloat(), point.activeUsers.toFloat())
        }

        val sessionDurationEntries = data.mapIndexed { index, point ->
            Entry(point.timestamp.toFloat(), (point.sessionDuration / 60f))
        }

        val screenViewsEntries = data.mapIndexed { index, point ->
            Entry(point.timestamp.toFloat(), point.screenViews.toFloat())
        }

        val activeUsersDataSet = LineDataSet(activeUsersEntries, "Active Users").apply {
            color = context.getColor(R.color.primary)
            setDrawCircles(false)
            lineWidth = 2f
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        val sessionDurationDataSet = LineDataSet(sessionDurationEntries, "Avg. Session (min)").apply {
            color = context.getColor(R.color.accent)
            setDrawCircles(false)
            lineWidth = 2f
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        val screenViewsDataSet = LineDataSet(screenViewsEntries, "Screen Views").apply {
            color = context.getColor(R.color.secondary)
            setDrawCircles(false)
            lineWidth = 2f
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        chart.data = LineData(activeUsersDataSet, sessionDurationDataSet, screenViewsDataSet)
        chart.invalidate()
    }

    fun setupCrashChart(chart: PieChart, context: Context) {
        chart.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(110)
            holeRadius = 58f
            transparentCircleRadius = 61f
            setDrawCenterText(true)
            centerText = "Crash Types"
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            legend.isEnabled = true
        }
    }

    fun updateCrashChart(
        chart: PieChart,
        crashTypes: Map<String, Int>,
        context: Context
    ) {
        val entries = crashTypes.map { (type, count) ->
            PieEntry(count.toFloat(), type)
        }

        val colors = listOf(
            context.getColor(R.color.primary),
            context.getColor(R.color.accent),
            context.getColor(R.color.secondary),
            context.getColor(R.color.positive_green),
            context.getColor(R.color.negative_red)
        )

        val dataSet = PieDataSet(entries, "Crash Types").apply {
            this.colors = colors
            valueTextSize = 12f
            valueTextColor = Color.WHITE
            sliceSpace = 3f
        }

        chart.data = PieData(dataSet)
        chart.invalidate()
    }
}
