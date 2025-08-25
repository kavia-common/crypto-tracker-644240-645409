package org.example.app.analytics.report

import android.content.Context
import org.example.app.analytics.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsReportGenerator @Inject constructor(
    private val context: Context,
    private val analyticsDataManager: AnalyticsDataManager
) {
    suspend fun generateReport(
        timeRange: AnalyticsTimeRange,
        filters: AnalyticsFilterOptions
    ): AnalyticsReport {
        val data = analyticsDataManager.getAnalyticsData(timeRange).collect { it }
        return createReport(data, timeRange, filters)
    }

    private fun createReport(
        data: AnalyticsData,
        timeRange: AnalyticsTimeRange,
        filters: AnalyticsFilterOptions
    ): AnalyticsReport {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val percentFormat = NumberFormat.getPercentInstance()
        
        return AnalyticsReport(
            metadata = ReportMetadata(
                generatedAt = System.currentTimeMillis(),
                timeRange = timeRange,
                filters = filters
            ),
            summary = createSummary(data),
            sections = listOf(
                createEngagementSection(data.engagementMetrics),
                createExperimentsSection(data.experimentResults),
                createCrashAnalyticsSection(data.crashAnalytics)
            )
        )
    }

    private fun createSummary(data: AnalyticsData): ReportSummary {
        return ReportSummary(
            totalActiveUsers = data.engagementMetrics.dailyActiveUsers.last().value.toInt(),
            averageSessionDuration = data.engagementMetrics.sessionDuration.average(),
            retentionRate = data.engagementMetrics.retentionRate,
            crashRate = data.crashAnalytics.crashRate
        )
    }

    private fun createEngagementSection(metrics: EngagementMetrics): ReportSection {
        return ReportSection(
            title = "User Engagement",
            metrics = listOf(
                ReportMetric(
                    name = "Daily Active Users",
                    value = metrics.dailyActiveUsers.last().value.toString(),
                    trend = calculateTrend(metrics.dailyActiveUsers)
                ),
                ReportMetric(
                    name = "Average Session Duration",
                    value = formatDuration(metrics.sessionDuration.last().value),
                    trend = calculateTrend(metrics.sessionDuration)
                ),
                ReportMetric(
                    name = "Screen Views per Session",
                    value = metrics.screenViews.last().value.toString(),
                    trend = calculateTrend(metrics.screenViews)
                ),
                ReportMetric(
                    name = "Retention Rate",
                    value = NumberFormat.getPercentInstance().format(metrics.retentionRate),
                    trend = 0.0 // No trend for retention rate
                )
            ),
            charts = listOf(
                createTimeSeriesChart(metrics.dailyActiveUsers, "Daily Active Users"),
                createTimeSeriesChart(metrics.sessionDuration, "Session Duration")
            )
        )
    }

    private fun createExperimentsSection(experiments: List<ExperimentMetrics>): ReportSection {
        return ReportSection(
            title = "Experiment Results",
            metrics = experiments.map { experiment ->
                ReportMetric(
                    name = experiment.experimentId,
                    value = "${experiment.variant}: ${NumberFormat.getPercentInstance().format(experiment.conversionRate)}",
                    trend = experiment.improvement
                )
            },
            charts = listOf(
                createExperimentComparisonChart(experiments)
            )
        )
    }

    private fun createCrashAnalyticsSection(crashAnalytics: CrashMetrics): ReportSection {
        return ReportSection(
            title = "Crash Analytics",
            metrics = listOf(
                ReportMetric(
                    name = "Crash Rate",
                    value = NumberFormat.getPercentInstance().format(crashAnalytics.crashRate),
                    trend = calculateTrend(crashAnalytics.crashTrend)
                )
            ),
            charts = listOf(
                createPieChart(crashAnalytics.crashTypes, "Crash Types"),
                createPieChart(crashAnalytics.affectedDevices, "Affected Devices")
            )
        )
    }

    private fun calculateTrend(metrics: List<DailyMetric>): Double {
        if (metrics.size < 2) return 0.0
        val first = metrics.first().value
        val last = metrics.last().value
        return (last - first) / first
    }

    private fun formatDuration(seconds: Double): String {
        val minutes = seconds / 60
        return String.format("%.1f min", minutes)
    }

    private fun createTimeSeriesChart(data: List<DailyMetric>, title: String): ReportChart {
        return ReportChart(
            type = ChartType.LINE,
            title = title,
            data = data.map { metric ->
                ChartDataPoint(
                    x = metric.timestamp,
                    y = metric.value
                )
            }
        )
    }

    private fun createExperimentComparisonChart(
        experiments: List<ExperimentMetrics>
    ): ReportChart {
        return ReportChart(
            type = ChartType.BAR,
            title = "Experiment Conversion Rates",
            data = experiments.map { experiment ->
                ChartDataPoint(
                    label = experiment.variant,
                    y = experiment.conversionRate
                )
            }
        )
    }

    private fun createPieChart(
        data: Map<String, Int>,
        title: String
    ): ReportChart {
        return ReportChart(
            type = ChartType.PIE,
            title = title,
            data = data.map { (label, value) ->
                ChartDataPoint(
                    label = label,
                    y = value.toDouble()
                )
            }
        )
    }
}

data class AnalyticsReport(
    val metadata: ReportMetadata,
    val summary: ReportSummary,
    val sections: List<ReportSection>
)

data class ReportMetadata(
    val generatedAt: Long,
    val timeRange: AnalyticsTimeRange,
    val filters: AnalyticsFilterOptions
)

data class ReportSummary(
    val totalActiveUsers: Int,
    val averageSessionDuration: Double,
    val retentionRate: Double,
    val crashRate: Double
)

data class ReportSection(
    val title: String,
    val metrics: List<ReportMetric>,
    val charts: List<ReportChart>
)

data class ReportMetric(
    val name: String,
    val value: String,
    val trend: Double
)

data class ReportChart(
    val type: ChartType,
    val title: String,
    val data: List<ChartDataPoint>
)

data class ChartDataPoint(
    val x: Long? = null,
    val y: Double,
    val label: String? = null
)

enum class ChartType {
    LINE,
    BAR,
    PIE
}
