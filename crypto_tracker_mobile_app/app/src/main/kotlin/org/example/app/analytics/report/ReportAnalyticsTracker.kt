package org.example.app.analytics.report

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.example.app.analytics.EventLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportAnalyticsTracker @Inject constructor(
    private val eventLogger: EventLogger
) {
    private val _reportMetrics = MutableStateFlow<ReportMetrics>(ReportMetrics())
    val reportMetrics: StateFlow<ReportMetrics> = _reportMetrics

    fun trackReportGeneration(reportId: String, format: ReportFormat, timeToGenerate: Long) {
        eventLogger.logUserAction(
            action = "report_generation",
            target = reportId,
            mapOf(
                "format" to format.name,
                "generation_time" to timeToGenerate.toString()
            )
        )

        updateMetrics { metrics ->
            metrics.copy(
                totalReportsGenerated = metrics.totalReportsGenerated + 1,
                averageGenerationTime = updateAverage(
                    metrics.averageGenerationTime,
                    timeToGenerate,
                    metrics.totalReportsGenerated
                ),
                formatDistribution = metrics.formatDistribution + (format to
                        (metrics.formatDistribution[format] ?: 0) + 1)
            )
        }
    }

    fun trackReportView(reportId: String) {
        eventLogger.logUserAction(
            action = "report_view",
            target = reportId
        )

        updateMetrics { metrics ->
            metrics.copy(totalReportViews = metrics.totalReportViews + 1)
        }
    }

    fun trackReportShare(reportId: String, shareMethod: String) {
        eventLogger.logUserAction(
            action = "report_share",
            target = reportId,
            mapOf("share_method" to shareMethod)
        )

        updateMetrics { metrics ->
            metrics.copy(
                totalReportShares = metrics.totalReportShares + 1,
                shareMethodDistribution = metrics.shareMethodDistribution + (shareMethod to
                        (metrics.shareMethodDistribution[shareMethod] ?: 0) + 1)
            )
        }
    }

    private fun updateMetrics(update: (ReportMetrics) -> ReportMetrics) {
        _reportMetrics.value = update(_reportMetrics.value)
    }

    private fun updateAverage(currentAvg: Double, newValue: Long, count: Int): Double {
        return (currentAvg * count + newValue) / (count + 1)
    }
}

data class ReportMetrics(
    val totalReportsGenerated: Int = 0,
    val totalReportViews: Int = 0,
    val totalReportShares: Int = 0,
    val averageGenerationTime: Double = 0.0,
    val formatDistribution: Map<ReportFormat, Int> = emptyMap(),
    val shareMethodDistribution: Map<String, Int> = emptyMap()
)
