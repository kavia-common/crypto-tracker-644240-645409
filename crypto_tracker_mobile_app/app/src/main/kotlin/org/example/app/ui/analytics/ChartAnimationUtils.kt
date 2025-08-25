package org.example.app.ui.analytics

import android.animation.ValueAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

object ChartAnimationUtils {
    
    fun animateLineChart(
        chart: LineChart,
        entries: List<Entry>,
        duration: Long = 1000L
    ) {
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = duration
        animator.interpolator = AccelerateDecelerateInterpolator()

        val originalEntries = entries.map { it.copy() }
        val animatedEntries = entries.map { Entry(it.x, 0f) }
        val dataSet = LineDataSet(animatedEntries, "Data").apply {
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            setDrawCircles(false)
        }

        chart.data = LineData(dataSet)

        animator.addUpdateListener { animation ->
            val progress = animation.animatedValue as Float
            entries.forEachIndexed { index, entry ->
                val targetY = originalEntries[index].y
                animatedEntries[index].y = targetY * progress
            }
            chart.data.notifyDataChanged()
            chart.notifyDataSetChanged()
            chart.invalidate()
        }

        animator.start()
    }

    fun animatePieChart(
        chart: PieChart,
        entries: List<PieEntry>,
        colors: List<Int>,
        duration: Long = 1000L
    ) {
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = duration
        animator.interpolator = AccelerateDecelerateInterpolator()

        val originalEntries = entries.map { it.value }
        val animatedEntries = entries.map { PieEntry(0f, it.label) }
        val dataSet = PieDataSet(animatedEntries, "Data").apply {
            this.colors = colors
            valueTextSize = 14f
            sliceSpace = 3f
        }

        chart.data = PieData(dataSet)

        animator.addUpdateListener { animation ->
            val progress = animation.animatedValue as Float
            entries.forEachIndexed { index, entry ->
                val targetValue = originalEntries[index]
                animatedEntries[index].value = targetValue * progress
            }
            chart.data.notifyDataChanged()
            chart.notifyDataSetChanged()
            chart.invalidate()
        }

        // Animate chart rotation
        chart.animateXY(duration, duration)
        animator.start()
    }

    fun createProgressAnimation(
        startValue: Float,
        endValue: Float,
        duration: Long = 1000L,
        onUpdate: (Float) -> Unit,
        onComplete: () -> Unit = {}
    ) {
        val animator = ValueAnimator.ofFloat(startValue, endValue)
        animator.duration = duration
        animator.interpolator = AccelerateDecelerateInterpolator()

        animator.addUpdateListener { animation ->
            onUpdate(animation.animatedValue as Float)
        }

        animator.addListener(
            onEnd = { onComplete() }
        )

        animator.start()
    }

    fun updateChartWithAnimation(
        chart: LineChart,
        newData: List<Entry>,
        duration: Long = 500L
    ) {
        val currentData = chart.data?.getDataSetByIndex(0)?.entries ?: emptyList()
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = duration
        animator.interpolator = AccelerateDecelerateInterpolator()

        animator.addUpdateListener { animation ->
            val progress = animation.animatedValue as Float
            val updatedEntries = newData.mapIndexed { index, newEntry ->
                val oldEntry = currentData.getOrNull(index)
                if (oldEntry != null) {
                    val deltaY = newEntry.y - oldEntry.y
                    Entry(newEntry.x, oldEntry.y + (deltaY * progress))
                } else {
                    Entry(newEntry.x, newEntry.y * progress)
                }
            }

            val dataSet = LineDataSet(updatedEntries, "Data").apply {
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawFilled(true)
                setDrawCircles(false)
            }

            chart.data = LineData(dataSet)
            chart.invalidate()
        }

        animator.start()
    }
}
